/**
 *
 */
package edu.jhu.jacana.align.edits

import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignPair

/**
 * An Edit is also a state in a CRF. It's a tuple of 3: (name, srcMove, tgtMove).
 * For instance, a Del-1 edit would have name=Deletion, srcMove=1, tgtMove=0,
 * meaning that 1 token from the source is deleted. Similarly, Ins-1 is (Insertion, 0, 1).
 * More complex edits, such as Del-NP, doesn't really specify the span of NP,
 * then srcMove=-1, TgtMove = 0. Later srcMove is dynamically determined per the length of NP.
 * @author Xuchen Yao
 *
 */
class Edit (var name:String, var srcMove: Int, var tgtMove: Int) {
    object Edit {
    	val DYNAMIC_MOVE = -1
    }
	
    // gives us 
	var fullName = this.getClass().getName()
	
	// this.getClass().getSimpleName() doesn't give us what we want due to this bug:
	// https://issues.scala-lang.org/browse/SI-2034
	var simpleName = fullName.substring(fullName.lastIndexOf("$")+1)
	
	override def toString(): String = {name}
	
	// handles basic DEL_X, INS_X, SUB_X case
	def canApply(pair: AlignPair, row:Int, column:Int): (Int, Int) = {
	    if (srcMove == Edit.DYNAMIC_MOVE || tgtMove == Edit.DYNAMIC_MOVE) 
	        throw new UnsupportedOperationException("You have to override this method in class "+fullName)
	    
	    if (!boundaryCheck(pair, row, column)) return null
        var sum = 0
	    (srcMove, tgtMove) match {
	        case (srcMove, 0) if srcMove > 0 => {
	            // Are srcMove tokens in the source marked as deleted?
	            for (irow <- row until row+srcMove) 
	                sum += pair.sumRowAt(irow, column)
	            if (sum == 0) (row+srcMove, column) else null
	        }
	        case (0, tgtMove) if tgtMove > 0 => {
	            for (icolumn <- column until column+tgtMove)
	                sum += pair.sumColumnAt(row, icolumn)
	            if (sum == 0) (row, column+tgtMove) else null
	        }
	        case (srcMove, tgtMove) => {
	            // suppose srcMove = 2, tgtMove = 2, then we need the 2x2 block
	            // starting at (row, column) with all 1's (so that they are aligned)
	            var total = srcMove * tgtMove
	            for (irow <- row until row+srcMove) 
		            for (icolumn <- column until column+tgtMove)
		            	total -= pair.alignMatrix(irow)(icolumn)
	            if (total == 0) (row+srcMove, column+tgtMove) else null
	            
	        }
	    }
	    
	}
	
	def boundaryCheck(pair: AlignPair, row:Int, column:Int): Boolean = {
	    if (row+srcMove <= pair.rows && column+tgtMove <= pair.columns)
	        true
        else
            false
	}
	
	
}

class DEL_1 (name:String = "DEL", srcMove: Int = 1, tgtMove: Int = 0) extends Edit (name, srcMove, tgtMove) {}
class INS_1 (name:String = "INS", srcMove: Int = 0, tgtMove: Int = 1) extends Edit (name, srcMove, tgtMove) {}
class SUB_1 (name:String = "SUB", srcMove: Int = 1, tgtMove: Int = 1) extends Edit (name, srcMove, tgtMove) {}