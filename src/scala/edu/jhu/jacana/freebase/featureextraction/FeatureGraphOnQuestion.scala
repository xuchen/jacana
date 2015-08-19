/**
 * 
 */
package edu.jhu.jacana.freebase.featureextraction

import edu.jhu.jacana.freebase.questionanalysis.Question
import java.util.Set
import org.jgrapht.EdgeFactory
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
import edu.jhu.jacana.nlp.SnowballStemmer
import edu.stanford.nlp.ling.Label
import edu.jhu.jacana.freebase.questionanalysis.KeywordWebLookup
import edu.jhu.jacana.util.FileManager
import org.jgrapht.graph.ClassBasedEdgeFactory

/**
 * a feature graph on questions that upon construction it extracts features for this question
 * @author Xuchen Yao
 *
 */
class FeatureGraphOnQuestion(question: Question) extends FeatureGraph {
    var nodes: Array[FeatureNode] = null
    init()
    
    def init() {

        val dummyPos = new HashSet() ++ List("DT", ".", "?", "IN", "TO")
		nodes = new Array[FeatureNode](question.qtokens.size)
		for ((label, i) <- question.qlabels.zipWithIndex) {
		    val node = new FeatureNodeOfQuestion(question.qtokensLowercaseStemmed(i))
		    // node.addFeature("pos="+label.tag())
		    // if (label.ner() != "O") node.addFeature("ner="+label.ner())
		    nodes(i) = node
		    if (!dummyPos.contains(label.tag()) && !(question.edgesDown(i) != null && 
		            question.edgesDown(i).size == 0 
		            ) ) {
		    	// don't add every node, removing punctuation and determinor
		        // println(label+" "+label.tag())
		    	this.addVertex(node)
		    } else {
		        // println("not adding node " + node.text)
		    }
		}

		if (question.qword != "") {
		    for (i <- question.qwordIdxStart until question.qwordIdxEnd) {
		        nodes(i).addFeature("qword=" + question.qword)
		        // nodes(i).addFeature("qrelation=" + question.qrelation)
		    }
		}
		
		if (question.qverbIdx != -1) {
		    nodes(question.qverbIdx).addFeature("qverb="+question.qlabels(question.qverbIdx).lemma()) 
		    nodes(question.qverbIdx).addFeature("qverb_tag="+question.qlabels(question.qverbIdx).tag()) 
		}
		
		for (edge <- question.graph.edgeIterable().asScala) {
	        val depId = edge.getTarget().index()-1
	        val govId = edge.getSource().index()-1

	        val relName = edge.getRelation().toString()
	        val rel = new FeatureRelation(nodes(govId), nodes(depId), relName)
	        if (this.containsVertex(nodes(govId)) &&  this.containsVertex(nodes(depId)))
	        	this.addEdge(nodes(govId), nodes(depId), rel)
	        // nodes(govId).addFeature("dep_out="+relName)
	        // nodes(depId).addFeature("dep_in="+relName)
		}
		
		for (i <- question.qtype_indices._2 until question.qtype_indices._3) {
		    nodes(i).addFeature("qfocus="+question.qtype_indices._1)
		}

		for ((s,e,ner) <- question.nerIndices) {
		    for (i <- s until e) {
		        nodes(i).addFeature("qtopic_ner="+ner)
		    }
		}
		
		// for all other nodes: if no features added so far, 
		// add the node text itself as the lexical feature
		for (node <- this.vertexSet().asScala if node.features.size == 0) {
		    node.addFeature(node.text)
		}
		
    }
    

}

object FeatureGraphOnQuestion {

    def main(args: Array[String]): Unit = {
        val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/small.json"))
        // val qlist = KeywordWebLookup.readQuestionJson(FileManager.getResource("freebase-data/webquestions/webquestions.examples.dev.20.json"))
        var counter = 0
        for (webq <- qlist if counter < 10) {
            counter += 1
            val q = new Question(webq.utterance)
            val g = new FeatureGraphOnQuestion(q)
            g.printUnigramFeaturesPerNode
            println()
            // g.printBigramFeaturesPerNode
            println()
            val ff = g.getFeatures()
            println("total features: " + ff)
            println("total features size: " + ff.size)
            // println("total pointwise features: " + g.pointWiseFeatureCombination().size)
            println()
            println()
 
        }
    }
}