package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.AlignedTuple
import edu.jhu.jacana.align.tuple.reverb.AlignedTuple
import scala.collection.mutable.HashMap
import edu.jhu.jacana.nlp.WordNet
import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import scala.collection.immutable.HashSet
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import edu.jhu.jacana.align.tuple.reverb.FragAnno

object WordnetTupleFeature extends TupleFeature {
    
	// object Type extends Enumeration {
	// 	type Type = Value
	// 	val WN_LEMMA_MATCH, WN_HYPERNYM, WN_HYPONYM, WN_SYNONYM, WN_DERIVED,
	// 	WN_ENTAILING, WN_CAUSING, WN_MEMBERS_OF, WN_HAVE_MEMBER,
    //     WN_SUBSTANCES_OF, WN_HAVE_SUBSTANCE, WN_PARTS_OF, WN_HAVE_PART = Value
	// }
	// import Type._
	
	// // warning: don't use Type.maxId here, it returns the size of Enumeration
	// // (so the name doesn't make sense) and it has existed as a bug for two years
	// features = new Array[String](Type.values.size)
	
    override def init() { 
        // fireup WordNet the first time
        WordNet.getLemmas(Array("cat"), Array("nn"))
    }

	
    override def featurePrefix() = "Wordnet"

    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    // getNormalizedWordnetRelationMatchCount(anno1.tokens, anno1.posTags, anno2.tokens, anno2.posTags)
	    getNormalizedWordnetRelationBooleanMatchCount(anno1.tokens, anno1.posTags, anno2.tokens, anno2.posTags)
	}

	def getNormalizedWordnetRelationBooleanMatchCount(s1:Array[String], pos1:Array[String],
											   s2:Array[String], pos2:Array[String]): Double = {
	    var all = 0
	    for (i <- 0 until s1.length) {
	    	for (j <- 0 until s2.length) {
	    		for (r <- WordNet.getAllRelations()) {
	    			if (WordNet.isOfRelation(s1(i), pos1(i), s2(j), pos2(j), r)){
	    			    all += 1
	    			    // println(s"${s1(i)} <-> ${s2(j)}: $r")
	    			}
	    		}
	    	}
	    }
	    all*1.0/(s1.size + s2.size)
	}
	
	def getNormalizedWordnetRelationMatchCount(s1:Array[String], pos1:Array[String],
											   s2:Array[String], pos2:Array[String]): Double = {

	    val setList1 = new ArrayBuffer[Set[String]]()
	    val setList2 = new ArrayBuffer[Set[String]]()

        val s1Hypernyms = (for (i <- 0 until s1.length) yield WordNet.getHypernymsSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2Hypernyms = (for (i <- 0 until s2.length) yield WordNet.getHypernymsSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1Hypernyms; setList2 += s2Hypernyms
        
        val s1Hyponyms = (for (i <- 0 until s1.length) yield WordNet.getHyponymsSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2Hyponyms = (for (i <- 0 until s2.length) yield WordNet.getHyponymsSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1Hyponyms; setList2 += s2Hyponyms
        
        val s1Synonyms = (for (i <- 0 until s1.length) yield WordNet.getSynonymsSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2Synonyms = (for (i <- 0 until s2.length) yield WordNet.getSynonymsSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1Synonyms; setList2 += s2Synonyms
        
        // val s1Derived = (for (i <- 0 until s1.length) yield WordNet.getDerivedSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        // val s2Derived = (for (i <- 0 until s2.length) yield WordNet.getDerivedSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        // setList1 += s1Derived; setList2 += s2Derived
        
        val s1Entailing = (for (i <- 0 until s1.length) yield WordNet.getEntailingSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2Entailing = (for (i <- 0 until s2.length) yield WordNet.getEntailingSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1Entailing; setList2 += s2Entailing
        
        val s1Causing = (for (i <- 0 until s1.length) yield WordNet.getCausingSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2Causing = (for (i <- 0 until s2.length) yield WordNet.getCausingSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1Causing; setList2 += s2Causing
        
        val s1MembersOf = (for (i <- 0 until s1.length) yield WordNet.getMembersOfSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2MembersOf = (for (i <- 0 until s2.length) yield WordNet.getMembersOfSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1MembersOf; setList2 += s2MembersOf
        
        val s1SubstancesOf = (for (i <- 0 until s1.length) yield WordNet.getSubstancesOfSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2SubstancesOf = (for (i <- 0 until s2.length) yield WordNet.getSubstancesOfSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1SubstancesOf; setList2 += s2SubstancesOf
        
        val s1PartsOf = (for (i <- 0 until s1.length) yield WordNet.getPartsOfSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2PartsOf = (for (i <- 0 until s2.length) yield WordNet.getPartsOfSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1PartsOf; setList2 += s2PartsOf
        
        val s1HaveMember = (for (i <- 0 until s1.length) yield WordNet.getHaveMemberSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2HaveMember = (for (i <- 0 until s2.length) yield WordNet.getHaveMemberSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1HaveMember; setList2 += s2HaveMember
        
        val s1HaveSubstance = (for (i <- 0 until s1.length) yield WordNet.getHaveSubstanceSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2HaveSubstance = (for (i <- 0 until s2.length) yield WordNet.getHaveSubstanceSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1HaveSubstance; setList2 += s2HaveSubstance
        
        val s1HavePart = (for (i <- 0 until s1.length) yield WordNet.getHavePartSet(s1(i), pos1(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        val s2HavePart = (for (i <- 0 until s2.length) yield WordNet.getHavePartSet(s2(i), pos2(i)).toSet).foldLeft(Set[String]())((r,c) => r ++ c)
        setList1 += s1HavePart; setList2 += s2HavePart
        var all = 0
        
        for (i <- 0 until setList1.size) {
        	all += intersectSize(setList1(i), setList2(i))
        }
        
        
	    // all*1.0/Math.min(s1.size, s2.size)
	    all*1.0/(s1.size + s2.size)
	}
	
    def intersectSize(s1: Set[String], s2: Set[String]): Int = { 
        if (s1 == null || s1.size == 0 || s2 == null || s2.size == 0)
            return 0
        return (s1 & s2).size
    }
 
	
}