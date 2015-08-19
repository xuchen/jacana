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
object AlignmentDepEdgeFeature extends ValidationFeature with Loggable {
    features = Array[String]("pobj", "nsubj", "amod", "rcmod", "nn", "prep", "cop", "conj", "cc", "null", "dobj", "nsubjpass", "xcomp", "partmod")
    private val prefix = "edge."
    for (i <- features.indices) features(i) = prefix + features(i)

    // var feat2idx = features.iterator.zipWithIndex.map{case (feat,idx) => feat -> idx}.toMap
    // var feat2idx = features.iterator.zipWithIndex.map(kv => kv._1 -> kv._2).toMap
    val feat2idx = (for (i <- features.indices) yield features(i) -> i).toMap // gives a sequence

 	def extract(query:CoreMap, snippets:List[Snippet], optionSet: HashSet[String]): Array[Double] = {
	    val values = new Array[Double](features.length)
	    
        val edge2count = new HashMap[String, Integer]()
        // StanfordCore.getLabels(query)
        val edges = StanfordCore.getBasicDepEdges(query)

        for (snippetItem <- snippets) {
            if (AlignmentRanker.containsOption(snippetItem.snippet, optionSet) && snippetItem.snippet_align.length() > 0) {
                // the snippet must at least contain some word from the option
                val indices = AlignmentRanker.getAlignedQueryIndices(snippetItem.snippet_align)
                for (i <- 0 until edges.size()) {
                    val edge = edges.get(i)
                    if (indices.contains(edge.getSource().index()-1) && indices.contains(edge.getTarget().index()-1)) {
                        val featName = prefix + edge.getRelation().toString()
	                    if (feat2idx.contains(featName)) {
		                    if (!edge2count.contains(featName))
		                        edge2count += featName -> 0
		                    edge2count += featName -> (1+edge2count(featName))
	                    }
                        
                    }
                }
            }
        }
	    val normalizer = edges.size()
	    edge2count.foreach {case (edge, count) =>
	        if (feat2idx.contains(edge))
	            values(feat2idx.apply(edge)) = count*1.0/normalizer
        
	    }
	    // println(edge2count)
	    
	    return values
	}
}
