package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.resource.Word2VecWrapper
import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import edu.jhu.jacana.align.tuple.reverb.FragAnno

object Word2VecTupleFeature extends TupleFeature {
	
    override def init() { 
        // fireup Word2VecWrapper the first time
        Word2VecWrapper.getSimilarity("a", "a")
    }

    override def featurePrefix() = "Word2Vec"

    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    combineTokens(anno1.tokens, anno2.tokens)
	}

	def combineTokens(tokens1:Array[String], tokens2:Array[String]):Double = {
	    var all = 0.0
	    for (p1 <- tokens1)
	        for (p2 <- tokens2)
	            all += Word2VecWrapper.getSimilarity(p1, p2)
	    // return all/Math.min(tokens1.size, tokens2.size)
	    return all/(tokens1.size + tokens2.size)
	}

}