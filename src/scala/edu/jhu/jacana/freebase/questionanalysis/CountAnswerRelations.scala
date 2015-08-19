/**
 *
 */
package edu.jhu.jacana.freebase.questionanalysis

import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.freebase.topicanalysis.TopicParser
import scala.collection.JavaConverters._
import edu.jhu.jacana.freebase.topicanalysis.FreebaseNode
import scala.collection.mutable.HashMap

/**
 * this object class counts/sorts the number of times of the relation of an answer node
 * in the training data
 * @author Xuchen Yao
 *
 */
object CountAnswerRelations {

    def main(args: Array[String]): Unit = {
        
        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.train.json"))
        var counter = 0
        val rel2count = new HashMap[String, Int]()
        for (webq <- qlist /*if counter < 1*/) {
            val topic = webq.url.split("\\/").last
            val fname = FileManager.getFreebaseResource("topic-json/" + topic + ".json.gz")
            val graph = TopicParser.Topic2Graph(fname, topic)
            val (one, all) = TopicParser.setAnswer(graph, webq.answers)
            if (one) {
            	counter += 1
                for (node <- graph.vertexSet().asScala if node.isAnswer) {
                    val relations = FreebaseNode.getIncomingRelationFromNode(graph, node)
                    for (rel <- relations) {
                        if (!rel2count.contains(rel))
                            rel2count(rel) = 0
                        rel2count(rel) += 1
                    }
                }
            }
        }
        for ((rel,count) <- rel2count.toList.sortBy(t => -t._2))
            println(count + " " + rel)
    }

}