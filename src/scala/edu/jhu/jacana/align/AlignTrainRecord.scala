/**
 *
 */
package edu.jhu.jacana.align

import gnu.trove.map.hash.TIntObjectHashMap
import gnu.trove.set.hash.TIntHashSet
import scala.collection.mutable.HashSet
import edu.jhu.jacana.align.util.AlignerParams
import java.util.Arrays
import edu.jhu.jacana.align.aligner.FlatAligner
import edu.jhu.jacana.util.FileManager

/**
 * an AlignTrainRecord is actually an AlignPair implementing the
 * TrainRecord interface. It has two layers of abstraction here:
 * segment and token
 * @author Xuchen Yao
 *
 */


class AlignTrainRecord (pair: AlignPair, labelAlphabet:IndexLabelAlphabet) extends TrainAlignRecord {
    import AlignTrainRecord._
    
    private var numTokens = pair.srcLen
    
    // each token's label
    private var labelsPerToken = new Array[Int](numTokens)
    
    private var numSeg = 1
    
    // which segment each token belongs to, "snum"
    private var tokensBySegment = new Array[Int](numTokens)
    
    if (!AlignerParams.phraseBased) {
	    for (i <- 0 until numTokens) {
	         pair.alignMatrix(i).toList.indexOf(1) match {
	            case -1 => labelsPerToken(i) = 0
	            case m if m > -1 => {
		            // use the first align for now, later we might think of 
		            // smarter ways to do it. (TODO)
                	labelsPerToken(i) = m + 1; labelAlphabet.get((m+1))
   	                src2tgtCount(1)(1) += 1
	            }
	        }       
	
	    }
	    
	    for (i <- 0 until numTokens) {
	        if (i != 0) {
	            if (labelsPerToken(i) == 0) {
	                // deleted word is a segment of its own
	                numSeg += 1
	            } else if (labelsPerToken(i) != labelsPerToken(i-1)) {
	                numSeg += 1
	            }
	        }
	        tokensBySegment(i) = numSeg - 1
	    }
     
    } else {
        val a = pair.alignMatrix
        var i = 0
        var maxI = 0
        //IndexLabelAlphabet.setMaxStateIdx(pair.tgtLen)
        while (i < pair.srcLen) {
            maxI = i+1
            var j = 0
            var maxJ = 0
            var allZeros = true
            while (j < pair.tgtLen && allZeros) {
                maxJ = j+1
                if (a(i)(j) == 1) {
                    allZeros = false
                    var expandI = true; var expandJ = true;
                    while (expandI || expandJ) {
                        if (expandI) {
                            if (maxI == pair.srcLen || maxI-i >= AlignerParams.maxSourcePhraseLen) {
                                expandI = false
                            } else {
                            	if ((maxI-i+1) * (maxJ-j) == pair.sumRectAt(i, maxI+1, j, maxJ)) {
                            	    maxI += 1
                            	} else
                            	    expandI = false
                            }
                        }
                        if (expandJ) {
                            if (maxJ == pair.tgtLen || maxJ-j >= AlignerParams.maxTargetPhraseLen) {
                                expandJ = false
                            } else {
                            	if ((maxI-i) * (maxJ-j+1) == pair.sumRectAt(i, maxI, j, maxJ+1)) {
                            	    maxJ += 1
                            	} else
                            	    expandJ = false
                            }
                        }
                    }
                } else {
                    j = maxJ
                }
            }
            if (allZeros) {
                labelsPerToken(i) = 0
                src2tgtCount(1)(0) += 1
            } else {
                var stateID = labelAlphabet.getMergedStateIdx(j+1, pair.tgtLen, maxJ-j)
                for (ii <- i until maxI)
                	labelsPerToken(ii) = stateID
                src2tgtCount(maxI-i)(maxJ-j) += 1
                // if (maxI-i > 1 || maxJ - j > 1)
                // 	println("%dx%d %s <-> %s".format(maxI-i, maxJ-j, pair.srcTokens.slice(i, maxI).mkString(" "), pair.tgtTokens.slice(j, maxJ).mkString(" ")))
            }
            i = maxI
        }
            
        i = 0
        var currSegLen = 0
	    while (i < numTokens) {
	        if (i != 0) {
	            if (labelsPerToken(i) == 0) {
	                // deleted word is a segment of its own
	                numSeg += 1
	                currSegLen = 0
	            } else if (labelsPerToken(i) != labelsPerToken(i-1)) {
	                numSeg += 1
	                currSegLen = 0
	            } else if (currSegLen == AlignerParams.maxTargetPhraseLen) {
	                numSeg += 1
	                currSegLen = 0
                }
                currSegLen += 1
	        }
	        tokensBySegment(i) = numSeg - 1
	        i += 1
	    }
 
    }

    // class labels for each segment
    private var _labels = new Array[Int](numSeg)
    // starting position for each segment, "spos"
    private var segmentPosition = new Array[Int](numSeg)
    
    private var currentSegment = 0
    for (i <- 0 until numTokens) {
        _labels(tokensBySegment(i)) = labelsPerToken(i)
        if (i == 0) {
        	segmentPosition(0) = 0 
        } else {
            if (tokensBySegment(i) != tokensBySegment(i-1)) {
            	segmentPosition(tokensBySegment(i)) = i
            }
        }
    }
    
    // 2D arrays holding segments in rows, and tokens for each segment in columns
    private var _tokens = new Array[Array[String]](numSeg)
    
    for (i <- 0 until numSeg) {
        _tokens(i) = pair.srcTokens.slice(segmentPosition(i), if (i != numSeg-1) segmentPosition(i+1) else numTokens)
    }
    
    private val goldLabelsPerToken = labelsPerToken.clone

    def getPair = pair
    // Array(B-NP, I-NP, B-VP, B-PP, B-NP, B-NP, I-NP, I-NP, I-NP, O)
    
    def numSegments(): Int = {return numSeg} // number of segments in the record
    
    def labels():Array[Int] = {return _labels} // labels of each segment
    
    def getLabelsPerToken(): Array[Int] = {labelsPerToken}

    def getGoldLabelsPerToken(): Array[Int] = {goldLabelsPerToken}
    
    def tokens(segmentNum: Int): Array[String] = {return _tokens(segmentNum)} // array of tokens in this segment  
    
    def set_score(s:Double) {pair.setScore(s)}

    // number of segments of given label
    // this is a very confusing method name
    def numSegments(label: Int):Int = {
        var num = 0
        for (i <- 0 until numSeg)
            if (_labels(i) == label)
                num += 1
        return num
    } 
    
    // the array of segment of given label and token
    def tokens(label: Int, i: Int): Array[String] = {
        var pos = 0
        for (i <- 0 until numSeg) {
            if (_labels(i) == label) {
                if (pos == i)
	                return _tokens(i)
	            pos += 1
            }
                
        }
        return null
    } 
    
    /** get the end position of the segment starting at segmentStart */
    def getSegmentEnd(segmentStart: Int): Int = {
        val currSeg = tokensBySegment(segmentStart) 
        for (i <- segmentStart+1 until numTokens) {
            if (currSeg != tokensBySegment(i))
                return i-1
        }
        return numTokens-1
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
        if (i < 0)
            return pair
        else
        	return  _tokens(tokensBySegment(i))(i - segmentPosition(tokensBySegment(i)))
    }
    
    def zero_y() { Arrays.fill(labelsPerToken, 0) }
    
    def set_y(tokenIdx:Int, label:Int) {labelsPerToken(tokenIdx) = label} // not applicable for training data.
    
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
	            var tgtIdx = tgt - 1
	            if (!tgt2src.containsKey(tgtIdx)) {
	                tgt2src.put(tgtIdx, new HashSet[Integer]())
	            }
	            tgt2src.get(tgtIdx).add(src+1)
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
    
    def toJSON: String = {
        var sb = new StringBuilder()
        sb.append("\t{\n")
        sb.append(AlignPair.keyValue2JSON("id", pair.id))
        sb.append(AlignPair.keyValue2JSON("name", pair.id))
        sb.append(AlignPair.keyValue2JSON("source", pair.src))
        sb.append(AlignPair.keyValue2JSON("target", pair.tgt))
        sb.append(AlignPair.keyValue2JSON("score", pair.score.toString))
        sb.append(AlignPair.keyValue2JSON("sureAlign", getDashedAlign))
        sb.append(AlignPair.keyValue2JSON("possibleAlign", "", linebreak=false))
        sb.append("\n\t}\n")
        return sb.toString       
    }
    
    // return a dashed alignment string such as "1-0 3-3"
    def getDashedAlign: String = {
        var sb = new StringBuilder()
        for (i <- 0 until labelsPerToken.length) {
            var j = labelsPerToken(i)
            if (j != 0) {
                // deletion is not printed
            	val (pos, span) = IndexLabelAlphabet.getPosAndSpanByStateIdx(j, pair.tgtLen) 
            	// pos returned starts from 1
                var c = pos-1
	            while (c < pos-1+span) {
	            	sb.append(f"$i-$c ")
		            c += 1
	            }
            }
        }
        
        return sb.toString.trim()
    }
 
    
    
}

object AlignTrainRecord {
    
    // a 2D matrix holding the counts of m:n phrases
    // e.g., cell[2][3] holds the number of 2:3 aligned phrases
    // (2 words in source align to 3 words in target) 
    // cell[1][0] holds the number of deleted words in the source
    // cell[0][0] cell[0][*] cell[>1][0] don't hold anything
    
    // TODO: when calling printAlignStat() standalone, enable the following code (don't know why)
    // AlignerParams.maxSourcePhraseLen = 8
    // AlignerParams.maxTargetPhraseLen = 8
    val src2tgtCount = Array.ofDim[Int](AlignerParams.maxSourcePhraseLen+1, AlignerParams.maxTargetPhraseLen+1)
    
    def printAlignStat() {
        var total = 0
        var i = 1
        var c = 0
        var warnOnSrc = false
        var warnOnTgt = false
        while (i < src2tgtCount.length) {
        	var j = 1
        	while (j < src2tgtCount(i).length) {
        	    c = src2tgtCount(i)(j)
        	    if (c > 0) {
	        	    total += c
	        	    println(s"$i x $j alignment: $c")
	        	    if (j == src2tgtCount(i).length-1)
	        	        warnOnTgt = true
	        	    if (i == src2tgtCount.length-1)
	        	        warnOnSrc = true
        	    }
        	    j += 1
        	}
            i += 1
        }
        println(s"total alignment: $total")
        
        // when set to do up to 4x4 alignment, there might be some alignment, e.g., 5x2
        // that were cut off due to the original 4x4 setting. Print a warning message
        // in case we are just computing the real statistics
        if (warnOnSrc) {
            println(s"you might want to increase AlignerParams.maxSourcePhraseLen " +
            		s"(${AlignerParams.maxSourcePhraseLen}) to make sure no phrases longer were cut off")
        }
        if (warnOnTgt) {
            println(s"you might want to increase AlignerParams.maxTargetPhraseLen " +
            		s"(${AlignerParams.maxTargetPhraseLen}) to make sure no phrases longer were cut off")
        }
    }
    
    def main(args: Array[String]): Unit = {
        AlignerParams.phraseBased  = true
        
        // note: enable the previous todo to run the code without an error
		// val trainData = new AlignTrainData("/Users/xuchen/Downloads/word-alignments/test-0.8/dev-0.8-annotated.json", false, false, false)
		// val trainData = new AlignTrainData("/Users/xuchen/workspace/jacana/alignment-data/mt-reference/mt-reference.dev.sure+poss.json", false, false, false)
		// val trainData = new AlignTrainData("/Users/xuchen/workspace/jacana/alignment-data/edinburgh/gold.train.sure+poss.json", false, false, false)
		// val trainData = new AlignTrainData("/Users/xuchen/workspace/jacana/alignment-data/all/msr.edinburgh.test.json", false, false)
		val trainData = new AlignTrainData("/Users/xuchen/workspace/jacana/alignment-data/msr/converted/RTE2_dev_M.align.sure+poss.json", false, false, false, new IndexLabelAlphabet())
		AlignTrainRecord.printAlignStat()
    }
    
    /*
     * some stats for the RTE2_dev data:
     * 
1 x 1 alignment: 5438
1 x 2 alignment: 68
1 x 3 alignment: 14
1 x 4 alignment: 1
1 x 5 alignment: 5
2 x 1 alignment: 91
2 x 2 alignment: 7
2 x 3 alignment: 3
3 x 1 alignment: 16
3 x 2 alignment: 2
4 x 1 alignment: 1
5 x 1 alignment: 1
total alignment: 5647

	looks like setting up to 3x3 covers most of them without sacrificing too much performance 
	
	stats for Edinburgh gold.train.sure.json data:

1 x 1 alignment: 11169
1 x 2 alignment: 189
1 x 3 alignment: 54
1 x 4 alignment: 17
1 x 5 alignment: 3
1 x 6 alignment: 1
1 x 9 alignment: 1
2 x 1 alignment: 170
2 x 2 alignment: 47
2 x 3 alignment: 14
2 x 4 alignment: 5
2 x 5 alignment: 6
3 x 1 alignment: 35
3 x 2 alignment: 23
3 x 3 alignment: 7
3 x 4 alignment: 3
3 x 5 alignment: 1
3 x 6 alignment: 1
4 x 1 alignment: 13
4 x 2 alignment: 4
4 x 3 alignment: 2
4 x 4 alignment: 2
4 x 5 alignment: 1
4 x 6 alignment: 1
4 x 7 alignment: 1
5 x 1 alignment: 2
5 x 3 alignment: 1
6 x 4 alignment: 2
7 x 3 alignment: 1
7 x 4 alignment: 1
total alignment: 11777


	stats for Edinburgh gold.train.sure+poss.json data:
	
1 x 1 alignment: 11724
1 x 2 alignment: 271
1 x 3 alignment: 77
1 x 4 alignment: 28
1 x 5 alignment: 9
1 x 6 alignment: 5
1 x 7 alignment: 4
2 x 1 alignment: 258
2 x 2 alignment: 90
2 x 3 alignment: 31
2 x 4 alignment: 10
2 x 5 alignment: 8
2 x 7 alignment: 7
3 x 1 alignment: 53
3 x 2 alignment: 37
3 x 3 alignment: 21
3 x 4 alignment: 7
3 x 5 alignment: 3
3 x 6 alignment: 2
3 x 7 alignment: 2
4 x 1 alignment: 19
4 x 2 alignment: 6
4 x 3 alignment: 5
4 x 4 alignment: 2
4 x 5 alignment: 1
4 x 6 alignment: 1
4 x 7 alignment: 3
5 x 1 alignment: 4
5 x 2 alignment: 1
5 x 3 alignment: 3
5 x 5 alignment: 1
6 x 1 alignment: 1
6 x 2 alignment: 1
6 x 4 alignment: 2
6 x 5 alignment: 1
6 x 6 alignment: 2
7 x 1 alignment: 1
7 x 3 alignment: 1
7 x 4 alignment: 2
7 x 7 alignment: 2
total alignment: 12706
you might want to increase AlignerParams.maxSourcePhraseLen (7) to make sure no phrases longer were cut off
you might want to increase AlignerParams.maxTargetPhraseLen (7) to make sure no phrases longer were cut off


Synthetized phrases:

alignment-data/msr/converted/synthetic-phrases/RTE2_dev_M.synthetic-phrases.json
1 x 1 alignment: 4494
2 x 1 alignment: 98
2 x 2 alignment: 287
3 x 1 alignment: 17
3 x 3 alignment: 98
4 x 1 alignment: 1
4 x 4 alignment: 31
5 x 1 alignment: 1
5 x 5 alignment: 5
6 x 6 alignment: 4
total alignment: 5036

alignment-data/edinburgh/synthetic-phrases/gold.synthetic-phrases.train.sure.json

1 x 1 alignment: 8162
1 x 2 alignment: 185
1 x 3 alignment: 51
1 x 4 alignment: 18
1 x 5 alignment: 3
1 x 6 alignment: 2
1 x 9 alignment: 1
2 x 1 alignment: 166
2 x 2 alignment: 822
2 x 3 alignment: 15
2 x 4 alignment: 5
2 x 5 alignment: 6
3 x 1 alignment: 35
3 x 2 alignment: 23
3 x 3 alignment: 300
3 x 4 alignment: 4
3 x 5 alignment: 1
3 x 6 alignment: 1
4 x 1 alignment: 14
4 x 2 alignment: 4
4 x 3 alignment: 2
4 x 4 alignment: 96
4 x 5 alignment: 1
4 x 6 alignment: 1
4 x 7 alignment: 1
5 x 1 alignment: 2
5 x 3 alignment: 1
5 x 5 alignment: 23
6 x 4 alignment: 2
6 x 6 alignment: 11
7 x 3 alignment: 1
7 x 4 alignment: 1
7 x 7 alignment: 1
8 x 8 alignment: 1
10 x 10 alignment: 1
total alignment: 9963
     */
}