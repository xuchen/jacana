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
import com.rockymadden.stringmetric.similarity.JaroWinklerMetric
import scala.collection.immutable.HashSet
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * Check whether the left/right neighbors token of an aligned pair
 * match or are similar.
 * 
 * Some function words have multiple aligning candidates, this feature
 * tries to put them into the 'right' context.
 *  
 * @author Xuchen Yao
 *
 */
object ContextualAlignFeature extends AlignFeature {
	object Types {
	    // neighboring words match
	    val LEFT_MATCH = "match.left"
	    val RIGHT_MATCH = "match.right"
	    val LEFT_SIMILAR = "similar.left"
	    val RIGHT_SIMILAR = "similar.right"
	        
	    // neighboring pos tags match
	    val POS_LEFT_MATCH = "match.pos.left"
	    val POS_RIGHT_MATCH = "match.pos.right"
	        
	    // current is a functional word and neighbors match
	    val LEFT_MATCH_FUNCTIONAL = "match.functional.left"
	    val RIGHT_MATCH_FUNCTIONAL = "match.functional.right"
	    val LEFT_SIMILAR_FUNCTIONAL = "similar.functional.left"
	    val RIGHT_SIMILAR_FUNCTIONAL = "similar.functional.right"
	        
	    // current is a functional word and neighboring pos tags match
	    val POS_LEFT_MATCH_FUNCTIONAL = "match.pos.functional.left"
	    val POS_RIGHT_MATCH_FUNCTIONAL = "match.pos.functional.right"
	}

	import Types._
	
	val threshold = 0.7
	
	// POS neighboring features should make sense but in a small dev set they 
	// decrease precision, setting a switch here
	val usePosNeighboringMatching = false
	
	val functionalWords = HashSet() ++ List("cc", "cd", "md", "dt", "pdt", "wdt", "in", "to", "rp")
	//val functionalWords = HashSet() ++ List("dt", "in", "to")
	
	def isFunctionalWord(pos: String) = functionalWords.contains(pos.toLowerCase())
	
	def posMatch(pos1: String, pos2: String):Boolean = {
	    if (pos1.length() >= 2 && pos2.length() >= 2)
	    	// only check the prefix, so that NN* match and VB* match
	    	pos1.substring(0,2) == pos2.subSequence(0, 2)
    	else
    	    // punctuation
    	    pos1 == pos2
	}
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
		if (j == -1) {
		} else {
			val bothFunctionalWords = isFunctionalWord(pair.srcPos(i)) && isFunctionalWord(pair.tgtPos(j))
	        var averageLength = (if (srcSpan < tgtSpan) 0.5 * (srcSpan + tgtSpan) else srcSpan)*AlignerParams.phraseWeight
	        //var averageLength = if (srcSpan < tgtSpan) srcSpan else 0.5 * (srcSpan + tgtSpan)
		    if (i-1 >= 0 && j-1 >= 0) {
		        if (pair.srcTokens(i-1).toLowerCase() == pair.tgtTokens(j-1).toLowerCase()) {
		        	ins.addFeature(LEFT_MATCH, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        	if (bothFunctionalWords)
		        		ins.addFeature(LEFT_MATCH_FUNCTIONAL, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        }
		        if (usePosNeighboringMatching && posMatch(pair.srcPos(i-1), pair.tgtPos(j-1))) {
		        	ins.addFeature(POS_LEFT_MATCH, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        	if (bothFunctionalWords)
		        		ins.addFeature(POS_LEFT_MATCH_FUNCTIONAL, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        }
		        if (JaroWinklerMetric.compare(pair.srcTokens(i-1), pair.tgtTokens(j-1)).get > threshold) {
		        	ins.addFeature(LEFT_SIMILAR, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        	if (bothFunctionalWords)
		        		ins.addFeature(LEFT_SIMILAR_FUNCTIONAL, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        }
		        //ins.addFeature(LEFT_SIMILAR, NONE_STATE, currState, JaroWinklerMetric.compare(pair.srcTokens(i-1), pair.tgtTokens(j-1)).get, srcSpan, featureAlphabet)
		    }
            if (i+srcSpan < pair.srcLen && j+tgtSpan < pair.tgtLen) {
		        if (pair.srcTokens(i+srcSpan).toLowerCase() == pair.tgtTokens(j+tgtSpan).toLowerCase()) {
		        	ins.addFeature(RIGHT_MATCH, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        	if (bothFunctionalWords)
		        		ins.addFeature(RIGHT_MATCH_FUNCTIONAL, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        }
		        if (usePosNeighboringMatching && posMatch(pair.srcPos(i+srcSpan), pair.tgtPos(j+tgtSpan))) {
		        	ins.addFeature(POS_RIGHT_MATCH, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        	if (bothFunctionalWords)
		        		ins.addFeature(POS_RIGHT_MATCH_FUNCTIONAL, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        }
		        if (JaroWinklerMetric.compare(pair.srcTokens(i+srcSpan), pair.tgtTokens(j+tgtSpan)).get > threshold) {
		        	ins.addFeature(RIGHT_SIMILAR, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        	if (bothFunctionalWords)
		        		ins.addFeature(RIGHT_SIMILAR_FUNCTIONAL, NONE_STATE, currState, 1.0*averageLength, srcSpan, featureAlphabet)
		        }
		        //ins.addFeature(LEFT_SIMILAR, NONE_STATE, currState, JaroWinklerMetric.compare(pair.srcTokens(i+1), pair.tgtTokens(j+1)).get, srcSpan, featureAlphabet)
            }
		}
	}
    
}