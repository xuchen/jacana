/**
 *
 */
package edu.jhu.jacana.align.reader

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.util.AlignerParams
import scala.io.Source
import scala.collection.mutable.ArrayBuffer

/**
 * @author Xuchen Yao
 *
 */
class DashedAlignReader (fname: String, transpose: Boolean = false) extends Iterator[AlignPair]{
	
    private var pair:AlignPair = null
    private val f = Source.fromFile(fname)
    private val lineIterator = f.getLines()
     
    def hasNext() : Boolean = {
        
		var id = ""; var src = ""; var tgt = ""
		var counter = 0
        while (lineIterator.hasNext) {
            counter += 1
            id = counter.toString
            var line = lineIterator.next()
            // 1-0 4-3 
            val sources = new ArrayBuffer[Int]()
            val targets = new ArrayBuffer[Int]()
            for (align <- line.trim().split("\\s+")) {
                align.split("-") match {
                    case Array(src, tgt) => {
                        if (!transpose) {
	                        sources.append(src.toInt)
	                        targets.append(tgt.toInt)
                        } else {
	                        sources.append(tgt.toInt)
	                        targets.append(src.toInt)
                        }
                    }
                }
            }
            val srcLen = sources.max + 1
            val tgtLen = targets.max + 1
            val source = (Array.fill(srcLen){"s"}).mkString(" ")
            val target = (Array.fill(tgtLen){"t"}).mkString(" ")
			pair = new AlignPair(id, source, target, process=false)
            sources.zip(targets) foreach {
                case (src, tgt) => pair.alignMatrix(src)(tgt) = 1
            }
            return true
        }
		f.close()
		pair = null
		
        return false
    }
    
    def next(): AlignPair = {
        return pair
    }
}


object DashedAlignReader {
	def main(args: Array[String]): Unit = {
		val reader = new DashedAlignReader("/tmp/test.align", true)
		for (alignedPair <- reader) {
			println(alignedPair)
		}
	}
}