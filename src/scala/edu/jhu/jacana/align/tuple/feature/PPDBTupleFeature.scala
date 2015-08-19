package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import edu.jhu.jacana.align.resource.PPDBsimple
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.tuple.reverb.FragAnno

object PPDBTupleFeature extends TupleFeature {
	
    override def init() { 
        // fireup PPDB the first time
		AlignerParams.phraseBased = true
        PPDBsimple.init()
    }

    override def featurePrefix() = "PPDB"

    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    enumerateCountInPPDB(anno1.tokens, anno2.tokens)
	}

	def enumerateCountInPPDB(tokens1:Array[String], tokens2:Array[String]):Double = {
	    var all = 0
	    for (p1 <- TupleFeature.enumeratePhrases(tokens1))
	        for (p2 <- TupleFeature.enumeratePhrases(tokens2))
	            all += PPDBsimple.isInPPDB(p1, p2)
	    // return all*1.0/Math.min(tokens1.size, tokens2.size)
	    return all*1.0/(tokens1.size + tokens2.size)
	}
	
}