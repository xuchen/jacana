/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet.NULL_STATE
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * Implements the "relative sentence position" features from Blunsom and Cohn (2006),
 * abs(At/|e| - t/|f|), that allows the model to learn a preference for aligning words
 * close to the matrix diagonal. 
 * 
 * (I suppose) This should work better for the Paraphrase08CCL corpus than RTE06MSR.
 * 
 * @author Xuchen Yao
 *
 */
object PositionalAlignFeature extends AlignFeature {
	object Types {
	    // align from source to target
	    val POSITION = "align_position.pos2pos"
	    val POSITION_RELATIVE = "align_position.pos2pos.relative"
	    val POSITION_NULL = "align_position.pos2null"
	}

	import Types._
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
		if (j == -1) {
			ins.addFeature(POSITION_NULL, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		} else {
            ins.addFeature(POSITION, NONE_STATE, currState, Math.abs((i+srcSpan) - (j+tgtSpan)), srcSpan, featureAlphabet)
            ins.addFeature(POSITION_RELATIVE, NONE_STATE, currState, Math.abs((i+srcSpan-1)*1.0/pair.srcLen - (j+tgtSpan-1)*1.0/pair.tgtLen), srcSpan, featureAlphabet)
		}
	}
    
}