/**
 *
 */
package edu.jhu.jacana.freebase.topicanalysis

import net.liftweb.json._
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.freebase.questionanalysis.KeywordWebLookup
import org.jgrapht._
import org.jgrapht.graph._
import ValueType._
import scala.collection.JavaConverters._
import scala.collection.immutable.HashSet
import scala.collection.mutable.ArrayBuffer

/**
 * a class for parsing the Freebase topic json files retrieved from the Freebase Topi API
 * (https://developers.google.com/freebase/v1/topic-overview)
 * @author Xuchen Yao
 *
 */
object TopicParser {

    implicit val formats = DefaultFormats // Brings in default date formats etc.

    val dummyRelations = new HashSet[String]() ++ Array("/type/object/attribution", "/type/object/type")
    
    def parseProperty(property: Map[String, JValue], indent: String = "") {
    	property.foreach { case (prop, arg2) =>
            println(indent)
            println(indent+prop)
            for (jarg2 <- arg2.extractOpt[Arg2]) {
                println(indent+jarg2.valuetype)
                jarg2.valuetype match {
                   case "string" | "float" | "bool" | "datetime" | "uri" | "int" | "key" => 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Literal] match { 
                               // value has more info than text, such as in description
                               case Some(str) => println(indent+str.value)
                               case _ =>}
                           }
                   case "object"=> 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Object] match { 
                               case Some(str) => println(indent+str.text)
                               case _ =>}
                           }
                   case "compound"=> 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Compound] match { 
                               case Some(jarg22) => println(indent+jarg22.text);
                               	// recursively parsing arg2 property
                               	parseProperty(jarg22.property, indent+"\t")
                               case _ =>}
                           }
                   case _ =>
                }
            }
            case _ =>
        }

    }
    
    def parsePropertyToGraph(property: Map[String, JValue], indent:String, 
            				graph:DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]], 
            				root:FreebaseNode) {
        graph.addVertex(root)
    	property.foreach { case (prop, arg2) if prop != "/common/topic/description" 
    	    			&& prop != "/common/document/text" && prop != "/common/topic/article" =>
            // println(indent)
            // println(indent+prop)
            for (jarg2 <- arg2.extractOpt[Arg2]) {
                // println(indent+jarg2.valuetype)

                // a 'pseudo'-node that is the arg2 of the root node, but arg1 of its own children node (in 'values')
                val arg2Node = new FreebaseNode("", "", ValueType.withName(jarg2.valuetype))
                
                // defer adding arg2Node and relation to the graph until after matching (IMPORTANT),
                // as we might modify arg2Node during matching
                jarg2.valuetype match {
                   case "string" | "float" | "bool" | "datetime" | "uri" | "int" | "key" => 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Literal] match { 
                               // value has more info than text, such as in description
                               case Some(str) => // println(indent+str.value)
                            		    if (jarg2.values.size == 1) {
                            		        // directly replace arg2Node with this since it would only have had one child
                            		        arg2Node.text = str.value
                            		    } else {
                            		    	val arg2Subnode = new FreebaseNode(str.value, "", ValueType.withName(jarg2.valuetype))
                            		    	// these arg2Subnode's are islands of their own in the graph.
                               				// they are connected through ndoe.subnodes, but not through edges between nodes in the graph
                               				graph.addVertex(arg2Subnode)
                               				arg2Node.addNode(arg2Subnode)
                            		    }
                               case _ =>}
                           }
                   case "object"=> 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Object] match { 
                               case Some(str) => // println(indent+str.text)
                            		    if (jarg2.values.size == 1) {
                            		        // directly replace arg2Node with this since it would only have had one child
                            		        arg2Node.text = str.text
                            		        arg2Node.id = str.id
                            		    } else {
                            		    	val arg2Subnode = new FreebaseNode(str.text, str.id, ValueType.withName(jarg2.valuetype))
                               				// these arg2Subnode's are islands of their own in the graph.
                               				// they are connected through ndoe.subnodes, but not through edges between nodes in the graph
                               				graph.addVertex(arg2Subnode)
                               				arg2Node.addNode(arg2Subnode)
                            		    }
                               case _ =>}
                           }
                   case "compound"=> 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Compound] match { 
                               case Some(jarg22) => // println(indent+jarg22.text);
                            		    if (jarg2.values.size == 1) {
                            		        // directly replace arg2Node with this since it would only have had one child
                            		        arg2Node.text = jarg22.text
                            		        arg2Node.id = jarg22.id
                                        	parsePropertyToGraph(jarg22.property, indent+"\t", graph, arg2Node)
                            		    } else {
                            		    	val arg2Subnode = new FreebaseNode(jarg22.text, jarg22.id, ValueType.withName(jarg2.valuetype))
                                        	arg2Node.addNode(arg2Subnode)
                               				// these arg2Subnode's are NOT islands of their own in the graph.
                               				// they are connected through both ndoe.subnodes and edges (property) between nodes in the graph
                                        	// graph.addVertex(arg2Subnode)
                                        	// recursively parsing arg2 property
                                        	parsePropertyToGraph(jarg22.property, indent+"\t", graph, arg2Subnode)
                            		    }
                               case _ =>}
                           }
                   case _ =>
                }
                graph.addVertex(arg2Node)
                val rel = new FreebaseRelation[FreebaseNode](root, arg2Node, prop)
                graph.addEdge(root, arg2Node, rel)

                // println(indent+rel)
            }
            case _ =>
        }

    }

    def Topic2Tuple(fname:String) {
        val text = FileManager.readFile(fname)

        val jtopic = parse(text).extract[Topic]
        println(jtopic.id)
        parseProperty(jtopic.property)
    }

    def Topic2Graph(fname:String, topic:String):DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]] = {
        val text = FileManager.readFile(fname)

        val jtopic = parse(text).extract[Topic]
        println(jtopic.id + " " + topic)
        val graph = new DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]](
                new ClassBasedEdgeFactory[FreebaseNode, FreebaseRelation[FreebaseNode]](classOf[FreebaseRelation[FreebaseNode]]))
        val rootNode = new FreebaseNode(topic, "", ROOT)
        parsePropertyToGraph(jtopic.property, "", graph, rootNode)
        // println(graph.edgeSet())
        return graph
    }
    
    def setAnswer(graph: DirectedMultigraph[FreebaseNode, FreebaseRelation[FreebaseNode]],
            	  answers:Array[String]): (Boolean, Boolean) = {
        val answerSet = new HashSet() ++ answers
        println("ANSWERS: " + answerSet)
        val answerSize = answerSet.size
        var one_answer_is_set = false
        var all_answers_are_set = false
        for (node <- graph.vertexSet().asScala ) { 
        	if (answerSet.contains(node.text)) {
	            one_answer_is_set = true; node.isAnswer = true
	            println("ANSWER NODE FOUND: " + node)
        	}
        }
        
        // deal with compound nodes, which always have more than one hop of relations
        for (node <- graph.vertexSet().asScala if node.valuetype == COMPOUND && !node.isAnswer) {
            var nodes = if (node.subnodes.size != 0) node.subnodes.toArray  else Array(node)
            for (sub <- nodes if !sub.isAnswer) {
            	val outEdges = graph.outgoingEdgesOf(sub)
            	val edge = getNondummyRelation(outEdges)
            	if (edge != null) {
            		val childNode = graph.getEdgeTarget(edge)
            		if (childNode.isAnswer) {
            		    sub.isAnswer = true
            		    println("TOP COMPOUND ANSWER NODE FOUND: " + sub)
            		}
            	}                   
            }
        }

        // when answerSet has more than one answer, say, in question:
        // "who are justin bieber's parents?" there are two answers: Pattie and Jeremy
        // we want to set both the nodes of Pattie and Jeremy to isAnswer = true
        // AND the parent of Pattie Jeremy, which is a "/people/person/parents" node AND
        // contains only the answer nodes, to isAnswer = true
        // finally we store those unique true answer nodes (both parents and children)
        val uniqueAnswerNodes = new scala.collection.mutable.HashSet[FreebaseNode]()
        for (node <- graph.vertexSet().asScala ) { 
            var all_subnodes_are_answers = true && node.subnodes.size > 1
            for (sub <- node.subnodes if !sub.isAnswer && all_subnodes_are_answers) all_subnodes_are_answers = false
            if (all_subnodes_are_answers) {
                node.isAnswer = true
                println("TOP ANSWER NODE FOUND: " + node)
                if (node.subnodes.size == answerSize) {
                    uniqueAnswerNodes += node
                    all_answers_are_set = true
                    for (sub <- node.subnodes) {
                        uniqueAnswerNodes += sub
                        // go down one more level for compound edges
                        if (sub.valuetype == COMPOUND) {
                        	for( subedge <- graph.outgoingEdgesOf(sub).asScala) {
                        		val subsub = graph.getEdgeTarget(subedge)
                           		if (subsub.isAnswer) uniqueAnswerNodes += subsub
                        	}
                        }
                    }
                }
            }
        }

        // following the above example, Pattie also shows up in another relation: /base/popstra/celebrity/supporter
        // and it was (incorrectly) set to isAnswer = true since we didn't find the answer Jeremy here.
        // thus we unset Pattie as the answer. This only (sort of) works only when there is more than one answer.
        // if there is only one answer, we have to blindly map every node whose's text matches the answer
        if (answerSize > 1 && uniqueAnswerNodes.size > 0) {
        	for (node <- graph.vertexSet().asScala if !uniqueAnswerNodes.contains(node) && node.isAnswer) {
       	        node.isAnswer = false
       	        println("ANSWER NODE UNSET: " + node)
        	}
        }
        
        if (one_answer_is_set == true && answerSize == 1)
            all_answers_are_set = true
        return (one_answer_is_set, all_answers_are_set)
    }
    
/** 
 *  there are a lot of simple compound nodes with three properties:
 *  /type/object/attribution, /type/object/type, and /something/matters
 *  we treat the first two as dummy relations and mark the compound node
 * as answer if /something/matters->node contains the answer
 */
    def getNondummyRelation(edges: java.util.Set[FreebaseRelation[FreebaseNode]]):FreebaseRelation[FreebaseNode] = {
        var relation: FreebaseRelation[FreebaseNode]  = null
        var found = true
        if (edges.size != dummyRelations.size +1) return null
        for (e <- edges.asScala) {
            if (!dummyRelations.contains(e.label)) {
                if (relation == null) relation = e
                else return null
            }
            
        }
        return relation
    }
    
    // usually used to get the "/type/object/type" values from a json.gz file
    // topic_id looks like /m/0klsdf
    def getPropertyValuesFromJson(topic_id:String, property:String): List[String] = {
        // convert /m/0klsdf to m.0klsdf
        val splits = topic_id.split("/").filter(x => x != "")
        var fid = ""
        splits(0) match {
            case "m" => fid = splits.mkString(".")
            case "en" => fid = splits(1)
            case _ => println(s"can't understand topic id: $topic_id"); return List[String]();
        }
    	val fname = FileManager.getFreebaseResource(s"topic-json/$fid.json.gz", false)
    	if (!FileManager.fileExists(fname)) return List[String]()
        val text = FileManager.readFile(fname)
        if (text.contains("\"error\":") && text.contains("\"code\":")) {
            println(s"warning: $fid not successfully downloaded") 
            return List[String]()
        }

        val jtopic = parse(text).extract[Topic]
        val buffer = new ArrayBuffer[String]()
        jtopic.property.foreach { case (prop, arg2) if prop == property =>
            for (jarg2 <- arg2.extractOpt[Arg2]) {

                jarg2.valuetype match {
                   case "string" | "float" | "bool" | "datetime" | "uri" | "int" | "key" => 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Literal] match { 
                               case Some(str) =>
                                   buffer += str.value
                               case _ =>}
                           }
                   case "object"=> 
                       for (arg2value <- jarg2.values) {
                           arg2value.extractOpt[Arg2Object] match { 
                               case Some(str) => // println(indent+str.text)
                                   buffer += str.text
                               case _ =>}
                           }
                   case _ =>
                }
            }
            case _ =>
        }
        
        return buffer.toList
    }

    def main(args: Array[String]): Unit = {
        // Topic2Graph("/tmp/justin_bieber.json", "justin bieber")
        // Topic2Graph(FileManager.getFreebaseResource("topic-json/justin_bieber.json.gz"), "justin bieber")

        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.train.json"))
        // val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/small.json"))
        var counter = 0
        var cOneAnswer = 0; var cAllAnswer = 0;
        var noAnswers = new HashSet[String]()
        for (q <- qlist /*if counter < 1*/) {
            counter += 1
            // println(q.answers.mkString(" "))
            val topic = q.url.split("\\/").last
            val fname = FileManager.getFreebaseResource("topic-json/" + topic + ".json.gz")
            val graph = Topic2Graph(fname, topic)
            val (one, all) = setAnswer(graph, q.answers)
            if (one) cOneAnswer += 1
            else noAnswers += q.utterance
            if (all) cAllAnswer += 1
            println()

        }
        
        println(f"perfect matching of answers: $cAllAnswer/$counter (${cAllAnswer*1.0/counter}%.3f)")
        var cPartialAnswer = cOneAnswer - cAllAnswer
        println(f"partial matching of answers: $cPartialAnswer/$counter (${cPartialAnswer*1.0/counter}%.3f)")
        println(f"no answers ${noAnswers.size}/$counter (${noAnswers.size*1.0/counter}%.3f) found:")
        for (a <- noAnswers)
        	println(a)
            /*
             * log: 
             * perfect matching of answers: 3110/3778 (0.823)
             * partial matching of answers: 524/3778 (0.139)
             * no answers 144/3778 (0.038) found:
             * where did william shakespeare perform most of his plays?
             * where is ann romney from?
             * ...
             */
    }

}


// literals include float, int, bool, datetime, uri, or string, or key
// https://developers.google.com/freebase/v1/topic-response
case class Arg2Literal(text:String, lang:String, value:String, creator:Option[String], timestamp:Option[String])

// object has id, that refers to other objects
case class Arg2Object(text:String, lang:String, id:String, creator:Option[String], timestamp:Option[String])

// compound basically is another Arg2 that needs to be parsed recursively
case class Arg2Compound(text:String, lang:String, id:String, property: Map[String, JValue], creator:Option[String], timestamp:Option[String])

case class Arg2(valuetype: String, values: List[JValue], count: Double, status:Option[String])

// JValue is mostly of type Arg2, but there are exceptions (have to parseOpt)
case class Topic(id: String, property: Map[String, JValue])
