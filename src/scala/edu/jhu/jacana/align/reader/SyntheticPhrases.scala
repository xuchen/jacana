/**
 *
 */
package edu.jhu.jacana.align.reader

import edu.jhu.jacana.align.AlignTrainData
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.evaluation.AlignEvaluator
import scala.collection.mutable.ArrayBuffer
import edu.jhu.jacana.align.AlignTrainRecord
import java.io.PrintWriter
import java.io.File
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * Convert token-based alignments to phrase-based via chunking.
 * 
 * The original RTE02MSR/Edinburgh++ datasets contain only 4~5%
 * of phrase alignment (see comments in [[AlignTrainRecord]]).
 * This object class synthesizes phrase alignment by merging
 * chunks (as defined by the chunker) with continuous/monotonic
 * alignments. 
 * 
 * @author Xuchen Yao
 *
 */
object SyntheticPhrases {
    
    
    /**
     * Given an array of chunks, return each individual chunk's starting (inclusive)
     * and ending (exclusive) position in a list of tuples.
     * 
	 * Array(B-NP, I-NP, B-VP, B-PP, B-NP, B-NP, I-NP, I-NP, I-NP, O) returns:
     * [(0,2), (2,3), (3,4), (4,5), (5,9)]
     */
	def getPhraseBoundaries(chunks:Array[String]): List[Tuple2[Int, Int]] = {
		// Array(B-NP, I-NP, B-VP, B-PP, B-NP, B-NP, I-NP, I-NP, I-NP, O)
	    val buffer = new ArrayBuffer[Tuple2[Int,Int]]()
	    var i = 0
	    var start = 0
	    while (i < chunks.length) {
	        if (chunks(i).startsWith("B")) {
	            if (i-1 != -1 && !chunks(i-1).startsWith("O"))
	                buffer.append((start, i))
	            start = i
	        } else if (chunks(i).startsWith("O")) {
	            if (i-1 != -1 && !chunks(i-1).startsWith("O"))
	                buffer.append((start, i))
	        } 
	        i += 1
	    }
	    return buffer.toList
	}
	
	def diagonallyFilled(record:AlignTrainRecord, srcS:Int, srcE:Int,
	        tgtS:Int, tgtE:Int): Boolean = {
	    if (srcE - srcS != tgtE - tgtS)
	        // not a squre
	        return false
	    val matrix = record.getPair.alignMatrix
	    var filled = true
	    var diag = -1
	    for (i <- srcS until srcE) {
	        diag += 1
	        for (j <- tgtS until tgtE) {
	            // diagonals: 1, non-diagonals: 0
	            if (i-srcS == diag && j-tgtS == diag) {
	                if (matrix(i)(j) != 1)
	                    return false
	            } else {
	                if (matrix(i)(j) == 1)
	                    return false
	            }
	        }
	    }
	    return filled
	}
	
	def fillSquare(record:AlignTrainRecord, srcS:Int, srcE:Int,
	        tgtS:Int, tgtE:Int) {
	    val matrix = record.getPair.alignMatrix
	    for (i <- srcS until srcE) {
	        for (j <- tgtS until tgtE) {
                matrix(i)(j) = 1
	        }
	    }    
	}
	
    def merge(data: AlignTrainData): AlignTrainData = {
        data.getTrainList.foreach(record =>  {
            val srcChunkIndices = getPhraseBoundaries(record.getPair.srcChunks)
            val tgtChunkIndices = getPhraseBoundaries(record.getPair.tgtChunks)
            for ((srcS, srcE) <- srcChunkIndices) {
                for ((tgtS, tgtE) <- tgtChunkIndices) {
                	if (diagonallyFilled(record, srcS, srcE, tgtS, tgtE)) {
                	    if (srcE-srcS > 1) {
                	        println(record.getPair.id)
                	        println((srcS, srcE, tgtS, tgtE))
                	    }
                	    fillSquare(record, srcS, srcE, tgtS, tgtE)
                	}
                }
            }
        })
        return data
    }

    def main(args: Array[String]): Unit = {
        AlignerParams.shallowProcess = true
        // instead of setting a fixed upper length, just merge as long a chunk as possible
        // AlignerParams.phraseBased = true
        // AlignerParams.setMaxSourcePhraseLen(3)
        // AlignerParams.setMaxTargetPhraseLen(3)
        //val data = new AlignTrainData("alignment-data/edinburgh/gold.test.sure.json")
        val alphabet = new IndexLabelAlphabet()
        val data = new AlignTrainData("alignment-data/msr/converted/RTE2_test_M.align.json", labelAlphabet=alphabet)
        
        merge(data)
       	var writer: PrintWriter = new PrintWriter(new File(
       	        "/tmp/RTE2_test_M.align.json"))
        writer.print(data.toJSONviaMatrix) 
        writer.close()
    }
}