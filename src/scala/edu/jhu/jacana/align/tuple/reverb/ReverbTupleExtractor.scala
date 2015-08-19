/**
 *
 */
package edu.jhu.jacana.align.tuple.reverb
import scala.collection.JavaConverters._

import com.google.common.base.Joiner
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.util.LinkedList
import java.util.Queue
import edu.washington.cs.knowitall.argumentidentifier.ConfidenceMetric
import edu.washington.cs.knowitall.commonlib.Range
import edu.washington.cs.knowitall.extractor.ExtractorException
import edu.washington.cs.knowitall.extractor.R2A2
import edu.washington.cs.knowitall.extractor.ReVerbExtractor
import edu.washington.cs.knowitall.extractor.ReVerbRelationExtractor
import edu.washington.cs.knowitall.extractor.conf.ConfidenceFunction
import edu.washington.cs.knowitall.extractor.conf.ConfidenceFunctionException
import edu.washington.cs.knowitall.extractor.conf.ReVerbOpenNlpConfFunction
import edu.washington.cs.knowitall.extractor.mapper.PronounArgumentFilter
import edu.washington.cs.knowitall.io.BufferedReaderIterator
import edu.washington.cs.knowitall.nlp.ChunkedSentence
import edu.washington.cs.knowitall.nlp.ChunkedSentenceIterator
import edu.washington.cs.knowitall.nlp.ChunkedSentenceReader
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;
import edu.washington.cs.knowitall.util.DefaultObjects
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker


/**
 * @author Xuchen Yao
 *
 */
object ReverbTupleExtractor {

    val minFreq = 20
    val useSynLexConstraints = false
    val mergeOverlapRels = true
    val allowUnary = false
    val confFunc = new ReVerbOpenNlpConfFunction()
    val normalizer = new BinaryExtractionNormalizer()
    val chunker = new OpenNlpSentenceChunker()
    var numSents = 0

    DefaultObjects.useWhitespaceTokenizer = true;
    DefaultObjects.initializeNlpTools()
    val extractor = new ReVerbExtractor(minFreq, useSynLexConstraints, mergeOverlapRels, allowUnary);
    
    def extract(sent:String): List[NormalizedBinaryExtraction] = {
        val chunked = chunker.chunkSentence(sent)
        (for (ext <- extractor.extract(chunked).asScala)
            yield normalizer.normalize(ext)).toList
    }
    
    def getConf(extr: ChunkedBinaryExtraction):Double = {
        try {
            return confFunc.getConf(extr);
        } catch  {
            case e:Exception =>
            	System.err.println("Could not compute confidence for " + extr
                    + ": " + e.getMessage());
            return 0;
        }
    }

      def printExtr(extr: NormalizedBinaryExtraction, conf: Double) {
        val arg1 = extr.getArgument1().toString();
        val rel = extr.getRelation().toString();
        val arg2 = extr.getArgument2().toString();

        val sent = extr.getSentence();
        val toks = sent.getTokensAsString();
        val pos = sent.getPosTagsAsString();
        val chunks = sent.getChunkTagsAsString();
        val arg1Norm = extr.getArgument1Norm().toString();
        val relNorm = extr.getRelationNorm().toString();
        val arg2Norm = extr.getArgument2Norm().toString();

        val arg1Range = extr.getArgument1().getRange();
        val relRange = extr.getRelation().getRange();
        val arg2Range = extr.getArgument2().getRange();
        val a1s = String.valueOf(arg1Range.getStart());
        val a1e = String.valueOf(arg1Range.getEnd());
        val rs = String.valueOf(relRange.getStart());
        val re = String.valueOf(relRange.getEnd());
        val a2s = String.valueOf(arg2Range.getStart());
        val a2e = String.valueOf(arg2Range.getEnd());

        val row = Array("stdin", String.valueOf(numSents), arg1,
                        rel, arg2, a1s, a1e, rs, re, a2s, a2e,
                        String.valueOf(conf), toks, pos, chunks, arg1Norm,
                        relNorm, arg2Norm).mkString("\t");

        System.out.println(row);
    }

    def main(args: Array[String]): Unit = {
        println("initialization done")
        
        val in = new BufferedReader(new InputStreamReader(System.in))
        val reader = DefaultObjects.getDefaultSentenceReader(in)
        
        val sentenceIt = reader.iterator();

        while (sentenceIt.hasNext()) {
            // get the next chunked sentence
            val sent = sentenceIt.next();
            numSents += 1

            // make the extractions
            val extractions = extractor.extract(sent);

            for (extr <- extractions.asScala) {

                // run the confidence function
                val conf = getConf(extr);

                val extrNorm = normalizer.normalize(extr);
                printExtr(extrNorm, conf);
            }
        }
    }

}