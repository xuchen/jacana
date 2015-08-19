/**
 *
 */
package edu.jhu.jacana.align

import edu.jhu.jacana.align.reader.MsrReader
import java.io.PrintWriter
import java.io.File
import edu.jhu.jacana.align.reader.ShallowJsonReader

/**
 * The Hierarchy is like this:
 * AlignTrainData: the whole training or test corpus
 * 	 AlignTrainRecord: each source/target pair (AlignPair) and it's token/segmentation info
 * 		AlignPair: each pair and an alignment matrix
 * 			AlignFeatureVector: every source token has an "instance" that contains extracted features
 * @author Xuchen Yao
 *
 */
class AlignTrainData (fname:String = "alignment-data/msr/converted/RTE2_tiny_M.align.txt", 
        transpose: Boolean = false, tokenize: Boolean = true, process: Boolean = true, labelAlphabet:IndexLabelAlphabet) extends TrainAlignData {
	private val list: List[AlignPair] = 
	    if (fname.contains("json")) new ShallowJsonReader(fname, transpose=transpose, process=process, tokenize=tokenize).toList
	    else new MsrReader(fname, transpose).toList
	private val trainList = for (x <- list) yield new AlignTrainRecord(x, labelAlphabet=labelAlphabet)
	private var current = 0

	def size():Int = { return trainList.size}

	def startScan() {
	    current = 0
	} 
	
	def getPairList(): List[AlignPair] = {return list}
	def getTrainList(): List[AlignTrainRecord] = {return trainList}

	def hasMoreRecords(): Boolean = {return current < list.size} 
	def nextRecord(): TrainAlignRecord = {
	    current += 1
	    return trainList(current-1)
	}
	def hasNext(): Boolean = {return hasMoreRecords} 
	def next(): AlignSequence = {return nextRecord}
	def getTrainData(): List[AlignTrainRecord] =  trainList
	
	def toMsrFormat(): String = {
	    val sb = new StringBuilder
	    for (x <- trainList)
	        sb.append(x.toMsrFormat)
	    return sb.toString
	    
	}
	
	def toJSON(): String = {
	    val sb = new StringBuilder
	    sb.append("[\n")
	    val jsons = trainList.map(x => x.toJSON)
	    sb.append(jsons.mkString("\t,\n"))
	    sb.append("]")
	    
	    return sb.toString
	}
	
	def toJSONviaMatrix(): String = {
	    val sb = new StringBuilder
	    sb.append("[\n")
	    val jsons = trainList.map(x => x.getPair.toJSON())
	    sb.append(jsons.mkString("\t,\n"))
	    sb.append("]")
	    
	    return sb.toString
	}

}