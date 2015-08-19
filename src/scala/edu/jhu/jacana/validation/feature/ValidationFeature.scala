/**
 *
 */
package edu.jhu.jacana.validation.feature

import edu.stanford.nlp.util.CoreMap
import edu.jhu.jacana.validation.reader.Snippet
import scala.collection.mutable.HashSet

/**
 * @author Xuchen Yao
 *
 */
abstract class ValidationFeature {
    var features: Array[String] = null
 	def extract(query:CoreMap, snippets:List[Snippet], optionSet: HashSet[String]): Array[Double]
}