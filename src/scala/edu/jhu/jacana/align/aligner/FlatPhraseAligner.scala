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

/**
 * An aligner that takes flat input of two sentence pairs (''src'' -> ''tgt'') and align them token by token.
 * Target word positions are the hidden states of a linear chain CRF, with various features extracted
 * over pairs of tokens in ''src'' and ''tgt''.
 * 
 * @author Xuchen Yao
 *
 */
class FlatPhraseAligner extends AbstractFlatAligner {

    FeatureExtractors =  Array[AlignFeature](DistortionAlignFeature, ChunkingStringSimilarityAlignFeature, StringSimilarityAlignFeature,
            PositionalAlignFeature, ContextualAlignFeature, PosTagAlignFeature, WordnetAlignFeature, PPDBAlignFeature, 
            UmbcSimilarityAlignFeature, NamePhylogenyAlignFeature) //, ChunkingAlignFeature,
    
    def initParams() {
         val prop = new java.util.Properties()
	     // quicker convergence
         //prop.setProperty("epsForConvergence", "0.00001")
         prop.setProperty("epsForConvergence", "0.001")
         prop.setProperty("modelGraph", "semi-markov")
         prop.setProperty("segmentViterbi", "true")
         prop.setProperty("trainer", "ll")
         
         if (configFilename != null)
	         prop.load(new FileInputStream(configFilename))
         AlignerParams.parseParameters(prop)
    }
    
    def initModel(featureAlphabet: Alphabet) {
        
         crf = new NestedLinearChainCRF(featureAlphabet, FeatureExtractors, labelAlphabet = this.labelAlphabet)
         if (crf.numF == 0) {
             log.fatal("Feature Count is 0! Call extractFeatures() first before initializing the model!")
             System.exit(-1)
         }
    }
    
}
  
object FlatPhraseAligner {
    
    def main(args: Array[String]): Unit = {
        
        val aligner = new FlatPhraseAligner()
        
        aligner.initParams()
        var tiny = true
        
        aligner.eclipseMode = false
        
        val edingburgh = true
        
        aligner.parseArguments(args)
        
        var devData:AlignTrainData = null
        
        if (aligner.trainFilename == null && !aligner.eclipseMode)
        	aligner.readModel(aligner.modelFilename)
    	else {
	        if (aligner.eclipseMode) {
	            aligner.transpose = false
		        if (tiny) {
		            if (edingburgh) {
			        	//aligner.trainFilename = aligner.dataDirEdinburgh + "tiny.phrase.sure.json"
			        	//aligner.devFilename = aligner.dataDirEdinburgh + "tiny.phrase.sure.json"
			        	aligner.trainFilename = aligner.dataDirEdinburghPhrases + "tiny.synthetic-phrases.train.sure.json"
			        	aligner.devFilename = aligner.dataDirEdinburghPhrases + "tiny.synthetic-phrases.train.sure.json"
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
				aligner.modelFilename = "/tmp/flatPhraseAligner.model"
	        }
	        
			val trainData = new AlignTrainData(aligner.trainFilename, aligner.transpose, labelAlphabet=aligner.getLabelAlphabet)
			AlignTrainRecord.printAlignStat()
			
	        val featureAlphabet = new Alphabet()
			aligner.extractFeatures(trainData, featureAlphabet)
			
			// can only be called *after* extracting features on the training data
			// 'cause feature extraction also sets the max possible state index.
			aligner.getLabelAlphabet.freeze()
	        
	        println("Total Features: " + featureAlphabet.size)
	        println("Total Labels: " + aligner.getLabelAlphabet.totalStates)
   	    
			devData = if (aligner.devFilename!=null) new AlignTrainData(aligner.devFilename, aligner.transpose, labelAlphabet=aligner.getLabelAlphabet) else null
			
	        if (devData!=null) {
	        	AlignerParams.train = false
	        	aligner.extractFeatures(devData, featureAlphabet)
	        	AlignerParams.train = true
	        }
   	    
	        aligner.initModel(featureAlphabet)
	        aligner.crf.train(trainData.getTrainData, devData, aligner.modelFilename)
	        if (aligner.modelFilename != null)
	        	aligner.saveModel(aligner.modelFilename)
    	}
        
        aligner.crf.printFeatureWeights()
        
        if (aligner.alignFilename != null) {
			val s = System.nanoTime
            val total_align = aligner.decode(aligner.alignFilename, aligner.outputFilename)
			val speed_in_ms = (System.nanoTime - s) *1.0/ total_align / 1e6
			val speed_in_num = 1000.0 / speed_in_ms
			println(f"Decoding time: $speed_in_ms%.2f ms per alignment ($speed_in_num%.2f alignments per seconds)")
			
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
			val testData = new AlignTrainData(aligner.testFilename, aligner.transpose, labelAlphabet=aligner.getLabelAlphabet)
			
	        
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