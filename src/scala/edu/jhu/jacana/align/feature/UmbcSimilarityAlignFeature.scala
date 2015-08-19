package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.resource.UmbcSimilarity
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.IndexLabelAlphabet

object UmbcSimilarityAlignFeature extends AlignFeature {
	object Types {
        val UMBC_SIM = "umbc"
    }
    
    import Types._
    
    override def init() { 
        // fire up UmbcSimilarity the first time
        UmbcSimilarity.getFeatScores(Array("a"), Array("a"))
    }
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
 		if (j == -1) {
		} else {
            val srcTokens = pair.srcTokens.slice(i, i+srcSpan)
           	val tgtTokens = pair.tgtTokens.slice(j, j+tgtSpan)
           	
           	val (srcIsPhrase, srcPhrase) = ChunkingAlignFeature.getPhrase(pair.srcChunks, i, i+srcSpan)
			val (tgtIsPhrase, tgtPhrase) = ChunkingAlignFeature.getPhrase(pair.tgtChunks, j, j+tgtSpan)
			
			if (srcIsPhrase && tgtIsPhrase && srcPhrase != "O") {
	           	for ((name, value) <- UmbcSimilarity.getFeatScores(srcTokens, tgtTokens)) {
	           	    	//ins.addFeature(s"$UMBC_SIM.$srcPhrase-$tgtPhrase.$name", NONE_STATE, currState, value, srcSpan, featureAlphabet) 
	           	    	ins.addFeature(s"$UMBC_SIM.$name", NONE_STATE, currState, value, srcSpan, featureAlphabet) 
	           	    }
	           	}
			}
	
		}       
}