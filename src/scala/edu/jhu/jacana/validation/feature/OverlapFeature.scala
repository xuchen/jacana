/**
 *
 */
package edu.jhu.jacana.validation.feature

import edu.stanford.nlp.util.CoreMap
import scala.collection.mutable.HashSet
import scala.collection.immutable.List
import edu.jhu.jacana.validation.reader.Snippet
import edu.jhu.jacana.validation.ranker.AlignmentRanker
import edu.jhu.jacana.nlp.StanfordCore

/**
 * turns out that adding this feature has a very bad impact on the dev set.
 * @author Xuchen Yao
 *
 */
object OverlapFeature extends ValidationFeature {
	features = Array[String]("overlap")
	
	def extract(query:CoreMap, snippets:List[Snippet], optionSet: HashSet[String]): Array[Double] = {
	    val values = new Array[Double](1)
	    
	    var count = 0
        optionSet.foreach{o =>
            val r = o.r // turn to regex
            for (snippetItem <- snippets) {
                count += r.findAllIn(snippetItem.snippet.toLowerCase()).length
            }
	    }
	    val normalizer = StanfordCore.getSentenceLength(query)
	    values(0) = count*1.0/normalizer
	    return values
	}
}