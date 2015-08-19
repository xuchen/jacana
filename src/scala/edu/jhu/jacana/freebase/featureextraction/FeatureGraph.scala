
/**
 *
 */
package edu.jhu.jacana.freebase.featureextraction

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph
import org.jgrapht.EdgeFactory
import org.jgrapht.graph.ClassBasedEdgeFactory
import scala.collection.mutable.HashSet

/**
 * a high level graph with each node labeled with features, for
 * feature extraction in an enumerated fashion later.
 * this class supports feature extraction and combination on
 * unigram, bigram features, where "bigram" features are
 * pair-wisely combined from unigram features per node pair or path. 
 * @author Xuchen Yao
 *
 */
abstract class FeatureGraph[V <: FeatureNode, E <: FeatureRelation[FeatureNode]](
        ef: EdgeFactory[V, E] = new ClassBasedEdgeFactory[FeatureNode, FeatureRelation[FeatureNode]](classOf[FeatureRelation[FeatureNode]]))
				extends DirectedMultigraph(ef) {
    def unigramFeatures():List[String] = {
        this.vertexSet().asScala.toList.foldLeft(List[String]())((r,c) => r ++ c.features)
    }

    def bigramFeatures():List[String] = {
        this.vertexSet().asScala.toList.foldLeft(List[String]())((r,c) => r ++ c.bigramFeatures)
    }
    
    // final feature set we want to use
    def getFeatures():List[String] = {
        val adjacentFeatures = new HashSet[String]
        for (edge <- this.edgeSet().asScala) {
            adjacentFeatures ++= adjacentFeatureWithEdge(edge)
        }

        return (adjacentFeatures ++ unigramFeatures()).toList
    }
    
    // for edge, get the following features:
    // source-target, source-edge_label-target
    def adjacentFeatureWithEdge(edge: E): List[String] = {
        val s = this.getEdgeSource(edge)
        val t = this.getEdgeTarget(edge)
        val feats = new ArrayBuffer[String]
        for (f1 <- s.features) {
            for (f2 <- t.features) {
                // only combine, don't comineSort here
                // as there is a direction between f1->f2
                feats += FeatureUtils.combine(f1, f2)
                feats += FeatureUtils.combine(f1, edge.text, f2)
            }
        }
        return feats.toList
    }
    
    // WARNING: produces too long a list (usually > 10,000)
    def pointWiseFeatureCombination():List[String] = {
        val features = unigramFeatures() ++ bigramFeatures()
        val f = new ArrayBuffer[String]
        for (f1 <- features) 
            for (f2 <- features if f2 != f1) 
                f += FeatureUtils.combine(f1, f2)
        return f.toList
    }
 
    def printUnigramFeaturesPerNode() {
        for (node <- this.vertexSet().asScala)
            println(node.text + " ==> " + node.features)
    }
    
    def printBigramFeaturesPerNode() {
        for (node <- this.vertexSet().asScala)
            println(node.text + " ==> " + node.bigramFeatures)
    }
}


/**
 * A node with labels as features. The node itself can also be a feature.
 * 
 */
abstract class FeatureNode (var text:String, nodeIsFeature:Boolean) {
    
    var features = if (nodeIsFeature) HashSet[String](text) else new HashSet[String]
    def addFeature(label:String) {features += label}
    
    def bigramFeatures(): List[String] = {
        val f = new ArrayBuffer[String]
        for (f1 <- features) 
            for (f2 <- features if f2 != f1) 
                f += FeatureUtils.combineSort(f1, f2)
        return f.toList
    }
}

class FeatureNodeOfQuestion (text:String) extends FeatureNode(text, false)
class FeatureNodeOfFreebase (text:String) extends FeatureNode(text, false)

class FeatureRelation[FeatureNode](v1:FeatureNode, v2:FeatureNode, val text:String) extends DefaultEdge {
// class FeatureRelation[V](v1:V, v2:V, val text:String) extends DefaultEdge 
    def toShortString():String = {
        if (text.length() > 20)
            return text.substring(20) + "..."
        else
            return text
    }
    override def toString():String = {return text+"=("+v1+", "+v2+")"}
}

object FeatureUtils {
    @inline def combineSort(f1:String, f2:String): String = {
        if (f2 > f1) f1+"|"+f2 else f2+"|"+f1
    }

    @inline def combineSort(f1:String, mid:String, f2:String): String = {
        if (f2 > f1) f1+"|"+mid+"|"+f2 else f2+"|"+mid+"|"+f1
    }
    @inline def combine(f1:String, f2:String): String = f1+"|"+f2
    @inline def combine(f1:String, mid:String, f2:String): String = f1+"|"+mid+"|"+f2
}