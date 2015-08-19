package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import edu.jhu.jacana.align.resource.WiktionaryRelations
import edu.jhu.jacana.align.tuple.reverb.FragAnno

object WiktionaryTupleFeature extends TupleFeature {
	
    override def init() { 
        // fireup Wiktionary the first time
        WiktionaryRelations.init()
    }

    override def featurePrefix() = "Wiktionary"

    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    enumerateCountInWiktionary(anno1.tokens, anno2.tokens)
	}

	def enumerateCountInWiktionary(tokens1:Array[String], tokens2:Array[String]):Double = {
	    var all = 0
	    for (p1 <- TupleFeature.enumeratePhrases(tokens1))
	        for (p2 <- TupleFeature.enumeratePhrases(tokens2))
	            all += WiktionaryRelations.isInWiktionary(p1, p2)
	    // return all*1.0/Math.min(tokens1.size, tokens2.size)
	    return all*1.0/(tokens1.size + tokens2.size)
	}

}