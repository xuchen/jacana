/**
 *
 */
package edu.jhu.jacana.freebase.featureextraction

import edu.jhu.jacana.freebase.questionanalysis.KeywordWebLookup
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.freebase.topicanalysis.TopicParser
import edu.jhu.jacana.freebase.questionanalysis.Question
import scala.collection.JavaConverters._

/**
 * @author Xuchen Yao
 *
 */
object FeatureWriter {

    def main(args: Array[String]): Unit = {
        var inFile = "freebase-data/webquestions/small.json"
        var outFile = "/tmp/dev.bnf"
        if (args.length > 1) {
            inFile = args(0); outFile = args(1)
        }
        val outf = FileManager.getWriter(outFile)
        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource(inFile))

        // val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/small.json"))
        var counter = 0
        for (webq <- qlist /*if counter < 1*/) {
            counter += 1
            val topic = webq.url.split("\\/").last
            val fname = FileManager.getFreebaseResource("freebase/topic-json/" + topic + ".json.gz")
            val graph = TopicParser.Topic2Graph(fname, topic)
            val (one, all) = TopicParser.setAnswer(graph, webq.answers)
            if (one) {
                // ## marks start of a question
                outf.write("##\t%s\t%s\n".format(webq.utterance, webq.answers.mkString(" || ")))
                val q = new Question(webq.utterance)
                val qfeatures = QuestionFeature.extract(q)
                for (node <- graph.vertexSet().asScala) {
                    // # marks start of a node
                    outf.write("#\t%s\n".format(node.text.replace("\n", " ")))
                	val nfeatures = NodeFeature.extract(graph, node, q)
                	val features = pointWiseProduce(qfeatures, nfeatures)
                	if (node.isAnswer)
                	    outf.write("+1 " + features.mkString(" ") + "\n")
                    else
                	    outf.write("-1 " + features.mkString(" ") + "\n")
                }
                // ### marks end of a question
                outf.write("###\n")
            }
        }
        outf.close()
    }
    
    def pointWiseProduce(f1:Array[String], f2:Array[String]): Array[String] = {
        val l1 = f1.size; val l2 = f2.size;
        val f = new Array[String](l1*l2)
        var i = 0; var j = 0; var k = 0;
        while (i < l1) {
            j = 0
            while (j < l2) {
                f(k) = formatFeature(f1(i) + "|" + f2(j))
                k += 1
                j += 1
            }
            i += 1
        }
        return f
    }
    
    def formatFeature(f:String): String = {
        // classias style features in the format of "feature:value"
        // no spaces or : allowed
        return f.replace(" ", "_").replace(":", "=")
    }
}