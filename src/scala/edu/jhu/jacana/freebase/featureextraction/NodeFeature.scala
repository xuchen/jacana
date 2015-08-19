/**
 *
 */
package edu.jhu.jacana.freebase.featureextraction

import org.jgrapht.graph.DirectedMultigraph
import edu.jhu.jacana.freebase.topicanalysis.ValueType._
import edu.jhu.jacana.freebase.topicanalysis.FreebaseNode
import edu.jhu.jacana.freebase.topicanalysis.FreebaseRelation
import edu.jhu.jacana.freebase.questionanalysis.Question
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.HashSet
import edu.jhu.jacana.freebase.topicanalysis.TopicParser

/**
 * @author Xuchen Yao
 *
 */
object NodeFeature {

    /**
     * very basic features for each node.
     * 
     * two very important features to be done:
     * TODO: alignment scores
     * TODO: leaf object property (mainly types)
     */
    def extract(graph:DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]],
            	node:FreebaseNode, q:Question):Array[String] = {
        val features = new ArrayBuffer[String]()
        val qtext = new HashSet() ++ q.qtokens.map(x => x.toLowerCase())
        
        // TODO: feature combination here
        features += "nodetype=" + node.valuetype
        features += "has_subnodes=" + (node.subnodes.size == 0)
        features += "is_subnode=" + (node.supernode != null)

        val inDegree = graph.inDegreeOf(node)
        val outDegree = graph.outDegreeOf(node)
        features += "is_leaf=" + (outDegree == 0)
        
        features += "node_text_overlap=" + intersection_relation(getTextSetFromNode(node.text), qtext)
        
        if (inDegree == 1) {
            val relation = graph.incomingEdgesOf(node).toArray()(0).asInstanceOf[FreebaseRelation[FreebaseNode]]
            features += "incoming_relation_text_overlap=" + intersection_relation(getTextSetFromRelation(relation.label), qtext)

            // does my parent node has overlap?
            val parentNode = graph.getEdgeSource(relation)
            features += "parent_node_text_overlap=" + intersection_relation(getTextSetFromNode(parentNode.text), qtext)
        }
        if (outDegree > 0) {}
        
        if (outDegree == 0 && inDegree == 0) {
        	// island nodes are subnodes of non-compound node
        	features += "is_island=true"
        	// TODO: mainly dealing with subnodes here.
        	// we should save features for each subnodes, then
        	// after use these features for their supernode
        }
        if (node.supernode != null && graph.inDegreeOf(node.supernode) > 0) {
            val relation = graph.incomingEdgesOf(node.supernode).toArray()(0).asInstanceOf[FreebaseRelation[FreebaseNode]]
            features += "incoming_relation_to_supernode_text_overlap=" + intersection_relation(getTextSetFromRelation(relation.label), qtext)

            // does my supernode's parent node has overlap?
            val parentNode = graph.getEdgeSource(relation)
            features += "parent_node_to_supernode_text_overlap=" + intersection_relation(getTextSetFromNode(parentNode.text), qtext)
        }
        
        if (node.valuetype == OBJECT && node.id.startsWith("/m/")) {
            for (value <- TopicParser.getPropertyValuesFromJson(node.id, "/type/object/type")) {
                features += "object_type=" + value
            }
        }
        
        return features.toArray
    }
    
    def getTextSetFromNode(nodeText:String) = new HashSet() ++ nodeText.toLowerCase().split(" ")

    def getTextSetFromRelation(relationText:String) = 
        // /person/sibling_s converts to: [person, sibling, s]
        new HashSet() ++ FreebaseRelation.splitRelation(relationText)
    
    def intersection_relation(subset:Set[String], superset:Set[String]):String = {
        val overlap = (superset & subset).size
        overlap match {
            case 0 => "no_overlap"
            case x if x == subset.size => "total_subsume"
            case _ => "some_overlap"
        }
    }
}