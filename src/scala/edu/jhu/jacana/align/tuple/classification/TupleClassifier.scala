/**
 *
 */
package edu.jhu.jacana.align.tuple.classification

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
import edu.jhu.jacana.align.tuple.feature._
import edu.jhu.jacana.align.tuple.reverb.AlignedTuple
import edu.jhu.jacana.align.tuple.reverb.Tuple
import edu.jhu.jacana.align.tuple.reverb.AlignTupleReader
import edu.jhu.jacana.align.tuple.reverb.AlignTupleAnno
import weka.classifiers.functions.SMO
import weka.classifiers.functions.VotedPerceptron
import weka.classifiers.Classifier
/**
 * @author Xuchen Yao
 *
 */
class TupleClassifier(modelName:String = null) {

    val features = Array[TupleFeature](WordnetTupleFeature, PPDBTupleFeature, WiktionaryTupleFeature, WordnetSimilarityTupleFeature, StringSimilarityTupleFeature)//, OverlapTupleFeature) //, Word2VecTupleFeature )
    //val features = Array[TupleFeature](OverlapTupleFeature)
    
    for (f <- features) f.init()
    
    var classifier:Logistic = if (modelName == null) new Logistic() else (weka.core.SerializationHelper.read(modelName)).asInstanceOf[Logistic]
    
	val labels = new FastVector();
	labels.addElement("positive");
	labels.addElement("negative");
	val cls = new Attribute("class", labels);
	val positiveIndex = cls.indexOfValue("positive")
	val negativeIndex = cls.indexOfValue("negative")
    
    def extractFeatures(tupleList: List[AlignedTuple], dataset: Instances) {

        val tupleAnnoList = for (tuple <- tupleList) yield new AlignTupleAnno(tuple)

        for (tuple <- tupleAnnoList) {
            
	        var values = new Array[Double](0)
            for (feature <- features) {
                values ++= feature.extract(tuple)
            }
	        if (tuple.align)
	           	values ++= Array[Double](positiveIndex)
	       	else
	       		values ++= Array[Double](negativeIndex)
	        val inst = new Instance(1.0, values)
	        dataset.add(inst)
        }
    }
    
    def extractFeatures(tuple: AlignedTuple): Instance = {
        val tupleAnno = new AlignTupleAnno(tuple)
            
        var values = new Array[Double](0)
        for (feature <- features) {
            values ++= feature.extract(tupleAnno)
        }
        if (tuple.align)
           	values ++= Array[Double](positiveIndex)
       	else
       		values ++= Array[Double](negativeIndex)
        return new Instance(1.0, values)
    }
    
    /**
     * Align two tuples in  the form of string triples (arg1, rel, arg2).
     * Return the confidence score [0, 1] and whether it's cross alignment.
     */
    def getAlignScoreAndIsCross(t1:(String,String,String), t2:(String,String,String)): (Double, Boolean) = {
        val t1t = new Tuple(t1._1, t1._2, t1._3)
        val t2t = new Tuple(t2._1, t2._2, t2._3)
        val tuple = new AlignedTuple(t1t, t2t, false, false)
        val score = getAlignScore(tuple)
        
        val t2tCross = new Tuple(t2._3, t2._2, t2._1)
        val tupleCross = new AlignedTuple(t1t, t2tCross, false, false)
        val scoreCross = getAlignScore(tupleCross)
        if (score > scoreCross)
            return (score, false)
        else
            return (scoreCross, true)
    }
    
    def getAlignScore(tuple: AlignedTuple): Double = {
            
        val tupleAnno = new AlignTupleAnno(tuple)
        var values = new Array[Double](0)
        for (feature <- features) {
            values ++= feature.extract(tupleAnno)
        }       
        val ins = new Instance(1.0, values)
        val distribution = classifier.distributionForInstance(ins)
        return distribution(positiveIndex)
    }
    
    def trainAndTest(trainList:List[AlignedTuple], 
            	devList:List[AlignedTuple] = null, 
            	crossValidateOnTrain: Boolean = false,
            	modelName: String = null) {
   		val attributes = new FastVector();
		for (feature <- features) {
		    for (feat <- feature.names)
		    	attributes.addElement(new Attribute(feat))
		}

		attributes.addElement(cls);
		val trainSet = new Instances("Tuple Alignment Train", attributes, trainList.length);
		// Make the last attribute be the class
		trainSet.setClassIndex(trainSet.numAttributes()-1); 
	
		extractFeatures(trainList, trainSet)

		// training
		// val classifier = new Logistic()
		// val classifier = new SMO()
		// val classifier = new VotedPerceptron()
		classifier.buildClassifier(trainSet)
		
		if (modelName != null)
		    weka.core.SerializationHelper.write(modelName, classifier);
		
		// val weights = classifier.coefficients()
		println(classifier.toString())	

		// testing on dev
		if (devList != null) {
			val devSet = new Instances("Tuple Alignment Dev", attributes, devList.length);
			devSet.setClassIndex(devSet.numAttributes()-1); 
			extractFeatures(devList, devSet)
			val eval = new Evaluation(devSet)
			val predictions = eval.evaluateModel(classifier, devSet)

			for(i <- 0 until predictions.size) {
		    	val scoresPerLabel = classifier.distributionForInstance(devSet.instance(i))
			    if (predictions(i) == positiveIndex && devList(i).align == true) {
			    	print(Console.GREEN + "CORRECT (" + "%.3f".format(scoresPerLabel(positiveIndex)) + ") ")
			    } else if (predictions(i) == negativeIndex && devList(i).align == false) {
			    	print(Console.GREEN + "CORRECT (" + "%.3f".format(scoresPerLabel(negativeIndex)) + ") ")
			    } else if (predictions(i) == positiveIndex && devList(i).align == false) {
			    	print(Console.RED + "WRONG (" + "%.3f".format(scoresPerLabel(negativeIndex)) + ") ")
			    } else {
			    	print(Console.RED + "WRONG (" + "%.3f".format(scoresPerLabel(positiveIndex)) + ") ")
			    }
			    println(predictions(i).toString + " " + devList(i) + Console.RESET)
			}
			
			for (i <- 0 until devList.size) {
			}
			println(eval.toSummaryString("\nResults on Dev/Test set\n\n", false))
			println(f"F1: ${eval.fMeasure(positiveIndex)}%.3f")
		}


		// cross validation
		if (crossValidateOnTrain) {
			val eval = new Evaluation(trainSet)
			eval.crossValidateModel(classifier, trainSet, 10, new Random(1))
			println(eval.toSummaryString("\nResults on 10-fold CV\n\n", false))
		}
    }
}

object TupleClassifier {

    def main(args: Array[String]): Unit = {
        object MODE extends Enumeration {
           type MODE = Value 
           val TRAIN_WITH_CV, TRAIN_AND_TEST, TRAIN_ALL_SAVE_MODEL, ONLY_TEST_LOAD_MODEL = Value
        }
        import MODE._

        val reader = new AlignTupleReader("alignment-data/tuples/msr.edinburgh.train.tuple.json")
        val testReader = new AlignTupleReader("alignment-data/tuples/msr.edinburgh.test.tuple.json")
        
        var mode = ONLY_TEST_LOAD_MODEL
        // var mode = TRAIN_ALL_SAVE_MODEL
        // var mode = TRAIN_AND_TEST
        mode match {
            case TRAIN_WITH_CV => 
            	val classifier = new TupleClassifier()
            	classifier.trainAndTest(reader.toList, null, true)
            case TRAIN_AND_TEST => 
            	val classifier = new TupleClassifier()
            	classifier.trainAndTest(reader.toList, testReader.toList, false)
            case TRAIN_ALL_SAVE_MODEL => 
            	val classifier = new TupleClassifier()
            	classifier.trainAndTest(reader.toList ++ testReader.toList, null, true, "/tmp/tuple-align.model")
            case ONLY_TEST_LOAD_MODEL => 
            	val classifier = new TupleClassifier("/tmp/tuple-align.model")
            	// println(classifier.getAlignScoreAndIsCross(("John", "loves", "Mary"), ("John", "likes", "Mary")))
            	// println(classifier.getAlignScoreAndIsCross(("John", "loves", "Mary"), ("Mary", "likes", "John")))
            	// println(classifier.getAlignScoreAndIsCross(("John", "loves", "Mary"), ("Mary", "likes", "Johnny")))
            	// println(classifier.getAlignScoreAndIsCross(("John", "loves", "Mary"), ("Mary", "likes", "Casey")))
            	println(classifier.getAlignScoreAndIsCross(("humans", "have", "energy"), ("electrical energy to pedal a bicycle", "", "")))
            	println(classifier.getAlignScoreAndIsCross(("a squirrel", "storing", "nuts"), ("some animals", "storing", "food")))
            	println(classifier.getAlignScoreAndIsCross(("a girl", "eat", "an apple"), ("Animals", "take in", "food as a nutrient")))
            	println(classifier.getAlignScoreAndIsCross(("a girl", "eat", "an apple"), ("Animals", "take in", "food")))
            	println("\n\n\n")
            	println(classifier.getAlignScoreAndIsCross(("a squirrel", "storing", "nuts"), ("An example of a plant responding to a change in its The typical life cycle of a plant environment", "leaves", "")))
            	println(classifier.getAlignScoreAndIsCross(("grow thicker fur in winter", "", ""), ("", "pollinated", "flowers With the help of animals insects and birds rain and wind")))
            	println(classifier.getAlignScoreAndIsCross(("", "formed", "seeds that will grow into new plants"), ("some animal to keep warm", "", "")))
            	println(classifier.getAlignScoreAndIsCross(("a girl", "eat", "an apple"), ("their the similarities and differences between animals arms and / or wings arm", "adapted", "Depending on how the organism lives")))
        }
    }
}