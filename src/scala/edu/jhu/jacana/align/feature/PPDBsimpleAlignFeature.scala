package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.resource.PPDBsimple
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet

@SerialVersionUID(-7679538554893386966L)
object PPDBsimpleAlignFeature extends AlignFeature {
    
    val FEAT_NAME = "PPDBsimple"

    override def init() { 
        // fireup PPDB the first time
        PPDBsimple.isInPPDB("a", "a")
    }
    
 	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
 		if (j == -1) {
		} else {
		    if (!AlignerParams.phraseBased) {
		    	if (srcSpan == 1 && tgtSpan == 1)
		    		ins.addFeature(FEAT_NAME, NONE_STATE, currState, PPDBsimple.isInPPDB(pair.srcTokens(i), pair.tgtTokens(j)), srcSpan, featureAlphabet)
		    } else {
		    	val srcToken = pair.srcTokens.slice(i, i+srcSpan).mkString("")
		    	val tgtToken = pair.tgtTokens.slice(j, j+tgtSpan).mkString("")
		    	var averageLength = (if (srcSpan < tgtSpan) 0.5 * (srcSpan + tgtSpan) else srcSpan)*AlignerParams.phraseWeight
		    	ins.addFeature(FEAT_NAME, NONE_STATE, currState, averageLength*PPDBsimple.isInPPDB(srcToken, tgtToken), srcSpan, featureAlphabet)
		    }
		}
	
	}
}