/**
 *
 */
package edu.jhu.jacana.align.aligner

import edu.jhu.jacana.align.reader.MsrReader
import edu.jhu.jacana.align.feature.AlignFeature
import edu.jhu.jacana.align.feature.StringSimilarityAlignFeature
import edu.jhu.jacana.align.feature.DistortionAlignFeature
import edu.jhu.jacana.align.AlignTrainData
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.IndexLabelAlphabet
import edu.jhu.jacana.align.util.Loggable
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.feature.WordnetAlignFeature
import edu.jhu.jacana.align.crf.NestedLinearChainCRF
import edu.jhu.jacana.align.feature._
import edu.jhu.jacana.align.Alphabet
import java.io.FileInputStream
import edu.jhu.jacana.align.evaluation.AlignEvaluator
import edu.jhu.jacana.align.crf.LinearChainCRF

/**
 * An aligner that takes flat input of two sentence pairs (''src'' -> ''tgt'') and align them token by token.
 * Target word positions are the hidden states of a linear chain CRF, with various features extracted
 * over pairs of tokens in ''src'' and ''tgt''.
 * 
 * @author Xuchen Yao
 *
 */
class FlatAligner extends AbstractFlatAligner {


    def initParams(parallel:Boolean = true) {
         val prop = new java.util.Properties()
         // quicker convergence
         //prop.setProperty("epsForConvergence", "0.00001")
         prop.setProperty("epsForConvergence", "0.001")
         prop.setProperty("trainer", "ll")
         if (this.phraseBased) {
             prop.setProperty("modelGraph", "semi-markov")
             prop.setProperty("segmentViterbi", "true")
         }
         if (!parallel || !this.parallel)
             prop.setProperty("parallel", "false")
         
         if (configFilename != null)
             prop.load(new FileInputStream(configFilename))
         AlignerParams.parseParameters(prop)
         
         FeatureExtractors =  Array[AlignFeature](DistortionAlignFeature, 
                 StringSimilarityAlignFeature, 
                 // don't enable these two for general purpose alignment
                 ContextualAlignFeature, PositionalAlignFeature,
                 // NOTE: Word2VecAlignFeature is extremely slow (taking 97% of all CPU time)
                 PosTagAlignFeature, PPDBsimpleAlignFeature, WordnetAlignFeature, // Word2VecAlignFeature,
                 WiktionaryRelationsAlignFeature, NicknamesAlignFeature  
                 /*, NamePhylogenyAlignFeature // not compatible on Windows*/)
            
         if (academic)
             FeatureExtractors ++=  Array[AlignFeature](UmbcSimilarityAlignFeature)
         // these feature data are stored in my local disk and are too big to be delivered to 3rd parties
         if (AlignerParams.phraseBased) {
             FeatureExtractors ++=  Array[AlignFeature](ChunkingStringSimilarityAlignFeature)
         } else {
         }
         for (f <- FeatureExtractors)
             f.init()
    }
    
    def initModel(featureAlphabet: Alphabet) {
        
         if (this.initialModelFilename != null)
             this.readModel(this.initialModelFilename)
         else {
             if (this.phraseBased) {
                 crf = new NestedLinearChainCRF(featureAlphabet, FeatureExtractors, labelAlphabet = this.labelAlphabet)
             } else {
                 crf = new LinearChainCRF(featureAlphabet, FeatureExtractors, labelAlphabet = this.labelAlphabet)
             }
         }
         if (crf.numF == 0) {
             log.fatal("Feature Count is 0! Call extractFeatures() first before initializing the model!")
             System.exit(-1)
         }
    }
    
}
  
object FlatAligner {
    
    def main(args: Array[String]): Unit = {
        
        val aligner = new FlatAligner()
        aligner.parseArguments(args)
        
        //aligner.phraseBased  = true
        
        aligner.initParams()
        var tiny = true
        
        aligner.eclipseMode = false
        
        val edingburgh = true
        
        
        var devData:AlignTrainData = null
        
        if (aligner.trainFilename == null && !aligner.eclipseMode) {
            aligner.readModel(aligner.modelFilename)
        } else {
            if (aligner.eclipseMode) {
                aligner.transpose = false
                if (tiny) {
                    if (edingburgh) {
                        //aligner.trainFilename = aligner.dataDirEdinburgh + "tiny.phrase.sure.json"
                        //aligner.devFilename = aligner.dataDirEdinburgh + "tiny.phrase.sure.json"
                        aligner.trainFilename = aligner.dataDirEdinburghPhrases + "tiny.synthetic-phrases.train.sure.json"
                        aligner.devFilename = aligner.dataDirEdinburghPhrases + "tiny.synthetic-phrases.test.sure.json"
                    } else {
                        aligner.trainFilename = aligner.dataDir + "RTE2_tiny_M.phrase.txt"
                        aligner.devFilename = aligner.dataDir + "RTE2_tiny_M.phrase.txt"
                    }
                } else {
                    if (edingburgh) {
                        aligner.trainFilename = aligner.dataDirEdinburgh + "gold.train.sure.json"
                    } else {
                        aligner.trainFilename = aligner.dataDir + "RTE2_dev_M.align.txt"
                    }
                }
                if (aligner.phraseBased)
                    aligner.modelFilename = "/tmp/flatPhraseAligner.model"
                else
                    aligner.modelFilename = "/tmp/flatTokenAligner.model"
            }
            
            aligner.printCurrentMem("before reading train data")
            aligner.setMemUsageStart()
            val trainData = new AlignTrainData(aligner.trainFilename, aligner.transpose, aligner.tokenize, labelAlphabet = aligner.getLabelAlphabet)
            AlignTrainRecord.printAlignStat()
            aligner.printConsumedMemInBetween("trainData (before feature extraction)")
        	
            val featureAlphabet = new Alphabet()
            aligner.extractFeatures(trainData, featureAlphabet)
            aligner.printConsumedMemInBetween("trainData (after feature extraction)")
            println("train data size: " + trainData.size())
        	
            // can only be called *after* extracting features on the training data
            // 'cause feature extraction also sets the max possible state index.
            aligner.getLabelAlphabet.freeze()
            
            println("Total Features: " + featureAlphabet.size)
            println("Total Labels: " + aligner.getLabelAlphabet.totalStates)
           
            aligner.printCurrentMem("after reading train data, before reading dev data")
            aligner.setMemUsageStart()
            devData = if (aligner.devFilename!=null) new AlignTrainData(aligner.devFilename, aligner.transpose, aligner.tokenize, labelAlphabet = aligner.getLabelAlphabet) else null
        	
            if (devData!=null) {
                AlignerParams.train = false
                aligner.extractFeatures(devData, featureAlphabet)
                AlignerParams.train = true
                aligner.printConsumedMemInBetween("devData (after feature extraction)")
                aligner.printCurrentMem("after reading dev data")
            }
           
            aligner.initModel(featureAlphabet)
            aligner.crf.train(trainData.getTrainData, devData, aligner.modelFilename, aligner.devFilename)
            if (aligner.modelFilename != null)
                aligner.saveModel(aligner.modelFilename)
        }
        
        aligner.crf.printFeatureWeights()
        
        if (aligner.alignFilename != null) {
            val s = System.nanoTime
            val total_align = aligner.decode(aligner.alignFilename, aligner.outputFilename)
            val speed_in_ms = (System.nanoTime - s) *1.0/ total_align / 1e6
            val speed_in_num = 1000.0 / speed_in_ms
            println(f"Decoding time: $speed_in_ms%.2f ms per alignment ($speed_in_num%.2f alignments per second)")
        	
        } else if (aligner.testFilename != null || aligner.eclipseMode) {
            if (aligner.eclipseMode) {
                if (tiny) {
                    if (edingburgh) {
                        //aligner.testFilename = "tiny.phrase.sure.json"
                        aligner.testFilename = "tiny.synthetic-phrases.train.sure.json"
                    } else {
                        aligner.testFilename = "RTE2_tiny_M.align.test.txt"
                    }
                } else {
                    if (edingburgh) {
                        aligner.testFilename = "gold.test.sure.json"
                    } else {
                        aligner.testFilename = "RTE2_test_M.align.txt"
                    }
                }
                aligner.outputFilename = "/tmp/"+aligner.testFilename+ (if (aligner.transpose) ".t2s" else ".s2t")
                if (edingburgh) {
                    //aligner.testFilename = aligner.dataDirEdinburgh + aligner.testFilename
                    aligner.testFilename = aligner.dataDirEdinburghPhrases + aligner.testFilename
                } else {
                    aligner.testFilename = aligner.dataDir + aligner.testFilename
                }
            }
            val testData = new AlignTrainData(aligner.testFilename, aligner.transpose, aligner.tokenize, labelAlphabet = aligner.getLabelAlphabet)
        	
            
            val s = System.nanoTime
            val (total_align, total, total_non_zero, correct, correct_non_zero) = aligner.decode(testData, aligner.outputFilename)
            val speed_in_ms = (System.nanoTime - s) *1.0/ total_align / 1e6
            val speed_in_num = 1000.0 / speed_in_ms
            println(f"Decoding time: $speed_in_ms%.2f ms per alignment ($speed_in_num%.2f alignments per seconds)")
            
           
            //println(testData.toMsrFormat)
           
            println("Precision for all: %.2f (%d/%d)".format(correct*1.0/total, correct, total))
            println("Precision for align: %.2f (%d/%d)".format(correct_non_zero*1.0/total_non_zero, correct_non_zero, total_non_zero))
            
            AlignEvaluator.evaluate(testData)
            println("done.")
        }
    }

}