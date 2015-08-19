/**
 *
 */
package edu.jhu.jacana.align.resource

import scala.collection.mutable.HashMap
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.align.util.Loggable
import scala.collection.mutable.ArrayBuffer

/**
 * String similarity measured by the UMBC EBIQUITY system.
 * 
 * Lushan Han, Abhay L. Kashyap, Tim Finin, James Mayfield, and Johnathan Weese. 2013.
 * UMBC EBIQUITY-CORE: Semantic Textual Similarity Systems. in Proceedings of 
 * the Second Joint Conference on Lexical and Computational Semantics.
 * @author Xuchen Yao
 *
 */
object UmbcSimilarity extends Loggable {

    var dbPath = FileManager.getResource("resources/paraphrase/umbc-ebiquity_RTE06MSR_PARAPHRASE08CCL_upTo4grams.txt.gz")
    var phrase2scores = new HashMap[String, Array[Double]]()
    var ruleSize = 2
    
    val UMBC_WO_PENALTY = "umbc-without-penalty"
    val UMBC_PENALTY = "umbc-with-penalty"
    
    init()
    
    def init(dbName:String = dbPath) {
        val reader = FileManager.getReader(dbName)
        var line:String = null
        line = reader.readLine()
        while (line != null) {
            val Array(p1, p2, score1, score2) = line.split("\\t") 
            val phrase = makePhrase(p1, p2)
            val scoreArray = Array[Double](score1.toDouble, score2.toDouble)
            phrase2scores.put(phrase, scoreArray) 
            line = reader.readLine()
        }
        reader.close()
    }
        
    def getFeatScores(tokens1: Array[String], tokens2: Array[String]): ArrayBuffer[(String, Double)] = {
        val s1 = tokens1.mkString(" ")
        val s2 = tokens2.mkString(" ")
        //val averageLength = 0.5*(tokens1.size+tokens2.size)
        val averageLength = Math.max(0.5*(tokens1.size+tokens2.size), tokens1.size)
        val buffer = new ArrayBuffer[(String, Double)]()
        if (s1 == s2) {
            buffer.append((UMBC_WO_PENALTY, 1.0*averageLength))
            buffer.append((UMBC_PENALTY, 1.0*averageLength))
            return buffer
        }
        val p1 = makePhrase(s1, s2)
        var scores: Array[Double] = null
        if (phrase2scores.contains(p1)) {
            scores = phrase2scores.get(p1).get
        } else {
	        val p2 = makePhrase(s2, s1)
	        if (phrase2scores.contains(p2)) {
	            scores = phrase2scores.get(p2).get
	        }
        }
        if (scores != null) {
        	buffer.append((UMBC_WO_PENALTY, scores(0)*averageLength))
        	buffer.append((UMBC_PENALTY, scores(1)*averageLength))
        }
        return buffer
    }
 
    private def makePhrase(s1: String, s2: String): String = s1+"::"+s2

    def main(args: Array[String]): Unit = {
        println(getFeatScores(Array("asd"), Array("jlads")))
        println(getFeatScores(Array("system", "."), Array("computer", ".")))
        println(getFeatScores(Array("computer", "."), Array("system", ".")))
        println(getFeatScores(Array("system"), Array("computer")))
        println(getFeatScores(Array("computer"), Array("system")))
    }

}