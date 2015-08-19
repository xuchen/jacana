/**
 *
 */
package edu.jhu.jacana.align.edits

import edu.jhu.jacana.align.AlignPair
import scala.collection.mutable.ArrayBuffer
import edu.jhu.jacana.align.reader.MsrReader

/**
 * Not working properly yet since it doesn't deal with re-ordering then
 * the code runs into infinite looping.
 * @author Xuchen Yao
 *
 */
object FlatTokenEditSequence {
    
   	val allowedEdits = List[Edit](new DEL_1(), new INS_1(), new SUB_1())
    
    def getEditSequence(pair: AlignPair): ArrayBuffer[Edit] = {
        var edits = new ArrayBuffer[Edit]
        var currentRow = 0
        var currentCol = 0
        while (!(currentRow == pair.srcLen && currentCol == pair.tgtLen)) {
            for (edit <- allowedEdits) {
                edit.canApply(pair, currentRow, currentCol) match {
                    case (m,n) => {
                        currentRow = m; currentCol = n;
                        edits.append(new Edit(edit.name, edit.srcMove, edit.tgtMove))
                    }
                    case null => {}
                    // TODO: deal with multiple matching case
                }
                
            }
        }
        return edits
    }
   	
   	def main(args: Array[String]) {
		val reader = new MsrReader("alignment-data/msr/converted/RTE2_tiny_M.align.txt")
		for (alignedPair <- reader if alignedPair.onlyTokenAligned) {
			println(alignedPair)
			val seq = getEditSequence(alignedPair)
			println(seq)
		}
   	}
}