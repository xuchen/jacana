/**
 * 
 */
package edu.jhu.jacana.align.pipe.iterator
/*
import java.util.Iterator
import cc.mallet.types.Instance
import edu.jhu.jacana.align.reader.MsrReader
import scala.collection.mutable.ArrayBuffer
* 
*/

/**
 * @author Xuchen Yao
 *
 */
@deprecated
class TokenEditIterator {}/*(reader: MsrReader) extends Iterator[Instance] {
    
    var ins:Instance = null
    var insBuffer = new ArrayBuffer[Instance]

    def hasNext() : Boolean = {
        if (insBuffer.size == 0) {
            var insString:String = ""
            var target:String = ""
	        if (reader.hasNext) {
	           val alignedPair = reader.next() 
	           for ((srcToken,i) <- alignedPair.srcTokens.view.zipWithIndex) {
	                 var sum = alignedPair.sumRow(i)
	                 if (sum == 0) {
	                    insString = "diff"
	                    target = "DEL"
	                 } else {
	                     var j = alignedPair.alignMatrix(i).toList.indexOf(1)
	                     if (srcToken.equalsIgnoreCase(alignedPair.tgtTokens(j))) {
	                         insString = "same"
		                     target = "REN"
	                     } else {
	                         insString = "diff"
		                     target = "REN"
	                     }
	                 }
	                 ins = new Instance(insString, target, null, null)
	                 insBuffer.append(ins)
	           }
	        }
        }
        return insBuffer.size > 0
    }
    
    def next(): Instance = {
        insBuffer.remove(0)
    }
    
    def remove() {}
}
*/