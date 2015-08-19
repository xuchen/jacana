/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet.NULL_STATE
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * Markovian features that model the distortion between a_i and a_{i+1}
 * 
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(-7527394948986634863L)
object DistortionAlignFeature extends AlignFeatureOrderOne {
	object Types {
        val START_2NULL = "start.2null"
        val START_2TGT = "start.2tgt"
        val MIDDLE_2NULL = "middle.2null"
        val MIDDLE_2TGT = "middle.2tgt"
        val END_2NULL = "end.2null"
        val END_2TGT = "end.2tgt"
        val EDGE_NULL2NULL = "edge.null2null"
        val EDGE_NULL2ALIGN = "edge.null2align"
        val EDGE_ALIGN2NULL = "edge.align2null"
        val EDGE_ALIGN2ALIGN = "edge.align2align"
        val EDGE_ALIGN2ALIGN_RELATIVE = "edge.align2align.relative"
            
        // expect to learn positive weights for monotonic alignment
        val EDGE_MONOTONIC_POSITIVE = "edge.monotonic.positive"
        // expect to learn negative weights for non-monotonic alignment
        val EDGE_MONOTONIC_NEGATIVE = "edge.monotonic.negative"
        // expect to learn negative weights for multiple source words aligning to the same target words
        val EDGE_MONOTONIC_ZERO = "edge.monotonic.zero"
            
        val EDGE_MONOTONIC_OFF_BY_ONE = "edge.monotonic.off_by_1"
	}
	import Types._
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
	    val srcEnd = pair.srcLen-1
		(i, j) match {
	        // TODO: think about the case where when i = 0 but srcSpan > 1, should we still add START2TGT?
		    case (0, -1) => ins.addFeature(START_2NULL, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		    case (0, x) if x > -1 => ins.addFeature(START_2TGT, NONE_STATE, currState, (pair.tgtLen-j-1)*1.0/pair.tgtLen, srcSpan, featureAlphabet)
		    // http://stackoverflow.com/questions/7078022/why-does-pattern-matching-in-scala-not-work-with-variables
		    case (`srcEnd`, -1) => ins.addFeature(END_2NULL, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		    case (`srcEnd`, x) if x > -1 => ins.addFeature(END_2TGT, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		    case (x, -1) if x > 0 => ins.addFeature(MIDDLE_2NULL, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		    case _ => ins.addFeature(MIDDLE_2TGT, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		}     
       if (i != 0) {
           // transitional (distortion) features
            var k = 0
            // k is the previous word's alignment (previous state)
            // for phrase-based alignment, assume the previous state is only a single token, not a phrase
            while (true && k < labelAlphabet.getMaxMergedStateIdx(pair.tgtLen)) {
            	var (prevJ, prevSpan) = IndexLabelAlphabet.getPosAndSpanByStateIdx(k, pair.tgtLen) 
                // getPosAndSpanByStateIdx uses 0 as deletion while here we use -1 as deletion (so that 0 represents the first word in tgt sentence)
            	prevJ -= 1
                // TODO: BUGFIX: k should range to all states instead of only tgtLen
                (prevJ, j) match {
                    case (-1, -1) => ins.addFeature(EDGE_NULL2NULL, NULL_STATE, NULL_STATE, 1.0, srcSpan, featureAlphabet)
                    case (-1, x) if x > -1 => ins.addFeature(EDGE_NULL2ALIGN, NULL_STATE, currState, (pair.tgtLen-j-1)*1.0/pair.tgtLen, srcSpan, featureAlphabet)
                    case (x, -1) if x > -1 => ins.addFeature(EDGE_ALIGN2NULL, k, currState, (pair.tgtLen-prevJ-1)*1.0/pair.tgtLen, srcSpan, featureAlphabet)
                    case _ => {
                        ins.addFeature(EDGE_ALIGN2ALIGN_RELATIVE, k, currState, Math.abs(j - prevJ - prevSpan - pair.tgtLen)*1.0/pair.tgtLen, srcSpan, featureAlphabet)
                    }
                }
            	if (k != 0 && currState != 0) {
	            	if (prevJ + prevSpan -1 < j)
	                    ins.addFeature(EDGE_MONOTONIC_POSITIVE, k, currState, 1.0, srcSpan, featureAlphabet)
	            	else if (prevJ + prevSpan -1 > j)
	                    ins.addFeature(EDGE_MONOTONIC_NEGATIVE, k, currState, 1.0, srcSpan, featureAlphabet)
	                else
	                	ins.addFeature(EDGE_MONOTONIC_ZERO, k, currState, 1.0, srcSpan, featureAlphabet)
	                	
	            	if (prevJ + prevSpan -1 - j == -1)
	                    ins.addFeature(EDGE_MONOTONIC_OFF_BY_ONE, k, currState, 1.0, srcSpan, featureAlphabet)
            	}
                k += 1
            }
       }
	}
}