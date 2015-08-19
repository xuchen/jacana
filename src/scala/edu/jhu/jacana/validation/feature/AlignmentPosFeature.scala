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
object AlignmentPosFeature extends ValidationFeature with Loggable {
    features = Array[String]("NN", "NNP", "JJ", "VB", "RB")
    // var feat2idx = features.iterator.zipWithIndex.map{case (feat,idx) => feat -> idx}.toMap
    // var feat2idx = features.iterator.zipWithIndex.map(kv => kv._1 -> kv._2).toMap
    val feat2idx = (for (i <- features.indices) yield features(i) -> i).toMap // gives a sequence
    
    def pos2feature(pos: String): String = {
        for (feat <- features) if (pos.contains(feat)) return feat
        return "NonContentWord"
    }

 	def extract(query:CoreMap, snippets:List[Snippet], optionSet: HashSet[String]): Array[Double] = {
	    val values = new Array[Double](features.length)
	    
        val pos2count = new HashMap[String, Integer]()
        val labels = StanfordCore.getLabels(query)

        for (snippetItem <- snippets) {
            if (AlignmentRanker.containsOption(snippetItem.snippet, optionSet) && snippetItem.snippet_align.length() > 0) {
                // the snippet must at least contain some word from the option
                val indices = AlignmentRanker.getAlignedQueryIndices(snippetItem.snippet_align)
                for (index <- indices) {
                    val tag = labels(index).tag()
                    val name = pos2feature(tag)

                    if (feat2idx.contains(name)) {
	                    if (!pos2count.contains(name))
	                        pos2count += name -> 0
	                    pos2count += name -> (1+pos2count(name))
                    }
                }
            }
        }
	    // normalizing by total count is very bad...
	    // val normalizer = pos2count.foldLeft(0)(_+_._2) + 1
	    // println("pos2count sum: " + pos2count.foldLeft(0)(_+_._2))
	    val normalizer = labels.size
	    pos2count.foreach {case (pos, count) =>
	        if (feat2idx.contains(pos))
	            values(feat2idx.apply(pos)) = count*1.0/normalizer
	    }
	    // println(pos2count)
	    // println(values.toList)
	    
	    return values
	}

}
