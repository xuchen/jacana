/**
 *
 */
package edu.jhu.jacana.validation.ranker

import edu.jhu.jacana.validation.reader.Question
import scala.collection.mutable.HashSet
import edu.jhu.jacana.nlp.StanfordCore
import edu.jhu.jacana.align.util.Loggable
import scala.collection.mutable.HashMap
import weka.core.FastVector
import weka.core.Attribute
import weka.core.Instances
import weka.core.Instance
import weka.classifiers.functions.Logistic
import weka.classifiers.Evaluation
import java.util.Random
import edu.jhu.jacana.validation.feature._

/**
 * @author Xuchen Yao
 *
 */
class AlignmentRanker extends OverlapRanker with Loggable {

    import AlignmentRanker._
    // val features = Array[ValidationFeature](AlignmentDepFeature, AlignmentPosFeature, AlignmentPortionFeature)
    val features = Array[ValidationFeature](AlignmentDepEdgeFeature, AlignmentPosFeature, AlignmentPortionFeature)
    
    def extractFeatures(questionList: List[Question], dataset: Instances,
            			top: Int, posIndex: Int, negIndex: Int) {
 		for ((question,qi) <- questionList.view.zipWithIndex) {
		    val scores = new Array[String](question.options.length)
			val optionList = getOptionContentList(question.options)
			println(question.qid)
		    for ((qos,i) <- question.answers.view.zipWithIndex) {
		        // each QuestionOptionSearch (usually 3 to 5 in total)
		        val dep2count = new HashMap[String, Integer]()
		        val queryTokenized = qos.query_tokenized
		        val optionSet = optionList(i)
		        val doc = StanfordCore.processWithSpaceTokenizer(queryTokenized)
		        val sents = StanfordCore.getSentences(doc)
		        if (sents.size() > 1) {
		            log.warn("question sentence is split into more than one!")
		            log.warn(queryTokenized)
		            log.warn(sents)
		        }
		        val num = Math.min(top, qos.snippets.length)
		        var values = new Array[Double](0)
		        for (feature <- features) {
		        	values ++= feature.extract(sents.get(0), qos.snippets.slice(0, num), optionList(i))
		            
		        }
		        if (question.answer == i)
		            values ++= Array[Double](posIndex.toDouble)
		        else
		            values ++= Array[Double](negIndex.toDouble)
		        val inst = new Instance(1.0, values)
		        dataset.add(inst)
		    }
		}       
    }

    override def rank(trainList: List[Question], devList: List[Question], top: Int) {

		// feature name declaration in Weka
		
		val attributes = new FastVector();
		for (feature <- features) {
		    for (feat <- feature.features)
		    	attributes.addElement(new Attribute(feat))
		}
		val labels = new FastVector();
		labels.addElement("positive");
		labels.addElement("negative");
		val cls = new Attribute("class", labels);
		val positiveIndex = cls.indexOfValue("positive")
		val negativeIndex = cls.indexOfValue("negative")
		attributes.addElement(cls);
		// *4: there are 4 options for each question
		val trainSet = new Instances("Answer Validation Train", attributes, trainList.length*4);
		val devSet = new Instances("Answer Validation Dev", attributes, devList.length*4);
		// Make the last attribute be the class
		trainSet.setClassIndex(trainSet.numAttributes()-1); 
		devSet.setClassIndex(devSet.numAttributes()-1); 
		
		extractFeatures(trainList, trainSet, top, positiveIndex, negativeIndex)
		extractFeatures(devList, devSet, top, positiveIndex, negativeIndex)
		
		// training
		val classifier = new Logistic()
		classifier.buildClassifier(trainSet)
		
		val weights = classifier.coefficients()
		println(classifier.toString())
		// printFeatureWeights(weights, attributes, positiveIndex)
		
		// testing on dev
		var insIdx = 0
		for ((question,qi) <- devList.view.zipWithIndex) {
            val scores = new Array[String](question.options.length)
		    for (i <- 0 until question.options.length) {
		    	val scoresPerLabel = classifier.distributionForInstance(devSet.instance(insIdx))
		    	insIdx += 1
		    	scores(i) = scoresPerLabel(positiveIndex).toString()
		    }
        	question.scores = Some(scores.toList)
		}

		// cross validation
		// val eval = new Evaluation(dataset)
		// eval.crossValidateModel(classifier, dataset, 10, new Random(1))
		// println(eval.toSummaryString("\nResults\n\n", false))
		
    }
    
    // not working since the logistic regression model doesn't use all the features
    // (weights.size < attributes.size)
    // http://stackoverflow.com/questions/17248830/coefficients-mapping-in-weka-logistic-classifier
    @deprecated
    def printFeatureWeights(weights: Array[Array[Double]], attributes:FastVector, positiveIndex:Int) {
        var i = 0
        val size = attributes.size()
        println(size)
        println(weights.length)
        println(weights(0).length)
        while (i < size - 1) {
            println(attributes.elementAt(i).asInstanceOf[Attribute].name() + ":\t" + weights(i)(positiveIndex))
            i += 1
        }
    }
}

object AlignmentRanker {
    /**
     * check whether the snippet contains any of the option content word
     */
    def containsOption(snippet:String, optionSet:HashSet[String]): Boolean = {
        optionSet.foreach{o =>
            val r = o.r
            r.findAllIn(snippet.toLowerCase()).length
            r.findFirstIn(snippet.toLowerCase()) match {
                case Some(x) => return true
                case _ =>
            }
        }
        return false
    }

    def getAlignedQueryIndices(dashedAlign:String, queryIsSource:Boolean = false):HashSet[Integer] = {
        val set = new HashSet[Integer]()
        dashedAlign.split("\\s+").foreach {align =>
            val Array(src, tgt) = align.split("-")
            if (queryIsSource)
                set += src.toInt
            else
                set += tgt.toInt
        }
        return set
    }
    
 
    def main(args: Array[String]): Unit = {}
}