package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.resource.PPDB
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.IndexLabelAlphabet

object PPDBAlignFeature extends AlignFeature {
	object Types {
        val PPDB_SIM = "ppdb."
    }
    
    import Types._
    
    override def init() { 
        // fireup PPDB the first time
        PPDB.getFeatScores(Array("a"), Array("a"))
    }
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
 		if (j == -1) {
		} else {
            val srcTokens = pair.srcTokens.slice(i, i+srcSpan)
           	val tgtTokens = pair.tgtTokens.slice(j, j+tgtSpan)
           	
           	val (srcIsPhrase, srcPhrase) = ChunkingAlignFeature.getPhrase(pair.srcChunks, i, i+srcSpan)
			val (tgtIsPhrase, tgtPhrase) = ChunkingAlignFeature.getPhrase(pair.tgtChunks, j, j+tgtSpan)
			
			// PPDB is too noisy, we have to add these phrase check, otherwise F1's are really low.
			if (srcIsPhrase && tgtIsPhrase) {
	           	for ((synt, name, value) <- PPDB.getFeatScores(srcTokens, tgtTokens)) {
	           	    if (synt != "[x]") {
	           	    	//println((synt,srcToken,tgtToken,name,value))
	           	    	//ins.addFeature(PPDB_SIM + synt + "." + name, NONE_STATE, currState, value, srcSpan, featureAlphabet) 
	           	    	ins.addFeature(PPDB_SIM + name, NONE_STATE, currState, value, srcSpan, featureAlphabet) 
	           	    }
	           	}
			}
	
		}       
    }
}