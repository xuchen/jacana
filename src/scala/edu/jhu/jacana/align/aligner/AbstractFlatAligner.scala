/**
 *
 */
package edu.jhu.jacana.align.aligner

import edu.jhu.jacana.align.feature.AlignFeature
import edu.jhu.jacana.align.AlignTrainData
import edu.jhu.jacana.align.crf.LinearChainCRF
import java.io.PrintWriter
import java.io.File
import edu.jhu.jacana.align.AlignTrainRecord
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import edu.jhu.jacana.align.util.Loggable
import edu.jhu.jacana.align.util.AlignerParams
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.PosixParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignTestRecord
import edu.jhu.jacana.align.AlignSequence
import scala.io.Source
import edu.jhu.jacana.util.FileManager
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.OutputKeys
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * an abstract class for flat structured aligners
 * @author Xuchen Yao
 *
 */
abstract class AbstractFlatAligner extends Loggable {
    AlignerParams.initSettings()
    val labelAlphabet = new IndexLabelAlphabet()
    def getLabelAlphabet = labelAlphabet 
    var FeatureExtractors: Array[AlignFeature] = null
    var crf: LinearChainCRF = null
    val options = new Options()
    
    // false: src2tgt, true: tgt2src
    var transpose = false
    // true: use OpenNLP tokenizer
    // false: use space tokenizer
    var tokenize = true
    
    // whether this is run within eclipse (parameters are passed by modifying the source code)
    var eclipseMode = true
    
    var trainFilename:String = null
    var devFilename:String = null
    var testFilename:String = null
    var alignFilename:String = null
    var outputFilename:String = null
    var modelFilename:String = null
    var initialModelFilename:String = null
    var configFilename:String = null
    val dataDir = "alignment-data/msr/converted/"
    val dataDirEdinburgh = "alignment-data/edinburgh/"
    val dataDirEdinburghPhrases = "alignment-data/edinburgh/synthetic-phrases/"
    var phraseBased = false
    var academic = false
    var parallel = true
    
    var heapSizeStart:Long = 0
    var heapSizeEnd:Long = 0
    
    // return true if no parameters are passed in args
    //  (might be in eclipse mode)
    def parseArguments(args: Array[String]): Boolean = {
        //if (eclipseMode) return true
        if (args.length == 0) {
            eclipseMode = true
            return true
        }
        //eclipseMode = false
        val parser:CommandLineParser  = new PosixParser()
        
        options.addOption("h", "help", false, "show help")
        options.addOption("r", "train", true, "training file")
        options.addOption("e", "test", true, "test file")
        options.addOption("d", "dev", true, "dev file to report F1 on while training")
        options.addOption("a", "align", true, "file to align (with tab-separated sentences)")
        options.addOption("o", "output", true, "aligned output")
        options.addOption("m", "model", true, "model file \n" +
                "(train: save to, test: read from, both: save to, dev: append '.iterXX' and save to)")
        options.addOption("i", "initial-model", true, "initial model for training \n" +
                "(first read this model, then continue the whole training process from here")
        options.addOption("t", "transpose", false, "transpose the alignment matrix")
        options.addOption("l", "single", false, "run in single thread (default: parallel)")
        options.addOption("n", "no-tokenize", false, "don't tokenize align file (use space separators), default: false")
        //options.addOption("c", "config", true, "config file")
        options.addOption("p", "phrase", false, "run phrase-based aligner (default is token-based)")
        options.addOption("c", "academic", false, "academic mode (full features)\n" +
                "only use this when running academic experiments with full feature sets.")
        options.addOption("", "src", true, "source language (en/fr, default: en)")
        options.addOption("", "tgt", true, "target language (en, default: en)")
            	
        val line:CommandLine  = parser.parse(options, args)
        if (line.hasOption("help")) {
            val usage = "java "+this.getClass().getName().split("\\$")(0)
            val header = "train&test: "+ " -r train_file -e test_file -o aligned_output -m save_to_model_file\n" +
                    "train: -r train_file -m save_to_model_file\n" + 
                    "test: -m load_from_model_file -e test_file -o aligned_output\n"
            
            (new HelpFormatter()).printHelp(100, usage, header, options, "")
            System.exit(0)
        }
        trainFilename = line.getOptionValue("train", null)
        exitIfNotExist(trainFilename)
        devFilename = line.getOptionValue("dev", null)
        exitIfNotExist(devFilename)
        testFilename = line.getOptionValue("test", null)
        exitIfNotExist(testFilename)
        alignFilename = line.getOptionValue("align", null)
        exitIfNotExist(alignFilename)
        outputFilename = line.getOptionValue("output", "/tmp/aligned.txt")
        //exitIfNotExist(outputFilename)
        modelFilename = line.getOptionValue("model", "/tmp/aligner.model")
        initialModelFilename = line.getOptionValue("initial-model", null)
        transpose = line.hasOption("transpose")
        parallel = !line.hasOption("single")
        tokenize = !line.hasOption("no-tokenize")
        configFilename = line.getOptionValue("config", null)
        exitIfNotExist(configFilename)
        phraseBased = line.hasOption("phrase")
        academic = line.hasOption("academic")
        
        return false
    }
    
    def exitIfNotExist(fname: String) {
        if (fname != null && !FileManager.fileExists(fname)) {
            System.err.println(s"$fname doesn't exist, exiting.")
            System.exit(-1)
        }
    }
    
    def extractFeatures(data: AlignTrainData, featureAlphabet: Alphabet) {
        var i = 0
         for (alignedRecord <- data.getTrainData) {
             extractFeatures(alignedRecord, featureAlphabet)
        }
    }

    def extractFeatures(record: AlignSequence, featureAlphabet: Alphabet) {
        for (extractor <- FeatureExtractors) {
            extractor.extract(record, featureAlphabet, labelAlphabet)
        }
    }

    
    /**
     * decode test data and write to <code>outputFname</code>
     * 
     * @param outputFname if ends with "json", write in JSON format, otherwise MSR
     */
    def decode(testData: AlignTrainData, outputFname: String = null): (Int, Int,Int,Int,Int) = {
        
        AlignerParams.train = false
        
        extractFeatures(testData, this.crf.featureAlphabet)
    	
        var total = 0; var total_non_zero = 0;
        var correct = 0; var correct_non_zero = 0;
        testData.startScan
        while (testData.hasMoreRecords) {
            val testRecord = testData.nextRecord.asInstanceOf[AlignTrainRecord]
            val goldLabels = testRecord.getGoldLabelsPerToken().toList
            total += goldLabels.size
            crf.decode(testRecord)
            val taggedLabels = testRecord.getLabelsPerToken().toList
            println(testRecord.getPair.id)
            println("gold label: " + goldLabels)
            println("decoddedo label: " + taggedLabels)
            for ((i,j) <- goldLabels.zip(taggedLabels)){
                if (i == j) correct += 1
                if (i != 0) {
                    total_non_zero += 1
                    if (i == j) correct_non_zero += 1
                }
            }
        }
        if (outputFname != null) {
            
            var writer: PrintWriter = new PrintWriter(new File(outputFname))
            if (outputFname.contains("json"))
                writer.print(testData.toJSON) 
            else
                writer.print(testData.toMsrFormat) 
            writer.close()
            println("aligned output written to "+outputFname)
        }
        return (testData.size(), total, total_non_zero, correct, correct_non_zero)
    }
    
         
    def decode(testRecord: AlignTestRecord): Double = {
        
        AlignerParams.train = false
        
        extractFeatures(testRecord, this.crf.featureAlphabet)
    	
        return crf.decode(testRecord)
    }
    
    
    /**
     * Given an input file, output the aligned results to a .json file
     * 
     */
    def decode(inputFile: String, outputFname: String): Int = {
        
           var writer: PrintWriter = null
          
          var counter = 0
          
          // Vulcan XML
           if (inputFile.contains("xml")) {
               // scala XML reads xml into an immutable object, which is
               // hard to be modified in-place, thus we fall back to DOM
               val documentBuilderFactory = DocumentBuilderFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", null)
               val documentBuilder = documentBuilderFactory.newDocumentBuilder();
               val document = documentBuilder.parse(inputFile);
               val root = document.getDocumentElement();
               
               /**
<questions>
  <q qid="34">
    <th option-id="(A)">
      <t>The difference is that in C4 plants, the initial steps of carbon fixation are separated structurally from the Calvin cycle, whereas in CAM plants, the two steps occur at separate times but within the same cell.</t>
      <h> in C4 plants, the Calvin cycle takes place at night</h>
    </th>
    <th option-id="(B)">
      <t>When their stomata partially close on hot, dry days, C3 plants produce less sugar because the declining level of CO2 in the leaf starves the Calvin cycle.</t>
      <h> in C4 plants, the Calvin cycle only occurs when stomata are closed</h>
    </th>
 </q>
 <q ...
 </q>
</questions>
                */
               var i = 0;
               while (i < root.getChildNodes().getLength()) {
                   val q = root.getChildNodes().item(i)
                   i += 1
                   if (q.getNodeName == "q") {
                     var j = 0
                     while (j < q.getChildNodes().getLength()) {
                         val th = q.getChildNodes().item(j)
                         j += 1
                         
                         if (th.getNodeName() == "th") {
                             counter += 1
      
                             var k = 0
                             var sent1 = ""; var sent2 = "";
                             var found = false
                             while (k < th.getChildNodes().getLength() && !found) {
                                 val t_or_h = th.getChildNodes().item(k)
                                 k += 1
                                 if (t_or_h.getNodeName() == "t")
                                     sent1 = t_or_h.getTextContent()
                                 if (t_or_h.getNodeName() == "h")
                                     sent2 = t_or_h.getTextContent()

                                 // decode
                                 if (sent1 != "" && sent2 != "") {
                                     if (transpose) {
                                         var tmp = sent1; sent1 = sent2; sent2 = tmp;
                                     }
                                     found = true
                                     var record = new AlignTestRecord(sent1, sent2, this.tokenize, labelAlphabet=this.labelAlphabet)
                                     val score = this.decode(record)
                                     val scoreNormalized = score/record.length
              
                                     val t_node = document.createElement("t-tokenized")
                                     if (transpose)
                                         t_node.setTextContent(record.pair.tgtTokens.mkString(" "))
                                     else
                                         t_node.setTextContent(record.pair.srcTokens.mkString(" "))
                                     th.appendChild(t_node)
                                                   
                                     val h_node = document.createElement("h-tokenized")
                                     if (transpose)
                                         h_node.setTextContent(record.pair.srcTokens.mkString(" "))
                                     else
                                         h_node.setTextContent(record.pair.tgtTokens.mkString(" "))
                                     th.appendChild(h_node)
                                     
                                     record.toJSON()
                                     val a_node = document.createElement("align")
                                     a_node.setTextContent(record.pair.getDashedAlign(this.transpose))
                                     th.appendChild(a_node)

                                     // append node
                                     val score_node = document.createElement("aligner-score")
                                     score_node.setTextContent(score.toString)
                                     th.appendChild(score_node)
              
                                     val score_node_normalized = document.createElement("aligner-score-normalized")
                                     score_node_normalized.setTextContent(scoreNormalized.toString)
                                     th.appendChild(score_node_normalized)
                                 }
                             }
                         }
                     }
                   }
               }
               val source = new DOMSource(document);

            val transformerFactory = TransformerFactory.newInstance();
            val transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            val result = new StreamResult(outputFname);
            transformer.transform(source, result);
           } else {
               // output to json format
            if (outputFname != null) {
                writer = new PrintWriter(new File(outputFname), "UTF-8")
                   writer.write("[\n")
            }
 
            if (inputFile.toLowerCase().endsWith(".json")) {
                val trainData = new AlignTrainData(inputFile, this.transpose, this.tokenize, labelAlphabet = this.labelAlphabet)
                val listTrainData = trainData.getTrainData
                val toWrite = new Array[String](listTrainData.length)
                if (false) {
                    // this parallelization works no faster than parallelization for feature extraction
                    // (AlignFeature.scala), but it complicates output, so disabling it.
                    val iListPar = (0 until listTrainData.length).par
                    for (i <- iListPar) {
                        val alignedRecord = listTrainData(i)
                        counter += 1
                        if (counter % 1000 == 0) {
                            println(inputFile + "\t" + counter.toString)
                        }
                        var sent1 = alignedRecord.getPair().src
                        var sent2 = alignedRecord.getPair().tgt
                        if (sent1.length() == 0 || sent2.length() == 0) {
                            log.error("skipping, not a pair: " + alignedRecord.getPair().id)
                        } else {
                               if (transpose) {
                                   var tmp = sent1; sent1 = sent2; sent2 = tmp;
                               }
                            var record = new AlignTestRecord(sent1, sent2, this.tokenize, alignedRecord.getPair.id, labelAlphabet=this.labelAlphabet)
                            val score = this.decode(record)
                            if (writer != null) {
                              if (outputFname.contains("json")) {
                                toWrite(i) = record.toJSON(alignedRecord.getPair.id)
                                // writer.print(record.toJSON(alignedRecord.getPair.id))
                                // if (i < listTrainData.length - 1)
                                //   writer.print("\t,\n")
                              } else {
                                toWrite(i) = record.toMsrFormat
                                // writer.print(record.toMsrFormat)
                              }
                            }
                    }
            	}
                for (i <- 0 until listTrainData.length) {
                    writer.print(toWrite(i))
                    if (i < listTrainData.length - 1)
                      writer.print("\t,\n")
                }
            	} else {
                for (i <- 0 until listTrainData.length) {
                    val alignedRecord = listTrainData(i)
                    counter += 1
                    if (counter % 1000 == 0) {
                        println(inputFile + "\t" + counter.toString)
                    }
                    var sent1 = alignedRecord.getPair().src
                    var sent2 = alignedRecord.getPair().tgt
                    if (sent1.length() == 0 || sent2.length() == 0) {
                        log.error("skipping, not a pair: " + alignedRecord.getPair().id)
                    } else {
                           if (transpose) {
                               var tmp = sent1; sent1 = sent2; sent2 = tmp;
                           }
                        var record = new AlignTestRecord(sent1, sent2, this.tokenize, alignedRecord.getPair.id, labelAlphabet=this.labelAlphabet)
                        val score = this.decode(record)
                        if (writer != null) {
                          if (outputFname.contains("json")) {
                            writer.print(record.toJSON(alignedRecord.getPair.id))
                            if (i < listTrainData.length - 1)
                              writer.print("\t,\n")
                          } else
                            writer.print(record.toMsrFormat)
                        }
                    }
            	}
            	}
               } else {
           
                val f = Source.fromFile(inputFile, "UTF-8")
                val lineIterator = f.getLines()
                //val total = lineIterator.length
                while (lineIterator.hasNext) {
                    counter += 1
                    if (counter % 1000 == 0) {
                        println(inputFile + "\t" + counter.toString)
                    }
                    var line = lineIterator.next()
                    //println(line)
                    val sents = line.trim().split("\\t")
                    var sent1 = sents(0); var sent2 = sents(1)
                    if (sent1.length() == 0 || sent2.length() == 0) {
                        log.error("skipping, not a pair in line: " + line)
                    } else {
                           if (transpose) {
                               var tmp = sent1; sent1 = sent2; sent2 = tmp;
                           }
                        var record = new AlignTestRecord(sent1, sent2, this.tokenize, labelAlphabet=this.labelAlphabet)
                        val score = this.decode(record)
                        if (writer != null) {
                          if (outputFname.contains("json")) {
                            writer.print(record.toJSON(counter.toString))
                            if (lineIterator.hasNext)
                              writer.print("\t,\n")
                          } else
                            writer.print(record.toMsrFormat)
                        }
                    }
                }
               }
            if (writer != null) {
                writer.write("]\n")
                writer.close()
                println("aligned output written to "+outputFname)
            }
           }
          
        return counter
    }
   
    def saveModel(fileName: String) {
        this.crf.saveModel(fileName)
    }
    
    def readModel(fileName: String) {
        val input = new ObjectInputStream(new FileInputStream(fileName))
        crf = input.readObject().asInstanceOf[LinearChainCRF]
        input.close()
        println("model read from " + fileName)
    }
    
    def setMemUsageStart() { heapSizeStart =  Runtime.getRuntime().totalMemory() }

    def setMemUsageEnd() { heapSizeEnd =  Runtime.getRuntime().totalMemory() }
    
    def printConsumedMemInBetween(msg:String = "") {
        setMemUsageEnd()
        println("%d MB memory used in between %s".format((heapSizeEnd-heapSizeStart)/1000000, msg))
    }

    def printCurrentMem(msg:String = "") {
        println("%d MB memory currently used %s".format(Runtime.getRuntime().totalMemory()/1000000, msg))
    }
}