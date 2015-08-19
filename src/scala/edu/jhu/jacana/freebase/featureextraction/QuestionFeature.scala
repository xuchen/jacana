/**
 *
 */
package edu.jhu.jacana.freebase.featureextraction

import edu.jhu.jacana.freebase.questionanalysis.Question
import scala.collection.mutable.ArrayBuffer

/**
 * @author Xuchen Yao
 *
 */
object QuestionFeature {

    def extract(q: Question):Array[String] = {
        val features = new ArrayBuffer[String]()
        features += "qtype=" + q.qtype
        features += "qword=" + q.qword
        features += "qrelation=" + q.qrelation
        features += "qverb=" + (if (q.qverb == null) "null" else q.qverb.lemma())
        features += "qverb_tag=" + (if (q.qverb == null) "null" else q.qverb.tag())
        for ((topic, ner) <- q.candidateTopics) {
            ner match {
                case "LOCATION" | "PERSON" | "MISC" =>
                	features += "qtopic_ner:" + ner
                case _ =>
            }
        }
        // TODO: more precise question analysis features
        // TODO: remove feature keywords (:, spaces)
        return features.toArray
    }
}