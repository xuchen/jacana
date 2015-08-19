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
 * whether two words are of certain relations according to WordNet.
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(-2678488059923538561L)
object WordnetAlignFeature extends AlignFeature {
	object Types {
        val WN_LEMMA_MATCH = "WordnetLemmaMatch"
        val WN_HYPERNYM = "WordnetHypernym"
        val WN_HYPONYM = "WordnetHyponym"
        val WN_SYNONYM = "WordnetSynonym"
        val WN_DERIVED = "WordnetDerived"
        val WN_ENTAILING = "WordnetEntailing"
        val WN_CAUSING = "WordnetCausing"
        val WN_MEMBERS_OF = "WordnetMembersOf"
        val WN_HAVE_MEMBER = "WordnetHaveMember"
        val WN_SUBSTANCES_OF = "WordnetSubstancesOf"
        val WN_HAVE_SUBSTANCE = "WordnetHaveSubstance"
        val WN_PARTS_OF = "WordnetPartsOf"
        val WN_HAVE_PART = "WordnetHavePart"
    }
    
    import Types._
    
    override def init() { 
        // fireup WordNet the first time
        WordNet.getLemmas(Array("cat"), Array("nn"))
    }
    
    // TODO: this function is too heavy loaded. Need memoization
    def doesIntersect(s1: HashSet[String], s2: HashSet[String]): Boolean = { 
        if (s1 == null || s1.size() == 0 || s2 == null || s2.size() == 0)
            return false
        val s = new HashSet[String](s1)
        s.retainAll(s2)
        return !s.isEmpty()
    }
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
	}
	
    override def extract(record: AlignSequence, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet):Array[AlignFeatureVector] =  {
        val pair = record.getPair
        
        val srcLemma = WordNet.getLemmas(pair.srcTokens, pair.srcPos).map(_.toLowerCase())
        val tgtLemma = WordNet.getLemmas(pair.tgtTokens, pair.tgtPos).map(_.toLowerCase())
        
        // should be slower than while(), but I can't help it
        val srcHypernyms = for (i <- 0 until pair.srcLen) yield WordNet.getHypernymsSet(pair.srcTokens(i), pair.srcPos(i))
        val tgtHypernyms = for (i <- 0 until pair.tgtLen) yield WordNet.getHypernymsSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcHyponyms = for (i <- 0 until pair.srcLen) yield WordNet.getHyponymsSet(pair.srcTokens(i), pair.srcPos(i))
        val tgtHyponyms = for (i <- 0 until pair.tgtLen) yield WordNet.getHyponymsSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcSynonyms = for (i <- 0 until pair.srcLen) yield WordNet.getSynonymsSet(pair.srcTokens(i), pair.srcPos(i))
        val tgtSynonyms = for (i <- 0 until pair.tgtLen) yield WordNet.getSynonymsSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        // val srcDerived = for (i <- 0 until pair.srcLen) yield WordNet.getDerivedSet(pair.srcTokens(i), pair.srcPos(i))
        // val tgtDerived = for (i <- 0 until pair.tgtLen) yield WordNet.getDerivedSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcEntailing = for (i <- 0 until pair.srcLen) yield WordNet.getEntailingSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtEntailing = for (i <- 0 until pair.tgtLen) yield WordNet.getEntailingSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcCausing = for (i <- 0 until pair.srcLen) yield WordNet.getCausingSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtCausing = for (i <- 0 until pair.tgtLen) yield WordNet.getCausingSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcMembersOf = for (i <- 0 until pair.srcLen) yield WordNet.getMembersOfSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtMembersOf = for (i <- 0 until pair.tgtLen) yield WordNet.getMembersOfSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcSubstancesOf = for (i <- 0 until pair.srcLen) yield WordNet.getSubstancesOfSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtSubstancesOf = for (i <- 0 until pair.tgtLen) yield WordNet.getSubstancesOfSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcPartsOf = for (i <- 0 until pair.srcLen) yield WordNet.getPartsOfSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtPartsOf = for (i <- 0 until pair.tgtLen) yield WordNet.getPartsOfSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcHaveMember = for (i <- 0 until pair.srcLen) yield WordNet.getHaveMemberSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtHaveMember = for (i <- 0 until pair.tgtLen) yield WordNet.getHaveMemberSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcHaveSubstance = for (i <- 0 until pair.srcLen) yield WordNet.getHaveSubstanceSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtHaveSubstance = for (i <- 0 until pair.tgtLen) yield WordNet.getHaveSubstanceSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        val srcHavePart = for (i <- 0 until pair.srcLen) yield WordNet.getHavePartSet(pair.srcTokens(i), pair.srcPos(i)) 
        val tgtHavePart = for (i <- 0 until pair.tgtLen) yield WordNet.getHavePartSet(pair.tgtTokens(i), pair.tgtPos(i))
        
        var i = 0
        while (i < pair.srcLen) {
            val ins = pair.featureVectors(i)
            val srcToken = pair.srcTokens(i)
            var j = 0
            while (j < pair.tgtLen) {
               val tgtToken = pair.tgtTokens(j)
               //ins.addFeature(JARO_WINKLER, -1, j+1, 1) 
               if (srcLemma(i) == tgtLemma(j))
            	   ins.addFeature(WN_LEMMA_MATCH, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcHypernyms(i), tgtHypernyms(j)))
            	   ins.addFeature(WN_HYPERNYM, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcHyponyms(i), tgtHyponyms(j)))
            	   ins.addFeature(WN_HYPONYM, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcSynonyms(i), tgtSynonyms(j)))
            	   ins.addFeature(WN_SYNONYM, -1, j+1, 1, 1, featureAlphabet) 
               // if (doesIntersect(srcDerived(i), tgtDerived(j)))
            	 //  ins.addFeature(WN_DERIVED, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcEntailing(i), tgtEntailing(j)))
            	   ins.addFeature(WN_ENTAILING, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcCausing(i), tgtCausing(j)))
            	   ins.addFeature(WN_CAUSING, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcMembersOf(i), tgtMembersOf(j)))
            	   ins.addFeature(WN_MEMBERS_OF, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcHaveMember(i), tgtHaveMember(j)))
            	   ins.addFeature(WN_HAVE_MEMBER, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcSubstancesOf(i), tgtSubstancesOf(j)))
            	   ins.addFeature(WN_SUBSTANCES_OF, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcHaveSubstance(i), tgtHaveSubstance(j)))
            	   ins.addFeature(WN_HAVE_SUBSTANCE, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcPartsOf(i), tgtPartsOf(j)))
            	   ins.addFeature(WN_PARTS_OF, -1, j+1, 1, 1, featureAlphabet) 
               if (doesIntersect(srcHavePart(i), tgtHavePart(j)))
            	   ins.addFeature(WN_HAVE_PART, -1, j+1, 1, 1, featureAlphabet) 
               j += 1
            }
            i += 1
        }
        return pair.featureVectors
    }

}