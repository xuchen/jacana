/**
 *
 */
package edu.jhu.jacana.freebase.questionanalysis

import edu.jhu.jacana.nlp.StanfordCore
import edu.jhu.jacana.align.util.Loggable
import scala.collection.mutable.HashSet
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations
import edu.stanford.nlp.semgraph.SemanticGraphEdge
import scala.collection.JavaConverters._
import edu.stanford.nlp.semgraph.SemanticGraphEdge
import edu.stanford.nlp.ling.IndexedWord
import edu.stanford.nlp.ling.CoreLabel
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.render
import edu.jhu.jacana.nlp.SnowballStemmer
import edu.stanford.nlp.semgraph.SemanticGraph


/**
 * @author Xuchen Yao
 *
 */
class Question (qOrig:String) extends Loggable {
    
    import Question._
    var q = qOrig.toLowerCase()
    var qtokens:Array[String] = null
    var qtokensLowercase:Array[String] = null
    var qtokensLowercaseStemmed:Array[String] = null
    var qlabels:Array[CoreLabel] = null
    
    // a few examples
    // who is john garcia?
    // qword: who, qtype: "", topic: john garcia
    
    // what type of government does japan currently have?
    // qword: what, qtype: government, topic: japan
    
    // what is the name of dawn french's first novel?
    // qword: what, qtype: name, topic: dawn french, candidateTopics: [dawn french, novel, first novel]
    // topic is selected among candidateTopics by choosing the one with the best Freebase hit score
    
    // how old is the current president of north korea
    // qword: how old, qtype: old, topic: President of North Korea
    
    // how many languages are there in the philippines?
    // qword: how many, qtype: "languages", topic: philippines
    
    // which paris airport is closest to the city center?
    // who is the governor of california 2010?
    var qtype:String = ""
 	var qtype_indices:(String,Int,Int) = ("", -1, -1)
    var qword:String = ""
    var qrelation:String = ""
    var qverb:CoreLabel = null
    var qverbIdx = -1
    var qwordIdxStart = 0
    var qwordIdxEnd = 1
    var candidateTopics = new HashSet[(String,String)]()
    var nerIndices: List[(Int,Int,String)] = null
    var nounPhrases:Buffer[String] = null
    var graph:SemanticGraph = null
    
	// contains (multiple) edges to the child (down) of current node
	var edgesDown: Array[HashSet[SemanticGraphEdge]] = null
    
    analyze()
    qtype = qtype_indices._1
    println(String.format("qword:%s\t\tqrelation:%s\t\tqtype:%s\t\tqverb:%s\t\t%s\t\t%s\t\t%s",
            qword, qrelation, qtype, qverb, candidateTopics, nounPhrases, qOrig))
    
    def analyze() {
        // caseless model with NER
		val document = StanfordCore.processQuery(q)
		// val document = StanfordCore.process(q, false)
		val sents = StanfordCore.getSentences(document)
		if (sents.size() > 1) {
		    log.warn("question contains " + sents.size() + " according to Stanford CoreNLP:")
		    log.warn(sents)
		}
		val sent = sents.get(0)
		qtokens = StanfordCore.getTokensInString(sent)
		qtokensLowercase = qtokens.map(x => x.toLowerCase())
		qtokensLowercaseStemmed = qtokensLowercase.map(x => SnowballStemmer.stem(x))
		qlabels = StanfordCore.getTokensInLabels(sent) 

		// find qword
		for (i <- 0 until qtokens.length) {
		    if (qwords.contains(qtokens(i))) {
		        qwordIdxStart = i
		        if (i+1 < qtokens.length && qtokens(i) == "how") {
		            // how long/much/many/old/often ...
		        	qword = qtokens(i) + " " + qtokens(i+1)
		        	qwordIdxEnd = i+2
		        } else {
		        	qword = qtokens(i)
		        	qwordIdxEnd = i+1
		        }
		    }
		}

		if (qword == "") log.warn("can't find a qword from " + qOrig)
		
		// find qtype and topic
		val deps = StanfordCore.getCollapsedCCProcessedDepRelations(sent) 

		graph = StanfordCore.getCollapsedCCProcessedGraph(sent) 
		// contains edge to the parent (up) of current node
		val edgesUp = new Array[SemanticGraphEdge](qtokens.length)
		// contains (multiple) edges to the child (down) of current node
		edgesDown = new Array[HashSet[SemanticGraphEdge]](qtokens.length)
		
		
		// first set qrelation in general cases
		var foundQword = false
		for (edge <- graph.edgeIterable().asScala) {
	        val depId = edge.getTarget().index()-1
	        edgesUp(depId) = edge
	        val govId = edge.getSource().index()-1
	        if (edgesDown(govId) == null)
	        	edgesDown(govId) = new HashSet[SemanticGraphEdge]()
	        edgesDown(govId).add(edge)

	        if (depId == qwordIdxStart) {
	            foundQword = true
	            val govId = edge.getSource().index()-1
	            // println(String.format("%s --%s--> %s", qword, edge.getRelation(), qtokens(govId)))
                // println(String.format("%s\t%s\t--%s-->\t%s", qOrig, qword, edge.getRelation(), qtokens(govId)))
	            qrelation = edge.getRelation().toString()
	        }
		}
		
		//set qverb
		for (w <- graph.getRoots().asScala if w.tag().startsWith("VB")) { 
		    qverbIdx = w.index()-1
		    qverb = qlabels(qverbIdx) 
		}
		if (qverb == null) {
		    for ((label,j) <- qlabels.zipWithIndex if qverb == null) {
		        if (label.tag().startsWith("VB")) {qverb = label; qverbIdx = j}
		    }
		}
		
		if (!foundQword) {
			for (w <- graph.getRoots().asScala) {
			    
                for (e <- edgesDown(w.index()-1) if qverb == null) {
                    // println(String.format("%s --%s--> %s", qword, e.getRelation(), qtokens(e.getDependent().index()-1)))
                    // println(String.format("%s\t%s\t--%s-->\t%s", qOrig, qword, e.getRelation(), qtokens(e.getDependent().index()-1)))
                    if (qlabels(e.getDependent().index()-1).tag().startsWith("VB")) {
                        qverbIdx = e.getDependent().index()-1
                        qverb = qlabels(qverbIdx)
                    }
                }
			    
			    // in the new version of Stanford CoreNLP, the "attr" dep relation
			    // is removed, then a lot of "what be" questions have "what" as the root
			    if(w.index()-1 == qwordIdxStart) {


		            // new relation "cop" introduced in Stanford parser replacing "attr"

		            // the original "attr" is:root ( ROOT-0 , is-2 ) attr ( is-2 , who-1 ) nsubj ( is-2 , kate-3 )
		            // now with "cop": root(ROOT-0, who-1) cop(who-1, is-2) nsubj(who-1, kate-3)
	            	qrelation = "cop"
			    }
			}
		}
		
		
		// more fine-grained analysis
		if (qword == "who") {
		} else if (qword == "when" || qword == "where") {
		} else if (qword.startsWith("how")) {
		    // how old is ...
		    // how many languages ...
		    val qedge = edgesUp(qwordIdxStart) // how
		    val qedgeParent = edgesUp(qedge.getGovernor().index()-1) // old/many in how old/many
		    
		    // there's always an advmod relation between how and * in "how *"
		    // thus we take parent of * as the relation
		    // e.g., how old is --> qrelation = attr (old <--attr-- is)
		    // how many languages --> qrelation = amod (many <--amod-- langauges)
		    qrelation = qedgeParent.getRelation().toString()
		    if (qrelation == "amod") {
		    	val grandparentIdx = qedgeParent.getGovernor().index()-1
		    	// how many languages: many <--amod-- languages
		    	// how many major dialects: many <--amod-- dialects
		    	qtype_indices = getPhraseHeadedBy(qtokens, grandparentIdx, edgesUp, edgesDown, qedgeParent.getDependent().index()-1)
		    } else {
		    	qtype_indices = getPhraseHeadedBy(qtokens, qedge.getGovernor().index()-1, edgesUp, edgesDown, qwordIdxStart)
		    }
		    // println(qword + "\t" + qtype_indices + "\t" + qrelation)
		} else if (qword == "what" || qword == "which") {
		    if (qrelation == "det") {
		        // what awards/percent/time/continent
		        // qtype_indices fits in the general case (parent of what)
		    	qtype_indices = getPhraseHeadedBy(qtokens, edgesUp(qwordIdxStart).getGovernor().index()-1, edgesUp, edgesDown, qwordIdxStart)
		        
		        
		        // two special cases:
		        // what type of political system does russia have?
		        // what kind of economy was the soviet union?
	            // what --det--> type/kind <--prep_of-- system --amod--> political 
		        if (qtype_indices._1.startsWith("kind") || qtype_indices._1.startsWith("type")) {
		        	val qedge = edgesUp(qwordIdxStart) // what
		            for (e <- edgesDown(qedge.getGovernor().index()-1)) {
		            	// kind/type in 'what kind/type'
		                val typeIdx = e.getDependent().index()-1
		                if (typeIdx != qwordIdxStart) {
		                	// get the phrase headed by this word indexed by typeIdx
                            qtype_indices = getPhraseHeadedBy(qtokens, typeIdx, edgesUp, edgesDown)
                            // println(qword + "\t" + qtype_indices + "\t" + qrelation)
		                } 
		            }
		        }
		    } else if (qrelation == "cop") {
		        // what is the name/currency/code/time/ ...
	            for (e <- edgesDown(qwordIdxStart) if e.getRelation().getShortName() != "cop") {
	            	// qtype_indices = getPhraseHeadedBy(qtokens, e.getDependent().index()-1, edgesUp, edgesDown)
	            	qtype_indices = (qtokens(e.getDependent().index()-1), e.getDependent().index()-1, e.getDependent().index())
	            }
		    } else {
		        // dep: what did XX die of? what <-dep-- die
		        // dobj: what does albania speak?
		        // nsubjpass: what is considered eastern canada?
		        qtype_indices = ("", -1, -1)
		    }
		}
		
		// find out the topics
		
		// first the named entities
		val entities = StanfordCore.getNamedEntitiesInString(sent)
		nerIndices = getNamedEntityIndices(entities).toList
		for ((s,e,ner) <- nerIndices) {
		    candidateTopics.add((qtokens.slice(s,e).mkString(" "), ner))
		}
		val it = StanfordCore.getPhrases(sent, "NP").iterator()
		nounPhrases = for (span <- StanfordCore.getPhrases(sent, "NP").asScala) 
		    yield qtokens.slice(span.getSource(), span.getTarget()+1).mkString(" ")
    }
    


    
    def toJson():JObject = {
        val nerTuples = (candidateTopics.map(t => t._1 + " ## " + t._2)).toList
        val npTuples = (nounPhrases.map(n => n + " ## NP")).toList
        val json = ("utterance" -> qOrig) ~ ("topics" -> (nerTuples ++ npTuples))
        // return render(json)
        return json
    }

}

object Question {

    val qwords = HashSet() ++ List("who", "when", "what", "where", "how", "which", "why", "whom", "whose")

    def getNamedEntityIndices(entities: Array[String]):ArrayBuffer[(Int,Int,String)] = {
		var start = 0; var end = 0; var l = entities.length
		val indices = new ArrayBuffer[(Int,Int,String)]()
		for ((e,i) <- entities.zipWithIndex) {
		    if (e != "O" && (i == 0 || entities(i-1) != entities(i)))
		        start = i
		    if (e != "O" && (i == l-1 || entities(i+1) != entities(i))) {
		        end = i;
		        indices += ((start, end+1, entities(i)))
		    }
		}
		return indices
    }

    def getPhraseHeadedBy(qtokens:Array[String], idx:Int, edgesUp:Array[SemanticGraphEdge], 
            edgesDown:Array[HashSet[SemanticGraphEdge]], indexToExclude:Int = -1):(String,Int,Int) = {
        if (edgesDown(idx) == null)
            // no children
            return (qtokens(idx), idx, idx+1)
        
        // if has (potentially multiple) children, then find out whose parent is idx
        val children = for (e <- edgesDown(idx) if indexToExclude != e.getDependent().index()-1) yield e.getDependent().index()-1
        val indices = children.toArray ++ Array(idx)
        val sorted = indices.sorted
        // a rough check
        val min = indices.min; val max = indices.max+1
        return (qtokens.slice(min, max).mkString(" "), min, max)
        
        // the following check doesn't always work for collapsed dependencies (some pp such as 'in' is not indexed)
        // if (last - indices(0) == indices.length-1) {
        //     return qtokens.slice(indices(0), last+1).mkString(" ")
        // } else {
        //     log.warn("head phrase not found: " + q)
        //     log.warn("head idx: " + idx + " head phrase indices: " + indices.toString())
        //     return qtokens(idx)
        // }
    }  
}