package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.AlignedTuple
import scala.collection.mutable.HashSet
import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import com.rockymadden.stringmetric.similarity.JaroWinklerMetric
import com.rockymadden.stringmetric.similarity.LevenshteinMetric
import edu.jhu.jacana.align.tuple.reverb.FragAnno

object StringSimilarityTupleFeature extends TupleFeature {

    // override def featurePrefix() = "JaroWinkler"
    override def featurePrefix() = "Levenshtein"

    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    // JaroWinklerMetric.compare(anno1.string.toLowerCase(), anno2.string.toLowerCase()).get
	    combineTokens(anno1.tokens, anno2.tokens)
	}

	def combineTokens(tokens1:Array[String], tokens2:Array[String]):Double = {
	    var all = 0.0
	    for (p1 <- tokens1)
	        for (p2 <- tokens2)
	            all += JaroWinklerMetric.compare(p1, p2).get
	    // return all/Math.min(tokens1.size, tokens2.size)
	    return all/(tokens1.size + tokens2.size)
	}
}