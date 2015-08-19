/**
 *
 */
package edu.jhu.jacana.align.resource


import scala.collection.mutable
import scala.collection.JavaConversions._
import edu.jhu.jacana.align.util.Loggable
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.FileInputStream
import java.io.ObjectInputStream
import coe.distrib.ConditionalEditModel
import edu.jhu.jacana.util.FileManager

/**
 * A similarity measure of names and variations from the following paper:
 * 
 * Nicholas Andrews, Jason Eisner, and Mark Dredze. 2012. 
 * Name phylogeny: A generative model of string variation. 
 * In Empirical Methods in Natural Language Processing (EMNLP).
 * 
 * @author Xuchen Yao
 *
 */
object NamePhylogeny extends Loggable {
    
  val MODEL_DIR     = FileManager.getResource("resources/name-phylogeny/")
  val MODEL_PATH    = MODEL_DIR + "half_wiki_transducer.model"
  val ALPHABET_PATH = MODEL_DIR + "half_wiki_alphabet.txt"

  val isyms = mutable.Map[Character,Integer]()
  val osyms = mutable.Map[Character,Integer]()

  //var transducer: ConditionalEditModel = null
  var transducer: ConditionalEditModel = null
  
  def isSetup = (transducer != null)

  def setup() {

    // Load the alphabet
    readAlphabet(ALPHABET_PATH)

    // Load the model
    transducer = readModel(MODEL_PATH)

    info("done loading transducer models")
  }

  def normalize(logp: Double): Double = {
    // If there are two long dissimilar strings, the probability will
    // underflow.
    //if(logp == Double.NegativeInfinity) {
    if(logp < -50) {
      //warn("underflow in transducer prob calc")
      -1.0
    }

    // Weird case: shouldn't happen
    else if(logp == Double.PositiveInfinity) {
      //warn("positive infinity in transducer prob calc")
      -1.0
    }

    // Weird case: shouldn't happen
    else if(logp == Double.NaN) {
      //warn("NaN in transducer prob calc")
      -1.0
    }

	else logp / 20d		// crude attempt to put on scale with other features
  }

  /**
   * get similarity between two strings. 
   * 
   * In theory, score(x,y) gives p(y|x) where x is the "parent" of y.
   * This function returns the larger value from p(y|x) and p(x|y)
   * since we don't usually know who's who's "parent".
   * 
   */
  def getSimilarity(x_str: String, y_str: String): Double = {
      //return 1+normalize(Math.max(score(x_str, y_str), score(y_str, x_str)))
      return -normalize(Math.max(score(x_str, y_str), score(y_str, x_str)))
  }
  
  def score(x_str: String, y_str: String): Double = {
  	if(!isSetup) {
  		warn("ad-hoc set up! no examples")
  		setup()
  	}
    //val x = coe.util.StringUtil.intify(x_str, isyms, false)
    //val y = coe.util.StringUtil.intify(y_str, osyms, false)
    val x = intify(x_str, isyms)
    val y = intify(y_str, osyms)
    return Math.log(transducer.p(x, y))
    //return transducer.p(x, y)
    
//	try {
//		var logp = transducer.logp(x,y)
//		var logpb = transducer.logp(y,x)
//		//log.info("x_str="+x_str+" y_str="+y_str+" logp=" + logp + " logpb=" + logpb)
//		return normalize(logp) + normalize(logpb)
//	} catch {
//		case e: ArrayIndexOutOfBoundsException =>
//			//e.printStackTrace
//			//log.warning("[Transducer score] ArrayIndexOutOfBoundsException x=%s, y=%s".format(x_str, y_str))
//			return -100d
//	}
  }

  /*
  def featurize(addTo: FeatureVectorInterface, a: Alignment, report: Document, passage: Document) {
    
    assert(transducer != null)

	val (reportCM, passageCM) = CanonicalMentionFinder.canonicalMentions(a, report, passage)
	val rh = report.getHeadString(reportCM)
	val ph = passage.getHeadString(passageCM)
    addTo.set("transducer-logp-head-canonical", 0, score(rh, ph))

	a match {
		case aca: ArgCorefAlignment => {
			val ms = aca.reportCoref.flatMap(ra =>
				aca.passageCoref.map(pa =>
					score(report.getHeadString(ra), passage.getHeadString(pa))))
			addTo.set("transducer-logp-head-coref-min", 0, ms.min)
			addTo.set("transducer-logp-head-coref-avg", 0, ms.sum / ms.size.toDouble)
		}
		case _ => {}
	}
  }
  */

  def intify(mention: String, syms: mutable.Map[Character,Integer]) : Array[Int] = {
    val xs = Array.ofDim[Int](mention.length)
    for(i <- 0 until mention.length) {
      val ind = syms get mention.charAt(i)
      ind match {
        case None    => xs(i) = '?'
        case Some(x) => xs(i) = x
      }
    }
    xs
  }

  def readModel(filename: String) : ConditionalEditModel = {
    val input = new ObjectInputStream(new FileInputStream(filename))
    val obj = input.readObject().asInstanceOf[ConditionalEditModel]
    obj
  }

  def readAlphabet(filename: String) {
    try {
      // Count the number of input lines
      val br = new BufferedReader(new FileReader(filename))
      
      val input_symbol_str  = br.readLine()
      val output_symbol_str = br.readLine()
      
      val input_symbols  = input_symbol_str.split("\t")
      for(i <- 0 until input_symbols.length) {
        if(input_symbols(i).length() > 1) {
          info("Ignoring input symbol: " + input_symbols(i) + " (length > 1).")
        }
        isyms.put(input_symbols(i).charAt(0),i)
      }
      info(isyms.keys.size + " input symbols.")
      
      val output_symbols = output_symbol_str.split("\t")
      for(o <- 0 until output_symbols.length) {
        if(output_symbols(o).length() > 1) {
          info("Ignoring input symbol " + output_symbols(o) + " (length > 1).")
        }
        osyms.put(output_symbols(o).charAt(0),o)
      }
      info(osyms.keys.size + " output symbols.");
    }
    catch {
      case e:IOException => {
        println(e.getMessage())
        sys.exit(1)
      }
    }
  }

  def main(args: Array[String]): Unit = {

        //println(getSimilarity("asd", "jlads"))
        //println(getSimilarity("system", "computer"))
        println(getSimilarity("William", "Bill"))
        println(getSimilarity("Bill", "William"))
        println(getSimilarity("Bill", "Bill"))
        println(getSimilarity("Bill", "Billy"))
        println(getSimilarity("Bill", "Mary"))
        println(getSimilarity("Billy", "Bill"))
        println("-----------");
        println(getSimilarity("William", "Bill"))
        println(getSimilarity("Billy", "Bill"))
        println(getSimilarity("Bill", "Bill"))
        println(getSimilarity("Mary", "Bill"))
        println("-----------");
        println(getSimilarity("NYC", "New York"))
        println(getSimilarity("New York", "NYC"))
        println(getSimilarity("New", "NYC"))
        println(getSimilarity("York", "NYC"))
  }
}