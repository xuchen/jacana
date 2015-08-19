/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet.NULL_STATE
import edu.jhu.jacana.align.util.AlignerParams
import scala.collection.immutable.HashSet
import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * capturing whether the pos tags of two words match and what are the pos tags of aligned words.
 * 
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(5761390368708268405L)
object PosTagAlignFeature extends AlignFeature {
	object Types {
	    val POS_MATCH = "pos.match"
	    val POS_NO_MATCH = "pos.no_match"
	    val POS_MAP = "pos.map."
	    val POS_PHRASE_INTERSECT = "pos.phrase_intersect"
	}

	import Types._
    
 	override def addTokenBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, j:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
        val srcPos = pair.srcPos(i)
		if (j == -1) {
			ins.addFeature(POS_MAP+srcPos+"-null", NONE_STATE, currState, 1.0, 1, featureAlphabet) 
		} else {
           val tgtPos = pair.tgtPos(j)
           if (srcPos == tgtPos)
        	   ins.addFeature(POS_MATCH, NONE_STATE, currState, 1.0, 1, featureAlphabet)
    	   else
        	   ins.addFeature(POS_NO_MATCH, NONE_STATE, currState, 1.0, 1, featureAlphabet)
       	   //ins.addFeature(POS_MAP+srcPos+"-"+tgtPos, NONE_STATE, currState, 1.0, 1, featureAlphabet)
       	   if (srcPos == tgtPos)
       	       ins.addFeature(POS_MAP+srcPos+"-"+tgtPos, NONE_STATE, currState, 1.0, 1, featureAlphabet)
       	   else
       	       ins.addFeature(POS_MAP+srcPos+"-OtherPOS", NONE_STATE, currState, 1.0, 1, featureAlphabet)
		}
    }
	
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
	    if (srcSpan == 1 && tgtSpan == 1)
	        return addTokenBasedFeature(pair, ins, i, j, currState, featureAlphabet, labelAlphabet)
        val srcPosArray = pair.srcPos.slice(i, i+srcSpan)
        val srcPos = srcPosArray.mkString("-")
        if (j == -1) {
			ins.addFeature(POS_MAP+srcPos+"-null", NONE_STATE, currState, 1.0, srcSpan, featureAlphabet) 
        } else {
        	val tgtPosArray = pair.tgtPos.slice(j, j+tgtSpan)
        	val tgtPos = tgtPosArray.mkString("-")
        	if (srcPos == tgtPos)
        		ins.addFeature(POS_MATCH, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet) 
        	else {
        	    ins.addFeature(POS_NO_MATCH, NONE_STATE, j+1, 1.0, srcSpan, featureAlphabet)
        	}
       	    ins.addFeature(POS_PHRASE_INTERSECT, NONE_STATE, currState, intersectScore(srcPosArray, tgtPosArray), srcSpan, featureAlphabet)
        }
	}
	
	def intersectScore(l1: Array[String], l2: Array[String]): Double = {
	    //val max = Math.max(l1.length, l2.length)
	    val set1 = l1.toSet[String]
	    var counter = 0
	    for (x <- l2) {
	        if (set1.contains(x))
	            counter += 1
	    }
	    
	    //return counter*1.0/max
	    return counter*1.0
	    
	}
}