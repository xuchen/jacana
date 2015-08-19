/**
 *
 */
package edu.jhu.jacana.freebase.featureextraction

import edu.jhu.jacana.freebase.questionanalysis.KeywordWebLookup
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.freebase.topicanalysis.TopicParser
import edu.jhu.jacana.freebase.questionanalysis.Question
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
 * @author Xuchen Yao
 *
 */
object GraphFeatureWriter {

    def main(args: Array[String]): Unit = {
        var inFile = "freebase-data/webquestions/small.json"
        var outFile = "/tmp/dev.bnf"
        var goldRetrieval = true
        if (args.length > 1) {
            inFile = args(0); outFile = args(1)
        }
        if (args.length > 2) { goldRetrieval = args(2).toBoolean }
        val outf = FileManager.getWriter(outFile)
        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource(inFile))
        // val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.dev.20.json"))
        var counter = 0; var totalFeatures = 0
        for (webq <- qlist /*if counter < 1*/) {
            counter += 1
            if (counter % 100 == 0) println(counter+"\n\n\n\n")
            // (topic, score) tuple
            val topics = new ArrayBuffer[(String, Double)]
            if (goldRetrieval) {
            	val topic = webq.url.split("\\/").last
            	topics.append((topic, 1000.0))
            } else {
                val rList = webq.getRetrievedList
                if (rList != null) { for (x <- rList) topics.append(x) }
            }
            // ## marks start of a question
            outf.write("##\t%s\t%s\n".format(webq.utterance, webq.answers.mkString(" || ")))
            
            for (((topic,score), rank) <- topics.zipWithIndex) {
                // #_# marks start of a topic
            	outf.write("#_#\t%d\t%s\t%.3f\n".format(rank, topic, score))

                val fname = FileManager.getFreebaseResource("topic-json/" + topic + ".json.gz")

                if (FileManager.fileExists(fname)) {
                val graph = TopicParser.Topic2Graph(fname, topic)
                val (one, all) = TopicParser.setAnswer(graph, webq.answers)
                if (one || !goldRetrieval) {
                    // if found one answer (during training or testing with gold(oracle) retrieval
                    // OR if during test (we don't know the answer)
                    val q = new Question(webq.utterance)
                    val qGraph = new FeatureGraphOnQuestion(q)
                    val fGraph = new FeatureGraphOnFreebase(graph, q)
                    val qfeatures = qGraph.getFeatures
                  	// println("question feature size: " + qfeatures.size)
                    for (node <- graph.vertexSet().asScala) {
                        // # marks start of a node
                        outf.write("#\t%s\n".format(node.text.replace("\n", " ")))
                    	val nfeatures = fGraph.getFeatures(node)
                    	// val afeatures = getAlignmentFeatures(nfeatures)
                    	// println("node feature size: " + nfeatures.size)
                    	totalFeatures += nfeatures.size
                    	// val features = pointWiseProduce(qfeatures, nfeatures) ++ afeatures
                    	val features = pointWiseProduce(qfeatures, nfeatures)
                    	// println("pointwise feature size: " + features.size)
                    	if (node.isAnswer)
                    	    outf.write("+1 " + features.mkString(" ") + "\n")
                        else
                    	    outf.write("-1 " + features.mkString(" ") + "\n")
                    }
                }
                } else {
                    println(s"Warning: $fname doesn't exist")
                }
            }
            // ### marks end of a question
            outf.write("###\n")
        }
        outf.close()
        println("total features: " + totalFeatures)
    }
    
    def getAlignmentFeatures(f:List[String]): List[String] = {
        return f.filter(s => s.contains("_rank"))
    }
    
    def pointWiseProduce(f1:List[String], f2:List[String]): Array[String] = {
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