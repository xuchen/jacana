/**
 *
 */
package edu.jhu.jacana.freebase.topicanalysis

import org.jgrapht.graph.DefaultEdge
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

import org.jgrapht.graph.DirectedMultigraph

/**
 * @author Xuchen Yao
 *
 */
class FreebaseRelation[V](v1:V, v2:V, val label:String) extends DefaultEdge {
    def toShortString():String = {
        if (label.length() > 20)
            return label.substring(20) + "..."
        else
            return label
    }
    override def toString():String = {return label+"=("+v1+", "+v2+")"}
}

object FreebaseRelation {
    def splitRelation(label:String) = label.toLowerCase().split("[/_.]").filter(x => x!="")
}

object ValueType extends Enumeration {
    type ValueType = Value
    val FLOAT = Value("float") 
    val INT = Value("int") 
    val BOOL = Value("bool")
    val DATETIME = Value("datetime")
    val URI = Value("uri")
    val STRING = Value("string")
    val OBJECT = Value("object")
    val COMPOUND = Value("compound")
    val KEY = Value("key")
    val ROOT = Value("root")
}

import ValueType._

/**
 * A node in the Freebase Graph. There are in general three types of nodes:
 * 1. arg1(root) node, the topic of interest
 * 2. arg2(non-compound) node, with literal value type or object; each node has one or more subnodes.
 * 		but all subnodes share the same property from the root node to this node
 * 3. arg2(compound) node, with "compound" value type; each node usually has multiple subnode,
 * 		each subnode has its own relation with arg2
 *   
 * Thus the general structure looks like:
 * FreebaseRelation (root node, non-compound arg2 node) ...
 * FreebaseRelation (root node, compound arg2 node) ...
 * FreebaseRelation (compound arg2 node, subnodes of arg2) ...
 * 
 * for instance:
 * /people/person/parents (justin bieber, non-compound arg2 node)
 * the subnodes of non-compound arg2 node are: "Pattie Mallette" and "Jeremy Bieber" (each is an object)
 * 
 * /people/person/sibling_s (justin bieber, compound arg2 node)
 * compound arg2 node contains two subnodes, each of which forms another relation with arg2:
 * /people/sibling_relationship/sibling (compound arg2 node, "Jazmyn Bieber")
 * /people/sibling_relationship/sibling (compound arg2 node, "Jaxson Bieber")
 * 
 * if the question is: who are the parents of justin bieber?
 * then the non-compound arg2 node and all its subnodes are marked as the answer
 * 
 * if the question is: who is the brother of justin bieber?
 * then the male subnode (Jaxson Bieber) of compound arg2 node is marked as answer
 */
class FreebaseNode(var text:String, var id:String, val valuetype:ValueType, var isAnswer:Boolean = false) {
    val subnodes = new ArrayBuffer[FreebaseNode]()
    
    // if this node has supernode, then it's its supernode's subnode
    var supernode: FreebaseNode = null
    
    def addNode(node:FreebaseNode) {subnodes.append(node); supernode = node}

    def toShortString():String = {
        if (text.length() > 40)
            return text.substring(0, 40) + "..."
        else
            return text
    }

    override def toString():String = {
        if (text == "" && subnodes.size == 1)
            return subnodes(0).text+"/"+valuetype.toString()
        else
        	return toShortString()+"/"+valuetype.toString()
    }
}

object FreebaseNode {
    
    def getIncomingRelationFromNode(graph:DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]],
            node:FreebaseNode): Array[String] = {

        val buf = new ArrayBuffer[String]()
        val edges = graph.incomingEdgesOf(node)
        if (edges != null && edges.size() > 0) {
            for (edge <- edges.asScala) {
                buf += edge.label
            }
        }
        return buf.toArray
    }
    
    def getIncomingRelationSplitsFromNode(graph:DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]],
            node:FreebaseNode): Array[String] = {

        val buf = new ArrayBuffer[String]()
        val edges = graph.incomingEdgesOf(node)
        if (edges != null && edges.size() > 0) {
            for (edge <- edges.asScala) {
                buf ++= FreebaseRelation.splitRelation(edge.label)
            }
        }
        return buf.toArray
    }
}
