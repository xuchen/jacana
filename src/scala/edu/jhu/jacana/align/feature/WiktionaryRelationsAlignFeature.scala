/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.resource.WiktionaryRelations
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet
/**
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(2008688667185376973L)
object WiktionaryRelationsAlignFeature extends AlignFeature {

    val FEAT_NAME = "WiktionaryRelation"
    override def init() { 
        WiktionaryRelations.getRelation("a", "a")
    }

 	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
 		if (j == -1) {
		} else {
			var averageLength = (if (srcSpan < tgtSpan) 0.5 * (srcSpan + tgtSpan) else srcSpan)*AlignerParams.phraseWeight
            val srcTokens = pair.srcTokens.slice(i, i+srcSpan).mkString(" ")
           	val tgtTokens = pair.tgtTokens.slice(j, j+tgtSpan).mkString(" ")
           	
           	val relation = WiktionaryRelations.getRelation(srcTokens, tgtTokens)
           	if (relation != null)
       	    	ins.addFeature(s"$FEAT_NAME.$relation", NONE_STATE, currState, averageLength, srcSpan, featureAlphabet) 
			
		}
	
	}
}