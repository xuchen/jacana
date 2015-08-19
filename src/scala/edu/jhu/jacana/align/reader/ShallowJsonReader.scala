/**
 *
 */
package edu.jhu.jacana.align.reader
import scala.io.Source
import edu.jhu.jacana.align.AlignPair
import net.liftweb.json._
import scala.collection.mutable.ArrayBuffer

/**
 * Read in alignment data in .json format supported by 
 * the Jacana Alignment Browser
 * 
 * https://github.com/lift/framework/tree/master/core/json
 * @author Xuchen Yao
 *
 */
class ShallowJsonReader(fname: String, transpose: Boolean = false, process: Boolean = true, tokenize: Boolean = true) extends Iterator[AlignPair] {
    
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    private var pair:AlignPair = null
    private val f = Source.fromFile(fname)
    private val allInOneString = f.mkString
    f.close()
    
    private val l = parse(allInOneString).extract[List[ShallowJson]]
    private var current = 0
    
    def getShallowJsons() = l
    
    def hasNext() : Boolean = {
        
        if (current < l.length) {
            val a = l(current)
            if (!transpose)
	            pair = new AlignPair(a.id, a.source, a.target, process, tokenize)
            else
	            pair = new AlignPair(a.id, a.target, a.source, process, tokenize)
            
            for (align <- a.sureAlign.trim().split("\\s+"); if align.length() > 0) {
                align.split("-") match {
                    case Array(src, tgt) => {
                        if (!transpose) {
                            pair.alignMatrix(src.toInt)(tgt.toInt) = 1
                        } else {
                            pair.alignMatrix(tgt.toInt)(src.toInt) = 1
                        }
                    }
                }
            }
            current += 1
            return true
        } else
        	return false
    }
    
    def next(): AlignPair = {
        return pair
    }
}

case class ShallowJson (possibleAlign:String, sureAlign:String, target:String, 
	        source:String, id:String, name:String)
        
object ShallowJsonReaderTest {
	def main(args: Array[String]): Unit = {
		val reader = new ShallowJsonReader("alignment-data/edinburgh/gold.train.sure.json", false, false)
		//val reader = new ShallowJsonReader("alignment-data/edinburgh/t.json", true)
		for (alignedPair <- reader) {
			println(alignedPair)
		}
	}
}