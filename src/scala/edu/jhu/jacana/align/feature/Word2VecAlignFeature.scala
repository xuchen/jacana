/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.resource.Word2VecWrapper
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(-2180620883239065092L)
object Word2VecAlignFeature extends AlignFeature {
    val NAME_SIM = "word2vec"
    
    override def init() { 
        // fireup Word2VecWrapper the first time
        Word2VecWrapper.getSimilarity("a", "a")
    }
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
 		if (j == -1) {
		} else {
		    if (srcSpan == 1 && tgtSpan == 1)
		    	ins.addFeature(NAME_SIM, NONE_STATE, currState, Word2VecWrapper.getSimilarity(pair.srcTokens(i), pair.tgtTokens(j)), srcSpan, featureAlphabet)
	    	else {
	    	    // very slow, don't enable it
	    		// var averageLength = (if (srcSpan < tgtSpan) 0.5 * (srcSpan + tgtSpan) else srcSpan)*AlignerParams.phraseWeight
		    	// val srcTokens = pair.srcTokens.slice(i, i+srcSpan)
		    	// val tgtTokens = pair.tgtTokens.slice(j, j+tgtSpan)
		    	// ins.addFeature(NAME_SIM, NONE_STATE, currState, averageLength * Word2VecWrapper.getAverageSimilarity(srcTokens, tgtTokens), srcSpan, featureAlphabet)
	    	}
		}
	
	}       
}