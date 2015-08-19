/**
 *
 */
package edu.jhu.jacana.align.resource

import edu.jhu.jacana.align.util.Loggable
import edu.jhu.jacana.util.FileManager
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import scala.collection.mutable.HashMap
import edu.jhu.jacana.nlp.SnowballStemmer

/**
 * mined the whole English wiktionary with jwktl and extracted various wordnet-like relations
 * 
 * @author Xuchen Yao
 *
 */
object WiktionaryRelations extends Loggable {

    var dbPath = FileManager.getResource("resources/wiktionary/wiktionary-relations.json.gz")
    val relation2int = new HashMap[String, Integer]()
    var int2relation:HashMap[Integer, String] = null
    val words2relationIdx = new HashMap[String, Integer]()
    init()

    def init(stem:Boolean = false) {
        if (int2relation != null) return
        val s = FileManager.readFile(dbPath)
        val a = JSONValue.parse(s).asInstanceOf[JSONArray]
        val l = a.iterator()
        while (l.hasNext()) {
            val o = l.next().asInstanceOf[JSONObject]
            val word = o.get("word").asInstanceOf[String]
            for(relation <- o.keySet().toArray(new Array[String](o.keySet().size()))
                    .filter(k => !k.equals("word") && !k.equals("pos"))) {
                if (!relation2int.contains(relation))
                    relation2int.put(relation, relation2int.size)
                val idx = relation2int.get(relation).get
                for (word_rel <- o.get(relation).asInstanceOf[String].split(" \\|\\| ")) {
                    // there is a chance that two words fall into different relations
                    // we simply ignore this case as it requires a hashmap from words to set[int]
                    
                    // to deal with the case such as Wikisaurus:strange
                    val word2 = word_rel.split(":", 2).last
                    // if (words2relationIdx.contains(makeKey(word, word2, stem))) {
                    //     println("already in: "+makeKey(word, word2, stem))
                    // }
                    words2relationIdx.put(makeKey(word, word2, stem), idx)
                }
            }
        }
        int2relation = relation2int.map(_.swap)
    }
    
    private def makeKey(s1:String, s2:String, stem:Boolean = false) = {
        if (stem)
            SnowballStemmer.stem(s1.toLowerCase()) + "::" + SnowballStemmer.stem(s2.toLowerCase())
        else
        	s1.toLowerCase()+"::"+s2.toLowerCase()
    }
    
    def getRelation(w1:String, w2:String, stem:Boolean = false): String = {
        var idx:Integer = -1
        idx = words2relationIdx.getOrElse(makeKey(w1,w2, stem), -1)
        if (idx == -1)
            idx = words2relationIdx.getOrElse(makeKey(w2,w1, stem), -1)
        if (idx != -1)
            return int2relation.get(idx).get
        else
        	return null
    }

    def isInWiktionary(w1:String, w2:String, stem:Boolean = false): Integer = {
        if (getRelation(w1, w2, stem) == null)
            0
        else
            1
    }

	def main(args: Array[String]) {
	    println(WiktionaryRelations.getRelation("free", "gratis"))
	    println(WiktionaryRelations.getRelation("free", "free of charge"))
	    println(WiktionaryRelations.getRelation("for nothing", "free"))
	    println(WiktionaryRelations.getRelation("for nothing", "fre"))
	    println(WiktionaryRelations.getRelation("untalkable", "talkable"))
	    println(WiktionaryRelations.getRelation("enteral", "parenteral"))
	}
}