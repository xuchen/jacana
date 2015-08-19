/**
 *
 */
package edu.jhu.jacana.freebase.clueweb

import scala.collection.mutable.HashSet
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.nlp.SnowballStemmer

/**
 * This object class loads the binary/unary rules (through either surface
 * matching or alignment) learned from intersecting Freebase relations and
 * ReVerb tuples (see Berant. et. al. 2013 EMNLP) and builds a smiple word
 * overlap model for any pairs of (Freebase relation, NL text)
 * @author Xuchen Yao
 *
 */
object ReverbRuleMapping {
    
    // val topDir = "/Volumes/Data1/xuchen/workspace/sempre/lib/fb_data/6/"
    val fnames = Array(FileManager.getFreebaseResource("ReverbMapping/binaryInfoStringAndAlignment.txt.gz"),
            FileManager.getFreebaseResource("ReverbMapping/unaryInfoStringAndAlignment.txt.gz"))
    
    // just naively store whether any rel#word pair existed from the intersection files
    val relTextSet = new HashSet[String]()
    // val relSet = new HashSet[String]()
    loadFiles()
    
    def loadFiles() {
        // the formulas are in first-order logic form, we can't interpret them, but
        // simply split them and treat each one independently
        val pattern = """.*formula":"(.*?)","source.*lexeme":"(.*)"}.*""".r
        val patternRel = """[!]*fb:(\S+)""".r
        for (fname <- fnames) {
            val reader = FileManager.getReader(fname)
            var line = reader.readLine()
            var c = 0
            while (line != null) {
                c += 1
                // each line looks like:
                // {"formula":"fb:music.artist.track","source":"STRING_MATCH","features":
                // {"FB_typed_size":0.0,"Intersection_size_typed":0.0,"NL-size":0.0,"NL_typed_size":0.0},
                // "lexeme":"tracks recorded"}
                pattern.findFirstMatchIn(line) match {
                    case Some(m) => val relCompound = m.group(1); val words = m.group(2)
                    		// if (c % 1000 == 0) println(relCompound)
                    		val stems = words.split(" ").map(x => SnowballStemmer.stem(x.toLowerCase()))
                    		for (relM <- patternRel.findAllMatchIn(relCompound)) {
                    		    var rel = relM.group(1)
                    		    if (rel.endsWith(")")) rel = rel.substring(0, rel.length()-1)
                    		    // relSet += rel
                    		    if (rel != "type.object.type") {
                    		        // unary rule contains too many "type.object.type" which skew
                    		        // the scoring badly, we rule it out here
                    		        for (w <- stems) {
                    		    	    val key = makeKey(rel, SnowballStemmer.stem(w.toLowerCase()))
                    		    	    relTextSet += key
                    		    	    // if (c % 1000 == 0) println("\t" + key)
                    		        }
                    		    }
                    		}
                    case _ =>
                }
                line = reader.readLine()
            }
            reader.close()
        }
    }

    def scoreRelationMapping(qtokensLowercaseStemmed:Array[String], relation:String): Double = {
        var score = 0.0
        val dotRel = RuleMappingEvaluation.dotBasedRelation(relation)
        for (w <- qtokensLowercaseStemmed) {
            val key = makeKey(dotRel, w)
            if (relTextSet.contains(key)) score += 1
        }
        return score
    }

    @inline def makeKey(rel:String, wordLoweredStemmed:String) = rel+"#"+wordLoweredStemmed

    def main(args: Array[String]): Unit = {
        println(ReverbRuleMapping.relTextSet.size) 
        // println(ReverbRuleMapping.relSet.mkString("\n")) 
    }

} 