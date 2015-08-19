/**
 * 
 */
package edu.jhu.jacana.freebase.featureextraction

import edu.jhu.jacana.freebase.topicanalysis.FreebaseNode
import edu.jhu.jacana.freebase.topicanalysis.FreebaseRelation
import edu.jhu.jacana.freebase.questionanalysis.Question
import edu.jhu.jacana.freebase.topicanalysis.TopicParser
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.HashSet
import org.jgrapht.graph.DirectedMultigraph
import scala.collection.JavaConverters._
import edu.jhu.jacana.freebase.questionanalysis.KeywordWebLookup
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.freebase.topicanalysis.ValueType._
import scala.collection.mutable.HashMap
import edu.jhu.jacana.align.util.Loggable
import edu.jhu.jacana.freebase.clueweb.CluewebRuleMapping

/**
 * a feature graph on freebase graph of a certain topic that upon construction 
 * it extracts features for every node in this graph
 * @author Xuchen Yao
 *
 */
class FeatureGraphOnFreebase(graph:DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]],
            	q:Question) extends FeatureGraph with Loggable {
    
    val labelPredicates = new HashSet() ++ List("/type/object/type", "/common/topic/notable_for")
    val node2fnode = new HashMap[FreebaseNode, FeatureNode]()

    init()
    
    def init() {
        // TODO: add surrounding node text overlap (who did portman in star wars?)
        
        val qtext = new HashSet() ++ q.qtokensLowercase
        
        val edge2score = new HashMap[FreebaseRelation[FreebaseNode], Double]()
        for (edge <- graph.edgeSet().asScala) {
        	val score = CluewebRuleMapping.scoreRelationMapping(q.qtokensLowercaseStemmed, edge.label)
        	edge2score += edge -> (if (score == 0.0) Double.MinValue else score )
        }
        val sorted = edge2score.toList.sortBy(t => -t._2)
        val edge2rank = new HashMap[FreebaseRelation[FreebaseNode], Int]()
        for ((edge_score, i) <- sorted.zipWithIndex) {
            edge2rank += edge_score._1 -> (i+1)
        }
        
        // convert every Freebase node into a feature node
        // by extracting features for each
        for (node <- graph.vertexSet().asScala) {
            val fnode = new FeatureNodeOfFreebase(node.text)
            node2fnode += node -> fnode
            fnode.addFeature("nodetype=" + node.valuetype)
            fnode.addFeature("node_text_overlap=" + intersectionRelation(getTextSetFromNode(node.text), qtext))
            fnode.addFeature("has_subnodes=" + (node.subnodes.size == 0))
            fnode.addFeature("is_subnode=" + (node.supernode != null))

            val inDegree = graph.inDegreeOf(node)
            val outDegree = graph.outDegreeOf(node)
            fnode.addFeature("is_leaf=" + (outDegree == 0))
            if (outDegree == 0 && inDegree == 0) {
        	    // island nodes are subnodes of non-compound node
        	    fnode.addFeature("is_island=true")
            }

            if (inDegree == 1) {
                for (relation <- graph.incomingEdgesOf(node).asScala) {
                	fnode.addFeature("incoming_relation_text_overlap=" + intersectionRelation(getTextSetFromRelation(relation.label), qtext))
                	fnode.addFeature("incoming_relation_rank=" + rankOfFeature(edge2rank, relation))

                	// does my parent node has overlap?
                	val parentNode = graph.getEdgeSource(relation)
                	fnode.addFeature("parent_node_text_overlap=" + intersectionRelation(getTextSetFromNode(parentNode.text), qtext))
                }
            }
            
            if (node.supernode != null && graph.inDegreeOf(node.supernode) > 0) {
                for (relation <- graph.incomingEdgesOf(node).asScala) {
                	fnode.addFeature("incoming_relation_to_supernode_text_overlap=" + intersectionRelation(getTextSetFromRelation(relation.label), qtext))
                	fnode.addFeature("incoming_relation_to_supernode_rank=" + rankOfFeature(edge2rank, relation))
           			// does my supernode's parent node has overlap?
                	val parentNode = graph.getEdgeSource(relation)
                	fnode.addFeature("parent_node_to_supernode_text_overlap=" + intersectionRelation(getTextSetFromNode(parentNode.text), qtext))
                }
            }

            if (outDegree > 0) {
            	for (edge <- graph.outgoingEdgesOf(node).asScala) {
            	    val childnode= graph.getEdgeTarget(edge)
            	    if (labelPredicates.contains(edge.label)) {
            	        // this is a property/label
            	        // type/object/type=person, /common/topic/notable_for=pop music, etc
            	        for (subnode <- childnode.subnodes) {
            	        	fnode.addFeature(s"${edge.label}=${subnode.text}")
            	        }
            	    } else {
            	        // this is a relation
            	        fnode.addFeature("out_rel="+edge.label)
            	        fnode.addFeature("out_relation_text_overlap=" + intersectionRelation(getTextSetFromRelation(edge.label), qtext))
            	        fnode.addFeature("out_relation_rank=" + rankOfFeature(edge2rank, edge))
            	    }
            	}
            }

            if (node.valuetype == OBJECT && node.id.startsWith("/m/")) {
                for (value <- TopicParser.getPropertyValuesFromJson(node.id, "/type/object/type")) {
                    fnode.addFeature("/type/object/type=" + value)
                }
            }
            this.addVertex(fnode)
        }
        
        for (edge <- graph.edgeSet().asScala) {
            val fnodeSource = node2fnode(graph.getEdgeSource(edge))
            val fnodeTarget = node2fnode(graph.getEdgeTarget(edge))
            val fedge = new FeatureRelation(fnodeSource, fnodeTarget, edge.label)
            this.addEdge(fnodeSource, fnodeTarget, fedge)
        }
    }
    
    def rankOfFeature(edge2rank: HashMap[FreebaseRelation[FreebaseNode], Int],
            edge: FreebaseRelation[FreebaseNode]):String = {
        edge2rank(edge) match {
            case x if x <= 1 => "top_1"
            case x if x <= 3 => "top_3"
            case x if x <= 5 => "top_5"
            case x if x <= 10 => "top_10"
            case x if x <= 50 => "top_50"
            case x if x <= 100 => "top_100"
            case x if x > 100 => "beyond_top_100"
        }
    }
    

    def getTextSetFromNode(nodeText:String) = new HashSet() ++ nodeText.toLowerCase().split(" ")
    
    def getTextSetFromRelation(relationText:String) = 
        // /person/sibling_s converts to: [person, sibling, s]
        new HashSet() ++ FreebaseRelation.splitRelation(relationText)

    def intersectionRelation(subset:Set[String], superset:Set[String]):String = {
        val overlap = (superset & subset).size
        overlap match {
            case 0 => "no_overlap"
            case x if x == subset.size => "total_subsume"
            case _ => "some_overlap"
        }
    }
    
    def unigramFeatures(node: FreebaseNode):List[String] = {
        if (this.node2fnode.contains(node))
        	return this.node2fnode(node).features.toList
        else {
        	log.warn(s"node $node not found in FeatureGraphOnFreebase")
        	return List[String]()
        }
    }

    def bigramFeatures(node: FreebaseNode):List[String] = {
        if (this.node2fnode.contains(node))
        	return this.node2fnode(node).bigramFeatures
        else {
        	log.warn(s"node $node not found in FeatureGraphOnFreebase")
        	return List[String]()
        }
    }
    
    // final feature set we want to use
    // def getFeatures(node: FreebaseNode):List[String] = unigramFeatures(node) ++ bigramFeatures(node)
    def getFeatures(node: FreebaseNode):List[String] = unigramFeatures(node)
    
}

object FeatureGraphOnFreebase {

    def main(args: Array[String]): Unit = {
        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/small.json"))
        // val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.dev.20.json"))
        var counter = 0
        for (webq <- qlist if counter < 1) {
            counter += 1
            val topic = webq.url.split("\\/").last
            val q = new Question(webq.utterance)
            val fname = FileManager.getFreebaseResource("topic-json/" + topic + ".json.gz")
            val graph = TopicParser.Topic2Graph(fname, topic)
            val (one, all) = TopicParser.setAnswer(graph, webq.answers)
            if (one) {
            	val g = new FeatureGraphOnFreebase(graph, q)
            	g.printUnigramFeaturesPerNode
            	println()
            	g.printBigramFeaturesPerNode
            	println()
            	println("total features: " + g.getFeatures().size)
            	// println("total pointwise features: " + g.pointWiseFeatureCombination().size)
            	println()
            	println()
            }
 
        }
    }
}