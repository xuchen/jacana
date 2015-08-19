/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.nlp.WordNet
import java.util.HashSet
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignSequence
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * whether two words are of certain relations according to WordNet when the POS is unknown.
 * @author Xuchen Yao
 *
 */
object WordnetNoPosAlignFeature extends AlignFeature {
    
	object Types {
        val WN_NOPOS_LEMMA_MATCH = "WordnetNoPosLemmaMatch"
        val WN_NOPOS_HYPERNYM = "WordnetNoPosHypernym"
        val WN_NOPOS_HYPONYM = "WordnetNoPosHyponym"
        val WN_NOPOS_SYNONYM = "WwordnetNoPosSynonym"
    }
    
    import Types._
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
	}
    override def extract(record: AlignSequence, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet):Array[AlignFeatureVector] =  {
        
        val pair = record.getPair
        //val srcLemma = WordNet.getLemmas(pair.srcTokens, pair.srcPos).map(_.toLowerCase())
        //val tgtLemma = WordNet.getLemmas(pair.tgtTokens, pair.tgtPos).map(_.toLowerCase())
        
        // should be slower than while(), but I can't help it
        val srcHypernyms = for (i <- 0 until pair.srcLen) yield WordNet.getHypernymsSet(pair.srcTokens(i))
        val tgtHypernyms = for (i <- 0 until pair.tgtLen) yield WordNet.getHypernymsSet(pair.tgtTokens(i))
        
        val srcHyponyms = for (i <- 0 until pair.srcLen) yield WordNet.getHyponymsSet(pair.srcTokens(i))
        val tgtHyponyms = for (i <- 0 until pair.tgtLen) yield WordNet.getHyponymsSet(pair.tgtTokens(i))
        
        val srcSynonyms = for (i <- 0 until pair.srcLen) yield WordNet.getSynonymsSet(pair.srcTokens(i))
        val tgtSynonyms = for (i <- 0 until pair.tgtLen) yield WordNet.getSynonymsSet(pair.tgtTokens(i))
        
        var i = 0
        while (i < pair.srcLen) {
            val ins = pair.featureVectors(i)
            val srcToken = pair.srcTokens(i)
            var j = 0
            while (j < pair.tgtLen) {
               val tgtToken = pair.tgtTokens(j)
               //ins.addFeature(JARO_WINKLER, -1, j+1, 1) 
               //if (srcLemma(i) == tgtLemma(j))
            //	   ins.addFeature(WN_LEMMA_MATCH, -1, j+1, 1) 
               if (WordnetAlignFeature.doesIntersect(srcHypernyms(i), tgtHypernyms(j)))
            	   ins.addFeature(WN_NOPOS_HYPERNYM, -1, j+1, 1, 1, featureAlphabet) 
               if (WordnetAlignFeature.doesIntersect(srcHyponyms(i), tgtHyponyms(j)))
            	   ins.addFeature(WN_NOPOS_HYPONYM, -1, j+1, 1, 1, featureAlphabet) 
               if (WordnetAlignFeature.doesIntersect(srcSynonyms(i), tgtSynonyms(j)))
            	   ins.addFeature(WN_NOPOS_SYNONYM, -1, j+1, 1, 1, featureAlphabet) 
               j += 1
            }
            i += 1
        }
        return pair.featureVectors
    }

}