/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import com.rockymadden.stringmetric.similarity._
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet.NULL_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet
import edu.jhu.jacana.align.Alphabet
import scala.collection.immutable.HashSet
import scala.collection.mutable.ArrayBuffer

/**
 * A bunch of string similarity measures from [[https://github.com/rockymadden/stringmetric/  stringmetric]]
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(8313863561827359006L)
object StringSimilarityAlignFeature extends AlignFeature {
	object Types {
        val JARO_WINKLER = "JaroWinkler"
        val DICE_SORENSEN = "DiceSorensen"
        val HAMMING = "Hamming"
        val JACCARD = "Jaccard"
        val LEVENSHTEIN = "Levenshtein"
        val NGRAM1 = "Ngram1"
        val NGRAM2 = "Ngram2"
        val NGRAM3 = "Ngram3"
        val NGRAM4 = "Ngram4"
        //val PREFIX_MATCH2 = "prefix_match2"
        //val PREFIX_MATCH3 = "prefix_match3"
        //val PREFIX_MATCH4 = "prefix_match4"
        val NUM_COMMON_PREFIX = "numCommonPrefix"
        val NUM_COMMON_SUFFIX = "numCommonSuffix"
        val A_AN_ONE = "a_an_one"
        val BE_VERB_MATCH = "beVerbMatch"
        val DO_VERB_MATCH = "doVerbMatch"
        val HAVE_VERB_MATCH = "haveVerbMatch"
        val IDENTICAL_MATCH = "identicalMatch"
        val IDENTICAL_MATCH_IGNORE_CASE = "identicalMatchIgnoreCase"
    }
    import Types._
	
	val a_an_one = new HashSet() ++ List("a", "an", "one")
	val beWords = new HashSet() ++ List("be", "is", "was", "were", "are")
	val doWords = new HashSet() ++ List("do", "does", "did")
	val haveWords = new HashSet() ++ List("have", "has", "had")
	
	def specialMatch(token1:String, token2:String): (String, Double) = {
	    val t1 = token1.toLowerCase()
	    val t2 = token2.toLowerCase()
	    if (a_an_one.contains(t1) && a_an_one.contains(t2))
	        return (A_AN_ONE, 1.0)
	    if (beWords.contains(t1) && beWords.contains(t2))
	        return (BE_VERB_MATCH, 1.0)
	    if (doWords.contains(t1) && doWords.contains(t2))
	        return (DO_VERB_MATCH, 1.0)
	    if (haveWords.contains(t1) && haveWords.contains(t2))
	        return (HAVE_VERB_MATCH, 1.0)
	    return ("", 0.0)
	}
	
	def commonPrefix(token1:String, token2:String): Double = {
	    val t1 = token1.toLowerCase()
	    val t2 = token2.toLowerCase()
	    val min = Math.min(token1.length(), token2.length())
	    var i = 0
	    while (i < min) {
	        if (t1.charAt(i) != t2.charAt(i))
	            return i
	        i += 1
	    }
	    return min*1.0/Math.max(token1.length(), token2.length())
	}
		
	def commonSuffix(token1:String, token2:String): Double = {
	    val l1 = token1.length()
	    val l2 = token2.length()
	    val t1 = token1.toLowerCase()
	    val t2 = token2.toLowerCase()
	    val min = Math.min(l1, l2)
	    var i = 0
	    while (i < min) {
	        if (t1.charAt(l1-1-i) != t2.charAt(l2-1-i))
	            return i
	        i += 1
	    }
	    return min*1.0/Math.max(l1, l2)
	}
	def prefixMatch(n:Int, token1:String, token2:String) {
	    
	}
	
	def getStringSimilarities(srcToken: String, tgtToken: String): List[Tuple2[String, Double]] = {
	    val buffer = new ArrayBuffer[(String,Double)]()
	    buffer.append((JARO_WINKLER, JaroWinklerMetric.compare(srcToken, tgtToken).get))
	    buffer.append((DICE_SORENSEN, DiceSorensenMetric.compare(srcToken, tgtToken)(1).get))
	    buffer.append((HAMMING, HammingMetric.compare(srcToken, tgtToken).getOrElse(0).toDouble))
	    buffer.append((JACCARD, JaccardMetric.compare(srcToken, tgtToken)(1).get))
	    buffer.append((LEVENSHTEIN, LevenshteinMetric.compare(srcToken, tgtToken).get))
	    // buffer.append((NGRAM1, NGramMetric.compare(srcToken, tgtToken)(1).getOrElse(0)))
	    // buffer.append((NGRAM2, NGramMetric.compare(srcToken, tgtToken)(2).getOrElse(0)))
	    buffer.append((NGRAM3, NGramMetric.compare(srcToken, tgtToken)(3).getOrElse(0)))
	    buffer.append((NGRAM4, NGramMetric.compare(srcToken, tgtToken)(4).getOrElse(0)))
	    buffer.append((NUM_COMMON_PREFIX, commonPrefix(srcToken, tgtToken)))
	    buffer.append((NUM_COMMON_SUFFIX, commonSuffix(srcToken, tgtToken)))
		val (special, value) = specialMatch(srcToken, tgtToken)
		if (special != "")
			buffer.append((special, value))
		if (srcToken.toLowerCase() == tgtToken.toLowerCase()) {
			buffer.append((IDENTICAL_MATCH_IGNORE_CASE, 1.0))
			if (srcToken == tgtToken) {
				buffer.append((IDENTICAL_MATCH, 1.0))
			}
		}    
	    
	    return buffer.toList
	}
    
 	override def addTokenBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, j:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet) {
		if (j == -1) {
		} else {
            val srcToken = pair.srcTokens(i)
			val tgtToken = pair.tgtTokens(j)
			for ((featureName, value) <- getStringSimilarities(srcToken, tgtToken))
				ins.addFeature(featureName, NONE_STATE, currState, value, 1, featureAlphabet) 
		}
	}

	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
        //var averageLength = 0.5 * (srcSpan + tgtSpan)
	    var averageLength = (if (srcSpan < tgtSpan) 0.5 * (srcSpan + tgtSpan) else srcSpan)*AlignerParams.phraseWeight
	    //var averageLength = if (srcSpan < tgtSpan) srcSpan else 0.5 * (srcSpan + tgtSpan)
		if (j == -1) {
		} else {
            val srcToken = pair.srcTokens.slice(i, i+srcSpan).mkString("")
           	val tgtToken = pair.tgtTokens.slice(j, j+tgtSpan).mkString("")
           	
        	//if (srcToken == "veryfew" && tgtToken == "poorlyrepresented")
        	//if (srcToken == "results" && tgtToken == "isaresult")
        	if (false && (srcToken == "saidinaninterviewwith" && tgtToken == "told" ||
        	      srcToken == "results" && tgtToken == "isaresult" ||
        	      srcToken == "asyousay" && tgtToken == "right" ||
        	      srcToken == "UnitedAirlines" && tgtToken == "UnitedAirways" ||
        	      srcToken == "appeared" && tgtToken == "informationindicated"))
        	    // for debug, ASSUME we have a good lexical resource to align
        	    // "very few" and "poorly represented" (score = 5.0)
        	    // then check whether they are really aligned together 
        		ins.addFeature(JARO_WINKLER, NONE_STATE, currState, 500.0, srcSpan, featureAlphabet) 
            else {
				for ((featureName, value) <- getStringSimilarities(srcToken, tgtToken))
					ins.addFeature(featureName, NONE_STATE, currState, value*averageLength, srcSpan, featureAlphabet) 
			}
		}
	}
}