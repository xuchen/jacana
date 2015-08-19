/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet.NULL_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignSequence

/**
 * Any feature function should extend this abstract class and implement at least
 * the [[addPhraseBasedFeature]] method (for phrase-based align) or 
 * [[addTokenBasedFeature]] method (for token-based align).
 * 
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(1L)
abstract class AlignFeature  extends Serializable {
    
    
    /**
     * override this function for any heavy startup overhead of this feature
     *
     */ 
    def init() { }
    
    /**
     * extract features for a pair of '''tokens'''.
     * 
     * ''this function should'' '''only''' ''be overridden if user-defined [[addPhraseBasedFeature]]
     * function isn't general enough to handle single-token cases (when srcSpan=tgtSpan=1).''
     * 
     * implementing method should call the following to add features:
     * 
     * <code>ins.addFeature(featureName, prevState, currState, featureValue, featureAlphabet)</code>
     * 
     * <code>prevState</code> is normally [[edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE]](-1) for non-Markovian features
     * 
     * @param i [0, srcLen)
     * @param j [-1, tgtLen)  when j == -1, it aligns to null (deletion)
     * 
     */
    def addTokenBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, j:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
        addPhraseBasedFeature(pair, ins, i, 1, j, 1, currState, featureAlphabet, labelAlphabet)
    }
    
    /**
     * extract features for a pair of '''phrases'''.
     * 
     * implementing method should call the following to add features:
     * 
     * <code>ins.addFeature(featureName, prevState, currState, featureValue, srcSpan, featureAlphabet)</code>
     * 
     * <code>prevState</code> is normally [[edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE]](-1) for non-Markovian features
     * 
     * @param i [0, srcLen)
     * @param srcSpan [1, maxSourcePhraseLen] respecting the constraint that i+srcSpan doesn't exceed sentence boundary
     * @param j [-1, tgtLen) when j == -1, it aligns to null (deletion)
     * @param tgtSpan [1, maxTargetPhraseLen] respecting the constraint that i+tgtSpan doesn't exceed sentence boundary
     * 
     */   
    def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet);
    
    /**
     * enumerates every token/phrase pairs and call [[addTokenBasedFeature]]
     * or [[addPhraseBasedFeature]] to add features.
     * 
     */
    def extract(record: AlignSequence, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet):Array[AlignFeatureVector] = {
        val pair = record.getPair
        var fv:Array[AlignFeatureVector] = null 
        if (this.isInstanceOf[AlignFeatureOrderOne]) {
            fv = new Array[AlignFeatureVector](pair.srcLen)
            for (i <- 0 until fv.length)
                fv(i) = new AlignFeatureVector
        } else {
            fv = pair.featureVectors
        }
        if (!AlignerParams.phraseBased) {
            // token-based align
          if (AlignerParams.parallel) {
            val iListPar = (0 until pair.srcLen).par
            // val jListPar = (-1 until pair.tgtLen).par
            // for {i <- iListPar; j <- jListPar} {
            //     addTokenBasedFeature(pair, fv(i), i, j, j+1, featureAlphabet)
            // }
            for {i <- iListPar} {
	                var j = -1
	                while (j < pair.tgtLen) {
	                    addTokenBasedFeature(pair, fv(i), i, j, j+1, featureAlphabet, labelAlphabet)
	                    j += 1
	                }
            }
          } else {
              var i = 0
	            while (i < pair.srcLen) {
	                val ins = fv(i)
	                var j = -1
	                while (j < pair.tgtLen) {
	                    addTokenBasedFeature(pair, ins, i, j, j+1, featureAlphabet, labelAlphabet)
	                    j += 1
	                }
	                i += 1
	            }
          }
	    } else {
            // phrase-based align
	        var i = pair.srcLen - 1
	        while (i >= 0) {
	            val ins = fv(i)
	            var j = pair.tgtLen - 1
	            while (j >= -1) {
	                var srcSpan = 1
	                while (srcSpan <= Math.min(AlignerParams.maxSourcePhraseLen, i+1)) {
			            val srcToken = pair.srcTokens.slice(i-srcSpan+1, i+1).mkString("")
	                    var tgtSpan = 1
		                if (j == -1) {
		                   // previous state: -1, current state: 0 (NULL)
		                   //ins.addFeature(ALIGN2NULL, NONE_STATE, NULL_STATE, 1.0, srcSpan) 
		                   if (srcSpan == 1 && tgtSpan == 1)
		                	   addTokenBasedFeature(pair, ins, i, -1, NULL_STATE, featureAlphabet, labelAlphabet)
	                	   else
	                		   addPhraseBasedFeature(pair, ins, i-srcSpan+1, srcSpan, -1, 1, NULL_STATE, featureAlphabet, labelAlphabet)
		                } else {
			                while (tgtSpan <= Math.min(AlignerParams.maxTargetPhraseLen, j+1)) {
			                    val posJ =j-tgtSpan+1
			                	val tgtToken = pair.tgtTokens.slice(posJ, j+1).mkString("")
			                	
			                	// IMPORTANT IMPORTANT IMPORTANT
			                	// while we are doing a backward scan (from tgtLen to 1)
			                	// getMergedStateIdx accepts a forward assignment (from posJ+1 forward by tgtSpan)
			                	val state = labelAlphabet.getMergedStateIdx(posJ+1, pair.tgtLen, tgtSpan)
			                	
			                	// sanity check
			                	//val (pos,span) = IndexLabelAlphabet.getPosAndSpanByStateIdx(state, pair.tgtLen)
			                	//println(f"$pos=$posJ+1, $span=$tgtSpan")
			                	
			                	// WRONG code:
			                	// val state = IndexLabelAlphabet.getMergedStateIdx(j+1, pair.tgtLen, tgtSpan)
			                	
			                   if (srcSpan == 1 && tgtSpan == 1)
				                   addTokenBasedFeature(pair, ins, i, j, j+1, featureAlphabet, labelAlphabet)
		                	   else
		                		   addPhraseBasedFeature(pair, ins, i-srcSpan+1, srcSpan, posJ, tgtSpan, state, featureAlphabet, labelAlphabet)
		                	
			                    tgtSpan += 1
			                }
		                }
	                    srcSpan += 1
	                }
	                j -= 1
	            }
	            i -= 1
	        }

	    }
        return fv
    }
}
