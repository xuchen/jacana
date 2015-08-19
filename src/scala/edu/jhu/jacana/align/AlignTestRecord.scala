/**
 *
 */
package edu.jhu.jacana.align

import edu.jhu.jacana.align.util.AlignerParams
import gnu.trove.map.hash.TIntObjectHashMap
import scala.collection.mutable.HashSet
import java.util.Arrays

/**
 * Construct a test record from a sentence pair
 * if the POS tags and chunks are given, then don't process the sentences with OpenNLP (can be somewhat slow)
 *
 * @author Xuchen Yao
 *
 */
class AlignTestRecord (sent1: String, sent2: String, tokenize:Boolean = true, 
                id:String="", sent1pos:Array[String] = null, sent1chunk:Array[String] = null,
                sent2pos:Array[String] = null, sent2chunk:Array[String] = null,
                labelAlphabet:IndexLabelAlphabet) extends SegmentAlignSequence {
    
    var pair = new AlignPair(id, sent1, sent2, process = true, tokenize, sent1pos, sent1chunk, sent2pos, sent2chunk)
    private var numTokens = pair.srcLen
    
    // each token's label
    private var labelsPerToken = new Array[Int](numTokens)
    
    private var numSeg = 1
    
    // which segment each token belongs to, "snum"
    private var tokensBySegment = new Array[Int](numTokens)
    
    // class labels for each segment
    private var _labels = new Array[Int](numSeg)
    // starting position for each segment, "spos"
    private var segmentPosition = new Array[Int](numSeg)
    
    private var currentSegment = 0
    
    def getPair = pair
    
    /** get the end position of the segment starting at segmentStart */
    def getSegmentEnd(segmentStart: Int): Int = {
        throw new NotImplementedError("This won't be called except in training (but this is Align*Test*Record)!")
    } 
    
    /** set segment boundary and label */
    def setSegment(segmentStart: Int, segmentEnd: Int, y:Int) {
        for (i <- segmentStart to segmentEnd)
            set_y(i, y)
    }
    
    def length(): Int = { numTokens}
    def y_length(): Int = { 
        if (!AlignerParams.phraseBased)
        	return pair.tgtLen
    	else
        	return labelAlphabet.getMaxMergedStateIdx(pair.tgtLen)
    }
    
    def y(tokenIdx:Int): Int = { labelsPerToken(tokenIdx)}
    
    /** The type of x is never interpreted by the CRF package. 
     * This could be useful for your FeatureGenerator class */ 
    def x(i:Int): Object = {
        throw new NotImplementedError("")
    }
    
    def zero_y() { Arrays.fill(labelsPerToken, 0) }
    
    def set_y(tokenIdx:Int, label:Int) {labelsPerToken(tokenIdx) = label} // not applicable for training data.
    
    def set_score(s:Double) {pair.setScore(s)}
    def toMsrFormat: String = {
        
        // warning: we can't call pair.toMsrFormat here during *test*
        // since the decoded labels are assigned to labelsPerToken by set_y()
        // if calling the following, then we'd write out the gold labels
        //return pair.toMsrFormat
        
        // already knowing what the source aligns to through labelsPerToken,
        // we output what the target aligns to in MSR format.
        // note that multiple source tokens can align to the same target 
        val tgt2src = new TIntObjectHashMap[HashSet[Integer]]()
        labelsPerToken.view.zipWithIndex foreach  { 
            case (tgt,src) if tgt > 0 => {
            	val (pos, span) = IndexLabelAlphabet.getPosAndSpanByStateIdx(tgt, pair.tgtLen) 
            	// pos returned starts from 1
                var tgtIdx = pos-1
	            while (tgtIdx < pos-1+span) {
    	            if (!tgt2src.containsKey(tgtIdx)) {
    	                tgt2src.put(tgtIdx, new HashSet[Integer]())
    	            }
    	            tgt2src.get(tgtIdx).add(src+1)
		            tgtIdx += 1
	            }
        	}
        	case _ =>
        }
        var sb = new StringBuilder()
		sb.append("# sentence pair " + pair.id);
        sb.append("\n")
        sb.append(pair.src)
        sb.append("\n")
		sb.append("NULL ({ / / }) ")
		pair.tgtTokens.view.zipWithIndex foreach { case (token, tgt) =>
		    sb.append(token + " ")
			if (tgt2src.containsKey(tgt)) {
			    sb.append("({ %s / / }) ".format(tgt2src.get(tgt).mkString(" ")))
			}
			else {
				sb.append("({ / / }) ");
			}
        }

        sb.append("\n")
        return sb.toString
    }
    
    private def fillInAlignMatrix() {
         for ((y,x) <- labelsPerToken.view.zipWithIndex) {
            if (y != 0) {
                // deletion is not printed
            	val (pos, span) = IndexLabelAlphabet.getPosAndSpanByStateIdx(y, pair.tgtLen) 
            	// pos returned starts from 1
                var c = pos-1
	            while (c < pos-1+span) {
	            	pair.alignMatrix(x)(c) = 1
		            c += 1
	            }
            }
        }     
    }
    
    def getDashAlign(transpose:Boolean = false):String = {
      fillInAlignMatrix()
      return pair.getDashedAlign(transpose)
    }
    
    def toJSON(name:String = null): String = {
        fillInAlignMatrix()
        return pair.toJSON(name)
    }
}