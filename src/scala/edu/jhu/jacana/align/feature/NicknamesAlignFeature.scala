/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.resource.Nicknames
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * @author Xuchen Yao
 *
 */
object NicknamesAlignFeature extends AlignFeature {
    val NAME_SIM = "nicknames"
    
    override def init() { 
        // fireup Word2VecWrapper the first time
        Nicknames.isNickname("a", "a")
    }
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
 		if (j == -1) {
		} else {
		    if (srcSpan == 1 && tgtSpan == 1)
		    	ins.addFeature(NAME_SIM, NONE_STATE, currState, if (Nicknames.isNickname(pair.srcTokens(i), pair.tgtTokens(j))) 1.0 else 0.0, srcSpan, featureAlphabet)
	    	else {
	    	}
		}
	
	}       
}