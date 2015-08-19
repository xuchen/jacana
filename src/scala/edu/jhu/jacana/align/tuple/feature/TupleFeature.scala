/**
 *
 */
package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.AlignedTuple
import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import edu.jhu.jacana.align.tuple.reverb.FragAnno

/**
 * @author Xuchen Yao
 *
 */
abstract class TupleFeature {
    // cross-product between (Arg1, Rel1, Arg2) in tuple 1 and (Arg1, Rel2, Arg2) in tuple 2
    protected var features = Array[String](".Arg1Arg1", ".Arg1Rel2", ".Arg1Arg2", 
            ".Rel1Arg1", ".Rel1Rel2", ".Rel1Arg2", ".Arg2Arg1", ".Arg2Rel2", ".Arg2Arg2")

 	def extract(pair:AlignTupleAnno): Array[Double] = {
	    val values = new Array[Double](features.size)
	    
	    values(0) = featureValue(pair.tuple1.arg1Anno, pair.tuple2.arg1Anno)
	    values(1) = featureValue(pair.tuple1.arg1Anno, pair.tuple2.relAnno)
	    values(2) = featureValue(pair.tuple1.arg1Anno, pair.tuple2.arg2Anno)
	    values(3) = featureValue(pair.tuple1.relAnno, pair.tuple2.arg1Anno)
	    values(4) = featureValue(pair.tuple1.relAnno, pair.tuple2.relAnno)
	    values(5) = featureValue(pair.tuple1.relAnno, pair.tuple2.arg2Anno)
	    values(6) = featureValue(pair.tuple1.arg2Anno, pair.tuple2.arg1Anno)
	    values(7) = featureValue(pair.tuple1.arg2Anno, pair.tuple2.relAnno)
	    values(8) = featureValue(pair.tuple1.arg2Anno, pair.tuple2.arg2Anno)
	    
	    return values
    }
    
    def names(): Array[String] = {
        features.map(x => featurePrefix()+x)
    }
    
    // subclasses should implement the following two functions:
    def featurePrefix():String
    
    def featureValue(anno1:FragAnno, anno2:FragAnno): Double

    // optionally implement init()
    def init() {}
}

object TupleFeature {
    
	def enumeratePhrases(tokens:Array[String]):Iterable[String] = {
	    val l = tokens.size
	    for (i <- 0 until l; p <- 1 to Math.min(l-i, 4)) yield tokens.slice(i, i+p).mkString(" ")
	}
}