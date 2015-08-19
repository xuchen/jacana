/**
 *
 */
package edu.jhu.jacana.freebase.clueweb

import edu.jhu.jacana.freebase.questionanalysis.KeywordWebLookup
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.freebase.topicanalysis.TopicParser
import edu.jhu.jacana.freebase.questionanalysis.Question
import scala.collection.JavaConverters._
import edu.jhu.jacana.freebase.featureextraction._
import edu.jhu.jacana.freebase.topicanalysis.FreebaseNode
import scala.collection.mutable.ArrayBuffer
import edu.jhu.jacana.nlp.SnowballStemmer
import scala.collection.mutable.HashSet

/**
 * this object class evaluates how rule mapping learned from ClueWeb, ReVerb, or word overlapping
 * performs against the freebase relations, in terms of MAP, MRR, median rank and rank in percentile.
 * @author Xuchen Yao
 *
 */
object RuleMappingEvaluation {
     /**
     * Freebase uses either / or . as rule separators while our vocabulary uses .
     * this method replaces / with .
     */
    @inline def dotBasedRelation(relation:String):String = {
        var r = relation.replace("/", ".")
        if (r.startsWith("."))
            return r.substring(1)
        else
            return r
    }
    
	import MappingType._
    def countOverlap(sent: String, strs: Array[String]): Int = {
        val sentLower = sent.toLowerCase()
        var c = 0
        for (s <- strs if sentLower.contains(s)) c += 1
        return c
    }
    
    def countSetOverlap(sent: String, strs: Array[String]): Int = {
        val sentSet = sent.toLowerCase().split(" ").toSet
        val strsSet = strs.toSet
        return (sentSet & strsSet).size
    }
    
    def answerRank(node_score: ArrayBuffer[(Double, Boolean, String)], ansScore: Double): Int = {
        var rank = 0
        for ((c, _, _) <- node_score if c >= ansScore) rank += 1
        // println(node_score)
        println(s"answer node's relation ranked number $rank")
        return rank
    }
    
    def baselineScore(question:String, relations:Array[String]): Double = {
        return countSetOverlap(question, relations).toDouble
    }
    
    def mappingScore(qtokensLowercaseStemmed:Array[String], relations:Array[String], nodeInfo:String,
            scoringFunction: (Array[String], String) => Double): Double = {
        var score = 0.0
        if (relations.size == 0) return Double.MinValue
        for (rel <- relations)
        	score += scoringFunction(qtokensLowercaseStemmed, rel)
        
        score /= relations.size
        // println(f"$score%.2f ${relations.toList} $nodeInfo")
        return if (score  == 0.0) Double.MinValue else score
    }
    
    def takeOutNamedEntities(qtokensLowercaseStemmed:Array[String], ner:Set[String]): Array[String] = {
        val nerLowercaseStemmed = new HashSet[String]()
        for (w <- ner)
            for (ww <- w.split(" "))
            	nerLowercaseStemmed  += SnowballStemmer.stem(ww.toLowerCase())
        return qtokensLowercaseStemmed.filter(x => !nerLowercaseStemmed.contains(x))
    }
    
    def main(args: Array[String]): Unit = {
        
        val mappingType = CLUEWEB
        // val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/small.json"))
        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.train.json"))
        var counter = 0; var totalRank = 0
        val ranker = new MapMrr()
        for (webq <- qlist /*if counter < 1*/) {
            val topic = webq.url.split("\\/").last
            val fname = FileManager.getFreebaseResource("topic-json/" + topic + ".json.gz")
            val graph = TopicParser.Topic2Graph(fname, topic)
            val (one, all) = TopicParser.setAnswer(graph, webq.answers)
            if (one) {
            	counter += 1
            	println(webq.utterance)
            	var node_score = new ArrayBuffer[(Double, Boolean, String)]()
            	var answerScore = Double.MinValue
                var q: Question = null
            	if (mappingType != BASELINE)
            		q = new Question(webq.utterance)
            	var relationText = ""
                var scoringFunction: (Array[String], String) => Double = null
                if (mappingType == CLUEWEB) {
                    scoringFunction = CluewebRuleMapping.scoreRelationMapping
                } else if (mappingType == REVERB) {
                    scoringFunction = ReverbRuleMapping.scoreRelationMapping
                }
                for (node <- graph.vertexSet().asScala) {
                    var score = 0.0
                    if (mappingType == BASELINE) {
                        val relations = FreebaseNode.getIncomingRelationSplitsFromNode(graph, node)
                    	score = baselineScore(webq.utterance, relations)
                    	relationText = relations.mkString(" ")
                    }
                    else {
                        val relations = FreebaseNode.getIncomingRelationFromNode(graph, node)
                    	// score = mappingScore(takeOutNamedEntities(q.qtokensLowercaseStemmed, q.candidateTopics.map(x => x._1).toSet),
                    	score = mappingScore(q.qtokensLowercaseStemmed,
                    	        relations,
                    	        if(node.isAnswer) node.text+" | isAnswer" else node.text,
                    	        scoringFunction)
                    	relationText = relations.mkString(" ")
                    }
                    // node_score += Tuple3(score, node.isAnswer, relationText+" | "+node.text)
                    node_score += Tuple3(score, node.isAnswer, relationText)
                    if (node.isAnswer && score > answerScore)
                        answerScore = score
                }
            	ranker.addResult(node_score.toList, false)
                totalRank += answerRank(node_score, answerScore)
                println()
            }
        }
        // println(f"average rank: ${totalRank*1.0/counter}%.2f")
        println(f"average rank: ${ranker.getMeanRank()}%.2f")
        println(f"median rank: ${ranker.getMedianRank()}")
        println(f"MAP: ${ranker.getMAP()}%.4f")
        println(f"MRR: ${ranker.getMRR()}%.4f")
        ranker.printRevelantCounts
    }
}

object MappingType extends Enumeration {
    type MappingType = Value
    val BASELINE, CLUEWEB, REVERB = Value
}