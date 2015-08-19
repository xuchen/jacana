/**
 *
 */
package edu.jhu.jacana.align

import edu.jhu.jacana.nlp.OpenNLP
import edu.jhu.jacana.align.util.AlignerParams
import scala.util.parsing.json.JSONFormat
import scala.collection.mutable.HashSet
import gnu.trove.map.hash.TIntObjectHashMap

/**
 * @author Xuchen Yao
 *
 */
class AlignPair (val id: String, source: String, target: String, 
          process: Boolean = true, tokenize: Boolean = false, 
          sent1pos:Array[String] = null, sent1chunk:Array[String] = null,
          sent2pos:Array[String] = null, sent2chunk:Array[String] = null) {
    
    if (tokenize)
       	OpenNLP.createTokenizer()
        	
    val srcTokens = if (tokenize) OpenNLP.tokenize(source) else source.split("\\s+")
    val tgtTokens = if (tokenize) OpenNLP.tokenize(target) else target.split("\\s+")
    val src = srcTokens.mkString(" ")
    val tgt = tgtTokens.mkString(" ")
    
    val srcLen = srcTokens.length
    val tgtLen = tgtTokens.length
    
    val rows = srcLen
    val columns = tgtLen
    
    val alignMatrix = Array.ofDim[Int](rows, columns)
    
    // each instance is a feature vector for that token in src
    val featureVectors = new Array[AlignFeatureVector](srcLen)
    for (i <- 0 until featureVectors.length)
        featureVectors(i) = new AlignFeatureVector
    
    /*
     * if true: then there's only one-to-one alignment between tokens
     * else: at least one token is aligned to multiple tokens (not 
     * 		 necessarily a phrase, i.e., continuous tokens)
     */
    var onlyTokenAligned  = true
    
    var srcPos: Array[String] = null
    var tgtPos: Array[String] = null
    var srcChunks: Array[String] = null
    var tgtChunks: Array[String] = null
    if (sent1pos != null && sent1chunk != null && sent2pos != null && sent2chunk != null) {
        srcPos = sent1pos; srcChunks = sent1chunk;
        tgtPos = sent2pos; tgtChunks = sent2chunk;
      
    } else if (process && AlignerParams.shallowProcess) {
        OpenNLP.createPosTagger()
        OpenNLP.createChunker()
        
        srcPos = OpenNLP.tagPos(srcTokens)
        tgtPos = OpenNLP.tagPos(tgtTokens) 
        
        srcChunks = OpenNLP.tagChunks(srcTokens, srcPos)
        tgtChunks = OpenNLP.tagChunks(tgtTokens, tgtPos)
    }
    
    var score = 0.0
    
    def getScore = score
    def setScore(s:Double) {score = s} 
 
    def sumRow(row:Int): Int = {
        sumRowAt(row, 0)
    }
    
    /**
     * Summing over the row from (row, column)
     */
    def sumRowAt(row:Int, column:Int): Int = {
        var sum = 0; var i = column
        while (i < columns) {sum+=alignMatrix(row)(i); i+=1}
        sum
    }
    
     def sumColumn(column:Int): Int = {
         sumColumnAt(0, column)
    }
     
    /**
     * Summing over the column from (row, column)
     */
     def sumColumnAt(row:Int, column:Int): Int = {
        var sum = 0; var i = row
        while (i < rows) {sum+=alignMatrix(i)(column); i+=1}
        sum
     }
     
     /**
      * Summing over the rectangle region from (rowStart, columnStart) (inclusive)
      * to (rowEnd, columnEnd) (exclusive)
      */
     def sumRectAt(rowStart:Int, rowEnd:Int, columnStart:Int, columnEnd:Int) : Int = {
         return AlignPair.sumRectAt(alignMatrix, rowStart, rowEnd, columnStart, columnEnd)
     }
    
    override def toString = {
        src+"\n"+tgt+"\n"+alignMatrix.map(_.mkString(" ")).mkString("\n")
    }
    
   def toJSON(externalName:String = null): String = {
        var sb = new StringBuilder()
        sb.append("\t{\n")
        if ((id != null && id != "") || (externalName == null || externalName == ""))
            sb.append(AlignPair.keyValue2JSON("name", id))
        else
            sb.append(AlignPair.keyValue2JSON("name", externalName))
        sb.append(AlignPair.keyValue2JSON("id", id))
        sb.append(AlignPair.keyValue2JSON("source", src))
        sb.append(AlignPair.keyValue2JSON("target", tgt))
        sb.append(AlignPair.keyValue2JSON("score", score.toString))
        sb.append(AlignPair.keyValue2JSON("sureAlign", getDashedAlign()))
        sb.append(AlignPair.keyValue2JSON("possibleAlign", "", linebreak=false))
        sb.append("\n\t}\n")
        return sb.toString
    }   
    
    // return a dashed alignment string such as "1-0 3-3"
    def getDashedAlign(transpose: Boolean = false): String = {
        var sb = new StringBuilder()
        for (i <- 0 until rows) {
            for (j <- 0 until columns) {
                if (alignMatrix(i)(j) != 0) {
                	if (transpose)
                		sb.append(s"$j-$i ")
            		else
                		sb.append(s"$i-$j ")
                }
                
            }
        }
        
        return sb.toString.trim()
    }
    
    def toMsrFormat: String = {
        // already knowing what the source aligns to through labelsPerToken,
        // we output what the target aligns to in MSR format.
        // note that multiple source tokens can align to the same target 
        val tgt2src = new TIntObjectHashMap[HashSet[Integer]]()
        for (i <- 0 until rows) {
            for (j <- 0 until columns) {
                if (alignMatrix(i)(j) != 0) {
		            if (!tgt2src.containsKey(j)) {
		                tgt2src.put(j, new HashSet[Integer]())
		            }
                    tgt2src.get(j).add(i+1)
                }
                
            }
        }
        
        var sb = new StringBuilder()
		sb.append("# sentence pair " + id);
        sb.append("\n")
        sb.append(src)
        sb.append("\n")
		sb.append("NULL ({ / / }) ")
		tgtTokens.view.zipWithIndex foreach { case (token, tgt) =>
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
    
}

object AlignPair {
     // given key=name, value=1.txt, return:
    // \t"name": "1.txt",\n
    // if linebreak == false, then don't output the trailing ,\n
    def keyValue2JSON(key: String, value: String, linebreak: Boolean = true): String = {
        var s = String.format("\t\"%s\": \"%s\"", JSONFormat.quoteString(key), JSONFormat.quoteString(value))
        if (linebreak)
            s += ",\n"
        return s
    }
    
      /**
      * Summing over the rectangle region from (rowStart, columnStart) (inclusive)
      * to (rowEnd, columnEnd) (exclusive)
      */
     def sumRectAt(a: Array[Array[Int]], rowStart:Int, rowEnd:Int, columnStart:Int, columnEnd:Int) : Int = {
         var sum = 0; var i = rowStart
         while (i < rowEnd) {
             var j = columnStart
             while (j < columnEnd){sum+=a(i)(j); j+=1}
             i += 1
         }
         return sum 
     }
    
}