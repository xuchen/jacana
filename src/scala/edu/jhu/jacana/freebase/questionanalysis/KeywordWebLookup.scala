/**
 *
 */
package edu.jhu.jacana.freebase.questionanalysis

import scala.io.Source
import net.liftweb.json._
import edu.jhu.jacana.util.FileManager
import java.io.PrintWriter
import java.io.File

/**
 * This object class reads a webquestion json file, does question analysis: 
 * qtype (wh/how etc), qword (country, name etc), and freebase topic (UK, Obama, etc),
 * then outputs to a json file (to be processed by some python script)
 * @author Xuchen Yao
 *
 */
object KeywordWebLookup {
    
    def readQuestionJson(fname: String): List[WebQuestion]  = {
    	implicit val formats = DefaultFormats // Brings in default date formats etc.
        
    	val f = Source.fromFile(fname, "UTF-8")
    	val allInOneString = f.mkString
    	f.close()
    	val l = parse(allInOneString).extract[List[WebQuestion]]
    	return l
    }

    def main(args: Array[String]): Unit = {
        val qlist = readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.test.json"))
        // val qlist = readQuestionJson(FileManager.getResource("freebase-data/webquestions/small.json"))
        val topicJson = for (q <- qlist) yield (new Question(q.utterance)).toJson
        // println(compact(render(JArray(topicJson))))
        // println(pretty(render(JArray(topicJson))))
       	var writer = new PrintWriter(new File("/tmp/topics.json"))
        writer.write(pretty(render(JArray(topicJson))))
        writer.close()
    }

}

case class WebQuestion(url: String, targetValue: String, utterance: String, retrievedList: Option[String]) {
    var answers:Array[String] = getAnswers()

    def getAnswers():Array[String] = {
        if (answers != null) return answers
        // {"url": "http://www.freebase.com/view/en/justin_bieber", 
        // "targetValue": "(list (description \"Jazmyn Bieber\") (description \"Jaxon Bieber\"))", 
        // "utterance": "what is the name of justin bieber brother?"}
        var r = "\\(description \"?(.*?)\"?\\)".r
        var i = r.findAllMatchIn(targetValue)
        answers = (for (m <- i) yield m.group(1)).toArray
        return answers
    }
    
    def getRetrievedList(): Array[(String, Double)] = {
        retrievedList match {
            case Some(ll) => for (ss <- ll.split(" "); splits = ss.split(":")) yield (splits(0), splits(1).toDouble)
            case None => return null
        }
    }
}