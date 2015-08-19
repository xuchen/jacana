/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.resource.NamePhylogeny
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * @author Xuchen Yao
 *
 */
object NamePhylogenyAlignFeature extends AlignFeature {
	object Types {
        val NAME_SIM = "namePhylogeny"
    }
    
    import Types._
    
    override def init() { 
        // fireup NamePhylogeny the first time
        NamePhylogeny.getSimilarity("a", "a")
    }
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
 		if (j == -1) {
		} else {
            val srcString = pair.srcTokens.slice(i, i+srcSpan).mkString(" ")
           	val tgtString = pair.tgtTokens.slice(j, j+tgtSpan).mkString(" ")
           	
           	//val (srcIsPhrase, srcPhrase) = ChunkingAlignFeature.getPhrase(pair.srcChunks, i, i+srcSpan)
			//val (tgtIsPhrase, tgtPhrase) = ChunkingAlignFeature.getPhrase(pair.tgtChunks, j, j+tgtSpan)
			
   	    	ins.addFeature(NAME_SIM, NONE_STATE, currState, NamePhylogeny.getSimilarity(srcString, tgtString), srcSpan, featureAlphabet)
		}
	
	}       
}