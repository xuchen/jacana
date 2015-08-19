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
object AlignmentDepFeature extends ValidationFeature with Loggable {
    features = Array[String]("pobj", "nsubj", "amod", "rcmod", "nn", "prep", "cop", "conj", "cc", "null", "dobj", "nsubjpass", "xcomp", "partmod")
    // var feat2idx = features.iterator.zipWithIndex.map{case (feat,idx) => feat -> idx}.toMap
    // var feat2idx = features.iterator.zipWithIndex.map(kv => kv._1 -> kv._2).toMap
    val feat2idx = (for (i <- features.indices) yield features(i) -> i).toMap // gives a sequence

 	def extract(query:CoreMap, snippets:List[Snippet], optionSet: HashSet[String]): Array[Double] = {
	    val values = new Array[Double](features.length)
	    
        val dep2count = new HashMap[String, Integer]()
        // StanfordCore.getLabels(query)
        val rels = StanfordCore.getBasicDepRelations(query)

        for (snippetItem <- snippets) {
            if (AlignmentRanker.containsOption(snippetItem.snippet, optionSet) && snippetItem.snippet_align.length() > 0) {
                // the snippet must at least contain some word from the option
                val indices = AlignmentRanker.getAlignedQueryIndices(snippetItem.snippet_align)
                for (index <- indices) {
                    if (feat2idx.contains(rels(index))) {
	                    if (!dep2count.contains(rels(index)))
	                        dep2count += rels(index) -> 0
	                    dep2count += rels(index) -> (1+dep2count(rels(index)))
                    }
                }
            }
        }
	    // normalizing by total count is very bad...
	    // val normalizer = dep2count.foldLeft(0)(_+_._2) + 1
	    // println("dep2count sum: " + dep2count.foldLeft(0)(_+_._2))
	    val normalizer = rels.size
	    dep2count.foreach {case (dep, count) =>
	        if (feat2idx.contains(dep))
	            values(feat2idx.apply(dep)) = count*1.0/normalizer
        
	    }
	    
	    return values
	}

}
