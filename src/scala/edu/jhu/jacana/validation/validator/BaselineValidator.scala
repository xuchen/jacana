/**
 *
 */
package edu.jhu.jacana.validation.validator

import edu.jhu.jacana.validation.reader.ExamInputJsonReader
import edu.jhu.jacana.validation.ranker.OverlapRanker
import edu.jhu.jacana.validation.ranker.AlignmentRanker

/**
 * A bag of words baseline which simply counts how many times an answer
 * candidate appears in the retrieved snippets
 * @author Xuchen Yao
 *
 */
class BaselineValidator extends AbstractValidator {

}

object BaselineValidator {
    
    def main(args: Array[String]): Unit = {
        val topDir = System.getProperty("user.home") + "/Halo2/Retrieved" + "/"
        val validator = new BaselineValidator()
        if (true) {
	        if (validator.eclipseMode) {
	            validator.trainFilename = topDir + "solr.4.json"
	            // validator.trainFilename = topDir + "bing.4.tiny.json"
	            // validator.trainFilename = topDir + "solr.4.json"
	            validator.testFilename = topDir + "bing.4.test.json"
	        }
	        val reader = new ExamInputJsonReader(validator.trainFilename)
	        val reader2 = new ExamInputJsonReader(validator.testFilename)
	        val trainList = reader.getAll()
	        val testList = reader2.getAll()
		    // val ranker = new OverlapRanker
		    val ranker = new AlignmentRanker
	        ranker.rank(trainList, testList, 50)
	        println("train " + trainList.length)
	        println("test " + testList.length)
	        validator.evaluate(testList)
        } else {
 	        if (validator.eclipseMode) {
	            validator.trainFilename = topDir + "bing.4.json"
	            // validator.trainFilename = topDir + "bing.4.tiny.json"
	            // validator.trainFilename = topDir + "solr.4.json"
	        }
	        val reader = new ExamInputJsonReader(validator.trainFilename)
	        val trainList = reader.getTrain()
	        val devList = reader.getDev()
		    // val ranker = new OverlapRanker
		    val ranker = new AlignmentRanker
	        ranker.rank(trainList, devList, 50)
	        println("train " + trainList.length)
	        println("dev " + devList.length)
	        validator.evaluate(devList)           
        }
    }
}