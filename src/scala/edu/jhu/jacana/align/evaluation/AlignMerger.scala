/**
 *
 */
package edu.jhu.jacana.align.evaluation

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignTrainData
import java.io.PrintWriter
import java.io.File
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * implements the following MT merge heuristics: intersection,
 * union, grow-diag-final.
 * @author Xuchen Yao
 *
 */
object AlignMerger {
    
    val neighbors = Array(Array(-1,0),Array(0,-1),Array(1,0),Array(0,1),Array(-1,-1),Array(-1,1),Array(1,-1),Array(1,1))
    
    def intersection(p1: AlignPair, p2: AlignPair): AlignPair = {
        val p = new AlignPair(p1.id, p1.src, p1.tgt, process=false)
        
        var i = 0
        while (i < Math.min(p1.rows, p2.columns)) {
            var j = 0
            while (j < Math.min(p1.columns, p2.rows)) {
                if (p1.alignMatrix(i)(j) == 1 && p2.alignMatrix(j)(i) == 1) {
                    p.alignMatrix(i)(j) = 1
                }
                j += 1
            }
            i += 1
        }
        return p
    }

    def union(p1: AlignPair, p2: AlignPair): AlignPair = {
        val p = new AlignPair(p1.id, p1.src, p1.tgt, process=false)
        
        var i = 0
        while (i < Math.max(p1.rows, p2.columns)) {
            var j = 0
            while (j < Math.max(p1.columns, p2.rows)) {
                if ((i < p1.rows && j < p1.columns && p1.alignMatrix(i)(j) == 1) || (j < p2.rows && i < p2.columns && p2.alignMatrix(j)(i) == 1)) {
                    p.alignMatrix(i)(j) = 1
                }
                j += 1
            }
            i += 1
        }
        return p
    }
    
    def grow_diag_final(p1: AlignPair, p2: AlignPair): AlignPair = {
        var overlap = intersection(p1, p2)
        
        grow_diag(overlap, union(p1, p2))
        finalize(overlap, p1, null)
        finalize(overlap, null, p2)
        return overlap
    }
    
    def grow_diag(overlap: AlignPair, union: AlignPair): AlignPair = {
        /*
 iterate until no new points added
    for english word e = 0 ... en
      for foreign word f = 0 ... fn
        if ( e aligned with f )
          for each neighboring point ( e-new, f-new ):
            if ( ( e-new not aligned or f-new not aligned ) and
                 ( e-new, f-new ) in union( e2f, f2e ) ) 
              add alignment point ( e-new, f-new )
         */
        
        var new_i = 0; var new_j = 0;
        var rows = overlap.rows
        var columns = overlap.columns
        var i = 0
        while (i < rows) {
            var j = 0
            while (j < columns) {
                if (overlap.alignMatrix(i)(j) == 1) {
                    for (Array(ii, jj) <- neighbors) {
                        new_i = i+ii; new_j = j+jj;
                        if (new_i >= 0 && new_i < rows && new_j >= 0 && new_j < columns) {
                            if ((overlap.sumRow(new_i) == 0 || overlap.sumColumn(new_j) == 0) 
                                    && union.alignMatrix(new_i)(new_j) == 1)
                                overlap.alignMatrix(new_i)(new_j) = 1
                        }
                        
                    }
                    
                }
                j += 1
            }
            i += 1
        }
        
        return overlap
    }
    
    def finalize (overlap: AlignPair, e2f: AlignPair, f2e: AlignPair): AlignPair = {
        /*
 FINAL(a):
  for english word e-new = 0 ... en
    for foreign word f-new = 0 ... fn
      if ( ( e-new not aligned or f-new not aligned ) and
           ( e-new, f-new ) in alignment a )
        add alignment point ( e-new, f-new )
         */
        assert (!(e2f == null && f2e == null), {System.err.println("e2f and f2e can't be null the same time!")})
        assert (!(e2f != null && f2e != null), {System.err.println("e2f and f2e can't be not null the same time!")})
        
        var rows = overlap.rows
        var columns = overlap.columns
        if (e2f != null) {
            var i = 0
	        while (i < rows) {
	            var j = 0
	            while (j < columns) {
	                if (overlap.alignMatrix(i)(j) == 1) {
                        if ((overlap.sumRow(i) == 0 || overlap.sumColumn(j) == 0) 
                                    && e2f.alignMatrix(i)(j) == 1)
                        	overlap.alignMatrix(i)(j) = 1
	                }
	                j += 1
	            }
	            i += 1
	        }       
        } else if (f2e != null) {
            var i = 0
	        while (i < rows) {
	            var j = 0
	            while (j < columns) {
	                if (overlap.alignMatrix(i)(j) == 1) {
                        if (f2e.alignMatrix(j)(i) == 1)
                        	overlap.alignMatrix(i)(j) = 1
	                }
	                j += 1
	            }
	            i += 1
	        }       

        }
        
        return overlap
    }

    def main(args: Array[String]): Unit = {
        
    val alphabet = new IndexLabelAlphabet()
		val s2tData = new AlignTrainData("/tmp/RTE2_test_M.align.txt.s2t", labelAlphabet=alphabet)
    alphabet.freeze
		val t2sData = new AlignTrainData("/tmp/RTE2_test_M.align.txt.t2s", labelAlphabet=alphabet)
        val writer = new PrintWriter(new File("/tmp/RTE2_test_M.align.txt.grow_diag_final"))
		
		var counter = 0
		s2tData.getTrainData().zip(t2sData.getTrainData()) foreach {
		    case (s2t, t2s) =>
		    val intersect  = AlignMerger.grow_diag_final(s2t.getPair, t2s.getPair)
		    writer.print(intersect.toMsrFormat)
		    counter += 1
		    if (counter % 10 == 0)
		        println(counter)
		}
		writer.close()
    }

}