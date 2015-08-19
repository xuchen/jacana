/**
 *
 */
package edu.jhu.jacana.validation.ranker

import edu.jhu.jacana.validation.reader._
import edu.jhu.jacana.util.FileManager
import scala.io.Source
import scala.collection.mutable.HashSet

/**
 * A naive baseline ranker for counting overlaps
 * @author Xuchen Yao
 *
 */
class OverlapRanker {
    
   /**
     * remove stopword from a list of strings and return to a list of set,
     * with each set containing unique strings from the input string
     */
    def getOptionContentList(options: List[String]): List[HashSet[String]] = {
        val retList = new Array[HashSet[String]](options.length)
        for ((option, i) <- options.zipWithIndex) {
            val contentSet = new HashSet[String]()
            option.toLowerCase().split("\\s+").filter(o => !OverlapRanker.stopwords.contains(o)).
            	foreach {o => contentSet += o}
            retList(i) = contentSet
        }
        return retList.toList
    }

    def rank(trainList: List[Question], devList: List[Question], top: Int) {
        for ((question,qi) <- devList.view.zipWithIndex) {
            val scores = new Array[String](question.options.length)
        	val optionList = getOptionContentList(question.options)
            for ((qos,i) <- question.answers.zipWithIndex) {
                // each QuestionOptionSearch
                var count = 0
                val num = Math.min(top, qos.snippets.length)
                optionList(i).foreach{o =>
                    val r = o.r // turn to regex
	                for (snippetItem <- qos.snippets.slice(0, num)) {
	                    count += r.findAllIn(snippetItem.snippet.toLowerCase()).length
	                }
                }
                scores(i) = count.toString
            }
        	question.scores = Some(scores.toList)
        }
    }

}

object OverlapRanker {
    var stopwords = new HashSet[String]()
    init()
    def init() {
    	val stopwordPath = FileManager.getResource("resources/stopword/en.stop")
    	val f = Source.fromFile(stopwordPath, "UTF-8")
    	f.getLines() foreach { line =>
    	    stopwords ++= line.split("\\s+")
    	}
    	// println(stopwords)
    }
    
	def main(args: Array[String]): Unit = {
	    val ranker = new OverlapRanker
	    ranker.rank(null, null, 0)
	}

}