/**
 *
 */
package edu.jhu.jacana.align.reader
//import scala.util.control.Breaks._

/**
 * Deprecated, call the [[AlignTrainRecord.printAlignStat]] method for stats.
 * @author Xuchen Yao
 *
 */
@deprecated
object CorpusStats {

    def main(args: Array[String]): Unit = {
        
        object Break extends Exception {}
		val reader = new MsrReader("alignment-data/msr/converted/RTE2_dev_M.align.txt")
		var total_pair = 0; var only_token_aligned = 0;
		var cross_aligned_pair = 0;
		for (alignedPair <- reader ) {
		    total_pair += 1
		    if (alignedPair.onlyTokenAligned) only_token_aligned += 1
		    var last_one_idx = 0
		    var one_idx = -1
		    try{
			    for (i <- 0 until alignedPair.rows) {
		            one_idx = alignedPair.alignMatrix(i).toList.indexOf(1)
		            if (one_idx != -1) {
		                if (one_idx > last_one_idx)
		                    last_one_idx = one_idx
		                else {
		                    cross_aligned_pair += 1
		                    // for some reason the break keyword throws a very vague exception
		                    //break
		                    throw Break
		                }
		            }
			    }
		    }
		    catch {case Break => }
		}
		println("total: %d, only_token_aligned: %d (%.2f), cross_aligned: %d (%.2f)".format(
		        total_pair, only_token_aligned, only_token_aligned*1.0/total_pair, cross_aligned_pair, cross_aligned_pair*1.0/total_pair))
        
    }

}