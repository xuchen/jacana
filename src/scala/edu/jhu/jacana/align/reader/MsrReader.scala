/**
 *
 */
package edu.jhu.jacana.align.reader
import scala.io.Source
import java.util.regex.Pattern
import edu.jhu.jacana.align.AlignPair

/**
 * An static class object that reads alignment files in
 * MSR format (RTE*.align.txt)
 * 
 * if set <code>transpose</code> to true, then swap source and target
 * @author Xuchen Yao
 *
 */
class MsrReader (fname: String, transpose: Boolean = false) extends Iterator[AlignPair] {
    
    private var pair:AlignPair = null
    private val f = Source.fromFile(fname)
    private val lineIterator = f.getLines()
    var wordAlignPattern = """\S+\s*\(\{.*?\}\)""".r
    var alignPattern = """\b(\d+)\b""".r
    
    def hasNext() : Boolean = {
		var counter = 0
		var id = ""; var src = ""; var tgt = ""
        while (lineIterator.hasNext) {
            counter += 1
            var line = lineIterator.next()
			if (line.startsWith("# ")) {
				id = line.split("\\s+").last
			} else if (line.trim() == "") {
			    src = ""; tgt = "";
			} else if (!line.startsWith("NULL ")) {
			    if (transpose)
			        tgt = line
		        else
		        	src = line
			} else {
			    // use substring(4) to remove "NULL"
				val ss = line.substring(4).replaceAll(" \\(\\{.*?\\}\\)", "").trim()
				if (transpose)
				    src = ss
			    else
			        tgt = ss
				// NULL ({ / / }) Nelson ({ 1 / / }) Beaver ({ 2 / / }) is ({ 3 / / }) 
				// staff ({ 5 p4 / / }) at ({ p6 / / }) Carolina ({ 8 9 11 p7 / / }) 
				// Analytical ({ 8 9 12 p7 / / }) Laboratories ({ 8 9 13 p7 / / }) . ({ 24 / / })
				pair = new AlignPair(id, src, tgt)
				for ((s, i) <- wordAlignPattern.findAllIn(line).zipWithIndex if i != 0) {
				    // Nelson ({ 1 / / })
					var Array(word, align) = s.split("\\s+", 2)
				    for ((m, mi) <- alignPattern.findAllIn(align).matchData.zipWithIndex) {
				        if (transpose) 
				        	pair.alignMatrix(i-1)(m.group(0).toInt-1) = 1
			        	else
				        	pair.alignMatrix(m.group(0).toInt-1)(i-1) = 1
				        // multiple match, then this token is mapped to multiple tokens
				        if (mi > 0) pair.onlyTokenAligned  = false
				    }
				}
				return true
			}
        }
		f.close
        return false
    }
    
    def next(): AlignPair = {
        return pair
    }
}

object MsrReaderTest {
	def main(args: Array[String]): Unit = {
		val reader = new MsrReader("alignment-data/msr/converted/RTE2_tiny_M.align.txt", true)
		for (alignedPair <- reader) {
			println(alignedPair)
		}
	}
}