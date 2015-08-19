/**
 *
 */
package edu.jhu.jacana.validation.reader
import scala.io.Source
import net.liftweb.json._
import scala.collection.mutable.ArrayBuffer

/**
 * @author Xuchen Yao
 *
 */
class ExamInputJsonReader (fname: String, noStoryQuestion:Boolean = true) extends Iterator[Question] {
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    private val f = Source.fromFile(fname, "UTF-8")
    private val allInOneString = f.mkString
    f.close()
    val l = parse(allInOneString).extract[List[Question]].filter(
            q => if (noStoryQuestion) q.isStory == false else true)
            
    for (q <- l) filterByUrl(q)

    private var current = 0
    
    def hasNext(): Boolean = {
        if (current < l.length) return true else return false
    }
    
    def next(): Question = {
        current += 1
        return l(current-1)
    }
    
    def filterByUrl(q:Question) {
        val qr  = q.question.toLowerCase().r
        for (qos <- q.answers) {
            qos.snippets = qos.snippets.filter(s => 
                qr.findFirstIn(s.snippet.toLowerCase()).isEmpty
                && notInUrlBlackList(s.source))
        }
    }
    
    def notInUrlBlackList(url:String): Boolean = {
        val s = url.toLowerCase()
        return  (!s.contains("wnyric") && !s.contains("nysedregents")
                && !s.contains("regents") && !s.contains("quizlet.com")
                && !s.contains("nytest") && !s.contains("quiz")
                && !s.contains("slcschools.org") && !s.contains("warehamps.org")
                && !(s.contains("science") && s.contains("grade")))
    }
    
    def getAll() = l
    
    def getTrain() = l.filter(q => q.trainDevTest.equalsIgnoreCase("train"))
    def getDev() = l.filter(q => q.trainDevTest.equalsIgnoreCase("dev"))
    def getTest() = l.filter(q => q.trainDevTest.equalsIgnoreCase("test"))
}

case class Snippet(snippet:String, snippet_align:String, snippet_tokenized:String, 
        		   source:String, rank:Int, title:String, title_align:String, title_tokenized:String)
case class QuestionOptionSearch(query:String, query_tokenized:String, var snippets:List[Snippet])
case class Question(qid:String, question:String, answers:List[QuestionOptionSearch], 
        		     question_snippet:List[Snippet], answer:Int,
        		     options:List[String], isStory:Boolean, 
        		     trainDevTest:String, var scores:Option[List[String]])
        
object ExamInputJsonReader {
    
	def main(args: Array[String]): Unit = {
	    val reader = new ExamInputJsonReader("/Users/xuchen/Vulcan/Halo2/Retrieved/bing.4.tiny.json")
	    println(reader.l.length)
	    for (q <- reader.l) {
	        for (qos <- q.answers) {
	            println(qos.snippets.length)
	        }
	    }
	}
}