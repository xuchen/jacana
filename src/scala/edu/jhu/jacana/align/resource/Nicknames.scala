/**
 *
 */
package edu.jhu.jacana.align.resource

import edu.jhu.jacana.util.FileManager
import net.liftweb.json._
import scala.io.Source
import scala.collection.mutable.HashSet

/**
 * @author Xuchen Yao
 *
 */
object Nicknames {

    val delimiter = ":"
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    var dbPath = FileManager.getResource("resources/nicknames/nicknames.json")
    
    private val f = Source.fromFile(dbPath)
    private val allInOneString = f.mkString
    f.close()
    private val l = parse(allInOneString).extract[List[NicknameJson]]
    val nicknames = new HashSet[String]()
    for (jj <- l) {
        val splits = jj.SYNONYM.split(" \\|\\| ").toList
        val names = splits :+ jj.word
        val len = names.length
        for (i <- 0 until len) {
        	for (j <- i until len) {
        		nicknames += makeKey(names(i), names(j))
        		nicknames += makeKey(names(j), names(i))
        	}
            
        }
    }
    
    def isNickname(name1:String, name2:String): Boolean = {
        return nicknames.contains(makeKey(name1, name2))
    }
    
    def makeKey(name1:String, name2:String) = name1.toLowerCase()+delimiter+name2.toLowerCase()
    
    
    def main(args: Array[String]): Unit = {
        println(Nicknames.isNickname("aaron", "ron"))
        println(Nicknames.isNickname("will", "William"))
        println(Nicknames.isNickname("will", "bill"))
        println(Nicknames.isNickname("will", "sill"))
    }

}

case class NicknameJson (SYNONYM:String, word:String, pos:String)