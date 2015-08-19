/**
 *
 */
package edu.jhu.jacana.align.tuple.reverb

import scala.io.Source
import edu.jhu.jacana.align.AlignPair
import net.liftweb.json._
import scala.collection.mutable.ArrayBuffer

/**
 * Read in tuple alignment data in .json format in the folder alignment-data/tuples.
 * 
 * <i>noCrossingArgs</i>: if <i>cross</i> is set to true in the .json file, it means
 * tuple1.arg1 <-> tuple2.arg2, tuple1.arg2 <-> tuple2.arg1. Setting <i>noCrossingArgs</i>
 * to true will switch arg1/2 in tuple2.
 * 
 * In the tuple alignment task, we always set it to true to make things simpler (the real
 * reason is not to break the whole train/test/eval pipeline in Weka). In *real world*
 * test however, we always make two predictions (one assumes cross=true the other false)
 * and take the one with better prediction score.
 * 
 * @author Xuchen Yao
 *
 */
class AlignTupleReader (fname: String, noCrossingArgs: Boolean = true) extends Iterator[AlignedTuple] {
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    private var tuple:AlignedTuple = null
    private val f = Source.fromFile(fname)
    private val allInOneString = f.mkString
    f.close()
    
    private val l = parse(allInOneString).
    		extract[List[AlignedTuples]].
    		foldLeft(List[AlignedTuple]())((r,c) => r ++ c.tuples)
    if (noCrossingArgs) {
        l.filter(t => t.cross).foreach{t => 
            val tmp = t.tgt.arg2; t.tgt.arg2 = t.tgt.arg1; t.tgt.arg1 = tmp;
            t.cross = false; }
    }

    private var current = 0

    def hasNext() : Boolean = {
        
        if (current < l.length) {
            tuple = l(current)
            current += 1
            return true
        } else
        	return false
    }    
    
    def next(): AlignedTuple = {
        return tuple
    }
}

object AlignTupleReader {
	def main(args: Array[String]): Unit = {
		val reader = new AlignTupleReader("alignment-data/tuples/msr.edinburgh.train.tuple.json", false)
		for (alignedTuple <- reader if alignedTuple.cross) {
			println(alignedTuple)
		}
	}
}