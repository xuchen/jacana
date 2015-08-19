/**
 *
 */
package edu.jhu.jacana.validation.validator

import edu.jhu.jacana.align.util.Loggable
import org.apache.commons.cli.Options
import org.apache.commons.cli.PosixParser
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter
import edu.jhu.jacana.util.FileManager
import edu.jhu.jacana.validation.reader._
import scala.collection.mutable.HashSet

/**
 * @author Xuchen Yao
 *
 */
abstract class AbstractValidator extends Loggable {

	val options = new Options()
	var eclipseMode = true
    var trainFilename:String = null
    var devFilename:String = null
    var testFilename:String = null
    var modelFilename:String = null
	
	// return true if no parameters are passed in
	//  (might be in eclipse mode)
	def parseArguments(args: Array[String]): Boolean = {
	    //if (eclipseMode) return true
	    if (args.length == 0) {
	        eclipseMode = true
	        return true
	    }
        //eclipseMode = false
		val parser:CommandLineParser  = new PosixParser()
	    
	    options.addOption("h", "help", false, "show help")
	    options.addOption("r", "train", true, "training file")
	    options.addOption("e", "test", true, "test file")
	    options.addOption("d", "dev", true, "dev file")
	    options.addOption("m", "model", true, "model file \n" +
	    		"(train: save to, test: read from, both: save to, dev: append '.iterXX' and save to)")
	    		
	    val line:CommandLine  = parser.parse(options, args)
	    if (line.hasOption("help")) {
	        val usage = "java "+this.getClass().getName().split("\\$")(0)
	        val header = "train&test: "+ " -r train_file -e test_file -m save_to_model_file\n" +
	        		"train: -r train_file -m save_to_model_file\n" + 
	        		"test: -m load_from_model_file -e test_file\n"
	        
	        (new HelpFormatter()).printHelp(100, usage, header, options, "")
	        System.exit(0)
	    }
	    trainFilename = line.getOptionValue("train", null)
	    exitIfNotExist(trainFilename)
	    devFilename = line.getOptionValue("dev", null)
	    // exitIfNotExist(devFilename)
	    testFilename = line.getOptionValue("test", null)
	    // exitIfNotExist(testFilename)
	    modelFilename = line.getOptionValue("model", "/tmp/aligner.model")
	    
	    return false
	}

	def exitIfNotExist(fname: String) {
	    if (fname != null && !FileManager.fileExists(fname)) {
	        System.err.println(s"$fname doesn't exist, exiting.")
	        System.exit(-1)
	    }
	}
	
	// colored output in ANSI terminals
	// install for colored output in eclipse:
	// http://mihai-nita.net/2013/06/03/eclipse-plugin-ansi-in-console/
	def colorizeAnswer(list:List[Double], correct:Int, proposed:HashSet[Int]):String = {
	    var strs = new Array[String](list.size)
	    for ((ans, i) <- list.view.zipWithIndex) {
	        val short = "%.3f".format(ans)
	        i match {
	            case j if (j == correct) => strs(i) = Console.GREEN + short
	            case j if (proposed.contains(j)) => strs(i) = Console.RED + short
	            case _ => strs(i) = Console.RESET + short
	        }
	    }
	    return "(" + strs.mkString(", ") + Console.RESET + ")"
	}
	
    def evaluate(questionList: List[Question]) {
        var correct = 0.0
        for (question <- questionList) {
        	question.scores match {
        	    case None => println("not answered: " + question.qid)
        	    case Some(list) =>
        	        val scoreList = list.map(_.toDouble)
        	        val set = new HashSet[Int]()
        	        var max = -1.0
        	        for ((score,i) <- scoreList.zipWithIndex) {
        	            if (score > max) {
        	                set.clear()
        	                max = score
        	                set += i
        	            } else if (score == max) {
        	                set += i
        	            }
        	        }
        	        val colored = colorizeAnswer(scoreList, question.answer, set)
        	        if (set.contains(question.answer)) {
        	            correct += 1.0/set.size
        	            println("%s/%d CORRECT:\t%s".format(question.qid, question.answer, colored))
        	        } else {
        	            println("%s/%d WRONG:\t\t%s".format(question.qid, question.answer, colored))
        	        }
        	}
        }
        val precision = correct / questionList.length
        println("precision: " + precision)
    }
	
}