/**
 *
 */
package edu.jhu.jacana.align.resource

import edu.jhu.jacana.util.FileManager
import scala.collection.mutable.HashMap
import gnu.trove.map.hash.TIntDoubleHashMap
import edu.jhu.jacana.align.Alphabet
import scala.collection.mutable.ArrayBuffer
import edu.jhu.jacana.align.util.AlignerParams

/**
 * reads in a gzipped PPDB file and return various scores of two phrases, if found.
 * @author Xuchen Yao
 *
 */
object PPDB {
    
    val NORMALIZING_CONSTANT = 15.0
    
    //var dbPath = FileManager.getResource("resources/paraphrase/ppdb_RTE06MSR_PARAPHRASE08CCL_upTo4grams.txt.gz")
    var dbPath:String = null
    if (AlignerParams.phraseBased)
        dbPath = FileManager.getResource("resources/paraphrase/ppdb_RTE06MSR_PARAPHRASE08CCL_upTo4grams_v1.0.txt.gz")
    else
        dbPath = FileManager.getResource("resources/ppdb/ppdb-1.0-eng-xxxl.pruned-20.lexical.gz")
            
    var phrase2scores = new HashMap[String, Array[Double]]()
    // a mapping between phrase to their syntactic class
    // e.g. [x] ||| keep the ||| to keep
    // would have: keep the::to keep -> [x]
    var phrase2synt = new HashMap[String, String]()
    
    // holds a mapping between 	rule feature (such as p(e|f)) and its ID (starting from 0)
    val ruleFeatureAlphabet = new Alphabet() 
    var ruleSize = -1
    init()
    
    def init(dbName:String = dbPath) {
        if (ruleSize != -1) return // already initialized
        val reader = FileManager.getReader(dbName)
        var line:String = null
        line = reader.readLine()
        var lineNum = 0
        while (line != null) {
            lineNum += 1
            val Array(synt, f, e, scores) = line.split(" \\|\\|\\| ") 
            val phrase = makePhrase(f, e)
            phrase2synt.put(phrase, synt)
            phrase2synt.put(makePhrase(e, f), synt)
            if (lineNum == 1)
                initRuleFeatureAlphabet(scores)
            val scoreArray = new Array[Double](ruleSize)
            for (field <- scores.split("\\s+")) {
                val splits = field.split("=")
	                if (splits.length == 2) {
	                val Array(feat, score) = splits
	                if (ruleFeatureAlphabet.contains(feat)) {
	                    scoreArray(ruleFeatureAlphabet.getWithoutPut(feat)) = score.toDouble
	                }
                }
            }
            phrase2scores.put(phrase, scoreArray) 
            line = reader.readLine()
        }
        reader.close()
    }
    
    def getFeatScores(tokens1: Array[String], tokens2: Array[String]): ArrayBuffer[(String, String, Double)] = {
        val s1 = tokens1.mkString(" ")
        val s2 = tokens2.mkString(" ")
        val averageLength = Math.max(0.5*(tokens1.size+tokens2.size), tokens1.size)
        val buffer = new ArrayBuffer[(String, String, Double)]()
        if (s1 == s2) {
            for (f <- ruleFeatureAlphabet.getStrings()) {
                buffer.append(("identical", f+".f2e", 1.0*averageLength))
                buffer.append(("identical", f+".e2f", 1.0*averageLength))
                //buffer.append(("identical", f+".f2e", 1.0/averageLength))
                //buffer.append(("identical", f+".e2f", 1.0/averageLength))
            }
            return buffer
        }
        val p1 = makePhrase(s1, s2)
        if (phrase2scores.contains(p1)) {
            val scores = phrase2scores.get(p1)
            scores.get.zipWithIndex foreach {case (x,i) => buffer.append((phrase2synt.get(p1).get, ruleFeatureAlphabet.getString(i)+".f2e", Math.exp(-x)*averageLength))}
            //scores.get.zipWithIndex foreach {case (x,i) => buffer.append((phrase2synt.get(p1).get, ruleFeatureAlphabet.getString(i)+".f2e", Math.abs(x/averageLength)))}
        }
        val p2 = makePhrase(s2, s1)
        if (phrase2scores.contains(p2)) {
            val scores = phrase2scores.get(p2)
            scores.get.zipWithIndex foreach {case (x,i) => buffer.append((phrase2synt.get(p2).get, ruleFeatureAlphabet.getString(i)+".e2f", Math.exp(-x)*averageLength))}
            //scores.get.zipWithIndex foreach {case (x,i) => buffer.append((phrase2synt.get(p2).get, ruleFeatureAlphabet.getString(i)+".e2f", Math.abs(x/averageLength)))}
        }
        /*
        if (buffer.size == 0) {
             for (f <- ruleFeatureAlphabet.getStrings()) {
                buffer.append(("not_found", f+".f2e", 14.0/averageLength))           
                buffer.append(("not_found", f+".e2f", 14.0/averageLength))           
             }
        }
        */
        return buffer
    }
    
    private def makePhrase(s1: String, s2: String): String = s1+"::"+s2
    
    private def initRuleFeatureAlphabet(scores: String) {
         for (field <- scores.split("\\s+")) {
            if (field.startsWith(raw"p(")) {
                // only care about the p(*) scores such as:
                // p(lhs|e)=2.86561 p(lhs|f)=3.22423 p(e|lhs)=12.35359 ...
                val Array(feat, _) = field.split("=")
                ruleFeatureAlphabet.put(feat)
            }
        }
         ruleSize = ruleFeatureAlphabet.size()
    }
    

    def main(args: Array[String]): Unit = {
        println(getFeatScores(Array("asd"), Array("jlads")))
        println(getFeatScores(Array("asd"), Array("asd")))
        println(getFeatScores(Array("system", "."), Array("computer", ".")))
        println(getFeatScores(Array("computer", "."), Array("system", ".")))
        println(getFeatScores(Array("system"), Array("computer")))
        println(getFeatScores(Array("computer"), Array("system")))
        println(phrase2scores.size)
        println("%d MB memory currently used".format(Runtime.getRuntime().totalMemory()/1000000))
    }

}