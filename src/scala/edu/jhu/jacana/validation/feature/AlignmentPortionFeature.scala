/**
 *
 */
package edu.jhu.jacana.validation.feature

import edu.jhu.jacana.align.util.Loggable
import edu.jhu.jacana.validation.reader.QuestionOptionSearch
import edu.stanford.nlp.util.CoreMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import edu.jhu.jacana.nlp.StanfordCore
import edu.jhu.jacana.validation.reader.Snippet
import edu.jhu.jacana.validation.ranker.AlignmentRanker
import weka.core.Attribute

/**
 * @author Xuchen Yao
 *
 */
object AlignmentPortionFeature extends ValidationFeature with Loggable {
    // NumericRange(0.0, 0.1, 0.2, ..., 0.999999...)
    val ranges = (0.0 to 1.0 by 0.2).dropRight(1)
    // Array[String] = Array(0.0, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
    val prefix = "range-"
    features = ranges.map(prefix + "%.1f".format(_)).toArray
    val feat2idx = (for (i <- features.indices) yield features(i) -> i).toMap

 	def extract(query:CoreMap, snippets:List[Snippet], optionSet: HashSet[String]): Array[Double] = {
	    val values = new Array[Double](features.length)
	    
        val portion2count = new HashMap[String, Integer]()
        val querySize = StanfordCore.getSentenceLength(query)

        for (snippetItem <- snippets) {
            if (AlignmentRanker.containsOption(snippetItem.snippet, optionSet) && snippetItem.snippet_align.length() > 0) {
                // the snippet must at least contain some word from the option
                val indices = AlignmentRanker.getAlignedQueryIndices(snippetItem.snippet_align)
                val portion = indices.size*1.0/querySize
                var i = ranges.size
                var break = false
                while (i > 0 && ! break) {
                    i -= 1
                    if (portion > ranges(i)) {
                        val fname = prefix + "%.1f".format(ranges(i))
                        portion2count += fname -> (portion2count.getOrElse(fname, 0).asInstanceOf[Integer] + 1)
                        break = true
                    }
                }
            }
        }
	    portion2count.foreach {case (portion, count) =>
	        if (feat2idx.contains(portion))
	            values(feat2idx.apply(portion)) = count.toDouble
	    }
	    // println(portion2count)
	    
	    return values
	}

}
