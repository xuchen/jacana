package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.FragAnno
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl._;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

object WordnetSimilarityTupleFeature extends TupleFeature {
    override def featurePrefix() = "WordnetSimilarity"
    val db = new NictWordNet()
    // to see a list of measures:
    // https://code.google.com/p/ws4j/source/browse/trunk/edu.cmu.lti.ws4j/src/main/java/edu/cmu/lti/ws4j/WS4J.java
    val relator = new Resnik(db)
    WS4JConfiguration.getInstance().setMFS(true);


    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    combineTokens(anno1.tokens, anno2.tokens)
	}
    
    def combineTokens(tokens1:Array[String], tokens2:Array[String]):Double = {
	    var all = 0.0
	    for (p1 <- tokens1)
	        for (p2 <- tokens2) {
	            val s = relator.calcRelatednessOfWords(p1, p2)
	            if (!(s > 1000 || s < 0 || s == Double.NaN || 
	                    s == Double.PositiveInfinity || s == Double.NegativeInfinity)) 
	                all += s
	        }
	    // return all/Math.min(tokens1.size, tokens2.size)
	    return all/(tokens1.size + tokens2.size)
	}
}