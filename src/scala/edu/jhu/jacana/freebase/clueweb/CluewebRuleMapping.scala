/**
 *
 */
package edu.jhu.jacana.freebase.clueweb

import edu.jhu.jacana.util.FileManager
import scala.collection.mutable.Map
import scala.collection.mutable.HashMap
import edu.jhu.jacana.freebase.topicanalysis.FreebaseRelation
import edu.jhu.jacana.nlp.SnowballStemmer

/**
 * this object class loads mapping between Freebase relations and Clueweb words, represented
 * by conditional log_e probabilities t(word|rel).
 * 
 * Suppose a question is represented by a word vector (w_vector),
 * a relation is represented by a relation vector (v_vector, e.g., people.person.parent
 * is transformed into a vector of (people, person, parent, people.person.parent),
 * and a relation set (R), to find out the most likely relation this question maps to,
 * we compute: p(rel|question) ~= p(r_vector | w_vector)
 * 					   			= p(w_vecotr | r_vector) p(r_vector) / p(w_vector)
 * 					  		   ~= p(w_vecotr | r_vector) p(r_vector)
 *                    		   ~= Muti_w p(w | r) p(r)
 *                    		   ~= Sum_w t(w | r) + t(r)
 * where p() is the probability and t() is the log probability
 * t(r) is a vector of 10484 elements
 * t(w|r) is a matrix of 10484 x 20,000 elements
 * 
 * files (FACC1: Freebase Annotation of the Clueweb Corpus, http://lemurproject.org/clueweb09/FACC1/):
 * idx2word.txt.gz: word, index, total_count_from_FACC1, log_e_prob(i.e., t(r))
 * idx2rel.txt.gz: rel, index, total_count_from_FACC1, log_e_prob(i.e., t(w))
 * all.prob_word_given_rel.grow-diag-final-and.txt.gz: t(r|w), 10484 rows/rules, 20,000 columns/words
 * 
 * Note: all words stored in file have been stemmed by Snowball (relations are not)
 * 
 * @author Xuchen Yao
 *
 */
object CluewebRuleMapping {
    // val dataDir = System.getProperty("user.home") + "/Halo2/freebase/CluewebRelations/"
   	val idx2relFname = FileManager.getFreebaseResource("CluewebRelations/idx2rel.txt.gz")
   	val idx2wordFname = FileManager.getFreebaseResource("CluewebRelations/idx2word.txt.gz")
   	val probWordGivenRelFname = FileManager.getFreebaseResource("CluewebRelations/all.prob_word_given_rel.grow-diag-final-and.txt.gz")
   	// val probWordGivenRelFname = dataDir + "all.prob_word_given_rel.grow-diag-final.txt.gz"
   	// val probWordGivenRelFname = dataDir + "all.prob_word_given_rel.intersect.txt.gz"
   	// val probWordGivenRelFname = dataDir + "all.prob_word_given_rel.union.txt.gz"
   	    
   	val rel2idx = Map[String, Int]()
   	val rel2prob = Map[String, Float]()
   	val word2idx = Map[String, Int]()
   	val word2prob = Map[String, Float]()
   	
   	loadVocabIdxProb(idx2relFname, rel2idx, rel2prob)
   	loadVocabIdxProb(idx2wordFname, word2idx, word2prob)
   	
    // log_p(0) = -inf, we store it as 0 in the matrix (same as log(1) unfortunately)
   	// val minusInfInLog:Float = 0
   	val minusInfInLog:Float = -30
   	val probMatrix = Array.ofDim[Float](rel2idx.size, word2idx.size)
   	loadProbMatrix(probWordGivenRelFname, probMatrix)
   	
   	def loadProbMatrix (fname:String, probMatrix:Array[Array[Float]]) {
        val reader = FileManager.getReader(fname)
        var line = reader.readLine()
        var c = 0
        while (line != null) {
            // each line looks like:
            // -6.57286 -inf -inf -1.34120 ... (20,000 elements long)
            fastSplit(probMatrix(c), line)
            c += 1
            line = reader.readLine()
        }
        reader.close()
    }
   	    
   	def loadVocabIdxProb (fname:String, vocab2idx:Map[String, Int], vocab2prob: Map[String, Float]=null) {
        val reader = FileManager.getReader(fname)
        var line = reader.readLine()
        while (line != null) {
            // location 0 829727276 -2.06583
            val Array(v, i, _, p) = line.split(" ")
            vocab2idx += v -> i.toInt
            if (vocab2prob != null)
                vocab2prob += v -> p.toFloat
            line = reader.readLine()
        }
        reader.close()
    }
    
    @inline def fastSplit(arr: Array[Float], line: String) {
        var c = 0
        var lastIdx = 0
        var idx = line.indexOf(' ')
        while (idx != -1) {
            arr(c) = str2float(line.substring(lastIdx, idx))
            lastIdx = idx
            c += 1
            idx = line.indexOf(' ', idx + 1)
        }
        arr(c) = str2float(line.substring(lastIdx))
        // println(c)
    }
    
    @inline def str2float(str: String): Float = {
        str match {
            case x if x.trim() == "-inf" => minusInfInLog
            case x => x.toFloat
        }
    }
    
   
    def jointProbOfRelationAndWords(qtokensLowercaseStemmed:Array[String], relation:String): Double = {
        var score = 0.0
        var relIdx = rel2idx.getOrElse(relation, -1)
        if (relIdx != -1) {
            score += rel2prob(relation)
        	for (w <- qtokensLowercaseStemmed if word2idx.contains(w)) {
        	    score += probMatrix(relIdx)(word2idx.get(w).get)
        	}
        }
        return score
    }
    
    /**
     * Given a question, return the log probability of p(relation | question)
     * rule looks like /people/person/parents
     */
    def scoreRelationMapping(qtokensLowercaseStemmed:Array[String], relation:String): Double = {
        var score = 0.0
        val dotRel = RuleMappingEvaluation.dotBasedRelation(relation)
        
        // TODO: adjust weights for relations of different levels
        score += jointProbOfRelationAndWords(qtokensLowercaseStemmed, dotRel)
       
        // whole rule not found
        if (score == 0.0) {
            val rels = FreebaseRelation.splitRelation(relation)
            for ((rel,i) <- rels.reverse.zipWithIndex)
                score += math.pow(2,i)*jointProbOfRelationAndWords(qtokensLowercaseStemmed, rel)/rels.size
                // 2**i as a weighting factor
        }

        return score
    }
    
    def printTopRelations(top: Int) {
        val sorted = rel2prob.toSeq.sortWith(_._2 > _._2)
        println((sorted slice(0, top)).mkString("\n"))
    }

    def getTopRelations(top: Int): List[String] = {
        val sorted = rel2prob.toSeq.sortWith(_._2 > _._2)
        val seq = for (tuple <- sorted slice(0, top)) yield tuple._1
        return seq.toList
    }

    def printTopWords(top: Int) {
        val sorted = word2prob.toSeq.sortWith(_._2 > _._2)
        println((sorted slice(0, top)).mkString("\n"))
    }

    def printTopWordsGivenRelation(relations: List[String], top: Int = 10) {
        var score = 0.0
        val idx2word = new Array[String](word2idx.size)
        word2idx.foreach {
            case (word, idx) => idx2word(idx) = word
        }
        
        for (relation <- relations) { 
            println("==================")
            println(relation)
        	var relIdx = rel2idx.getOrElse(relation, -1)
        	if (relIdx != -1) {
        		val word_idx2prob = Map[Int, Float]()
        		probMatrix(relIdx).view.zipWithIndex foreach {
        			case (score, i) => word_idx2prob += i -> score
        		}
        		val sorted = word_idx2prob.toSeq.sortWith(_._2 > _._2)
        		for (i <- 0 until top) {
        		    println(idx2word(sorted(i)._1))
        		}
        	} else {
        	    println("Warning: relation not found: " + relation)
        	}
            println()
            println()
        }
    }
    
    def main(args: Array[String]): Unit = {
        println(rel2prob.size)
        // println(probMatrix(0).toList)
        // printTopRelations(200)
        // println()
        // println()
        // printTopWords(50)
        printTopWordsGivenRelation(getTopRelations(200))
    }

}