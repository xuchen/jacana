package edu.jhu.jacana.align.tuple.feature

import edu.jhu.jacana.align.tuple.reverb.AlignedTuple
import scala.collection.mutable.HashSet
import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import edu.jhu.jacana.align.tuple.reverb.FragAnno

object OverlapTupleFeature extends TupleFeature {

	val splitSeparator = "[ -]".r

    override def featurePrefix() = "overlap"

    override def featureValue(anno1:FragAnno, anno2:FragAnno): Double = {
	    normalizedOverlap(anno1.string, anno2.string)
	}

	def normalizedOverlap(s1:String, s2:String): Double = {
	    val s1s = new HashSet() ++ splitSeparator.split(s1.toLowerCase())
	    val s2s = new HashSet() ++ splitSeparator.split(s2.toLowerCase())
	    val intersect = s1s & s2s
	    // return intersect.size*1.0/Math.min(s1s.size, s2s.size)
	    return intersect.size*1.0/(s1s.size + s2s.size)
	}
}