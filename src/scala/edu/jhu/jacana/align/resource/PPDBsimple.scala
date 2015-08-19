/**
 *
 */
package edu.jhu.jacana.align.resource

import edu.jhu.jacana.util.FileManager
import scala.collection.mutable.HashSet
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.nlp.SnowballStemmer

/**
 * @author Xuchen Yao
 *
 */
object PPDBsimple {

    var dbPathToken:String = FileManager.getResource("resources/ppdb/ppdb-1.0-eng-xxxl.pruned-20.lexical.pair.gz")
    var dbPathPhrase:String = FileManager.getResource("resources/ppdb/ppdb-1.0-xl-phrasal.pair.gz")
    val pairs = new HashSet[String]()
    init()
    
    def init(dbName:String = dbPathToken, stem:Boolean = false) {
        if (pairs.size > 0) return // already initialized
        readPPDB(dbPathToken, stem)
        if (AlignerParams.phraseBased)
        	readPPDB(dbPathPhrase)
    }
    
    private def readPPDB(dbName:String = dbPathToken, stem:Boolean = false) {
        val reader = FileManager.getReader(dbName)
        var line:String = null
        line = reader.readLine()
        while (line != null) {
            val splits = line.split("\t") 
            val p1 = splits(0); val p2 = splits(1)
            pairs += makePhrase(p1, p2, stem)
            line = reader.readLine()
        }
        reader.close()       
    }

    def isInPPDB(w1:String, w2:String, stem:Boolean = false): Int = {
        if (pairs.contains(makePhrase(w1,w2, stem)) || pairs.contains(makePhrase(w2,w1, stem)))
            return 1
        else
            return 0
    }

    private def makePhrase(s1: String, s2: String, stem:Boolean = false): String = {
        if (stem)
            SnowballStemmer.stem(s1.toLowerCase()) + "::" + SnowballStemmer.stem(s2.toLowerCase())
        else
        	s1.toLowerCase()+"::"+s2.toLowerCase()
    }
    
    
	def main(args: Array[String]) {
        println(isInPPDB("10", "10th"))
        println(isInPPDB("agent", "officer"))
        println(isInPPDB("agent", "freedom"))
    }
}