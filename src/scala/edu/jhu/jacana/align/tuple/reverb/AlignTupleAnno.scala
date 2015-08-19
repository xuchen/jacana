/**
 *
 */
package edu.jhu.jacana.align.tuple.reverb

import edu.jhu.jacana.nlp.StanfordCore
import edu.jhu.jacana.nlp.SnowballStemmer
import edu.jhu.jacana.align.util.Loggable
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreLabel

/**
 * Annotation for aligned tuple
 * 
 * @author Xuchen Yao
 *
 */
class AlignTupleAnno (tuple:AlignedTuple) {
    
    var align = tuple.align
    var cross = tuple.cross
    var tuple1 = new TupleAnno(tuple.src)
    var tuple2 = new TupleAnno(tuple.tgt)
}

class TupleAnno(val t:Tuple) extends Loggable {
    // in most cases it's a grammatical sentence, so we parse it
    val origSent = (t.arg1+" "+t.rel+" "+t.arg2).replaceAll("\\s+", " ").trim()
   	val document = StanfordCore.processWithSpaceTokenizer(origSent)
  	private val sents = StanfordCore.getSentences(document)
  	private val sent = sents.get(0)
  	val tokens = StanfordCore.getTokensInString(sent)
  	
  	if (tokens.size != origSent.split(" ").size) {
  	   log.error("sentence length doesn't match after parsing with CoreNLP!") 
  	   log.error(origSent.split("\\s+").size + " original: " + origSent)
  	   log.error(tokens.size + " after parsing: " + tokens.mkString(" "))
  	}
    
    // now proceed assuming the above error didn't happen
    
  	
  	val tokensLowercase = tokens.map(x => x.toLowerCase())
  	val tokensLowercaseStemmed = tokensLowercase.map(x => SnowballStemmer.stem(x))
  	val labels = StanfordCore.getTokensInLabels(sent)        
  	val posTags = labels.map(x => x.tag()) 
  	val lemmas = labels.map(x => x.lemma()) 
  	val entities = StanfordCore.getNamedEntitiesInString(sent)
    
    
    val arg1IdxStart = 0
    val arg1IdxEnd = t.arg1.split(" ").size
    val relIdxStart = arg1IdxEnd 
    val relIdxEnd = relIdxStart  + t.rel.split(" ").size
    val arg2IdxStart = relIdxEnd 
    val arg2IdxEnd = origSent.split(" ").size
    
    val arg1Anno = new FragAnno(arg1IdxStart, arg1IdxEnd, tokens, labels, entities)
    val arg2Anno = new FragAnno(arg2IdxStart, arg2IdxEnd, tokens, labels, entities)
    val relAnno = new FragAnno(relIdxStart, relIdxEnd, tokens, labels, entities)
    
    def getArg1Tokens() = tokens.slice(arg1IdxStart, arg1IdxEnd)
    def getArg2Tokens() = tokens.slice(arg2IdxStart, arg2IdxEnd)
    def getRelTokens() = tokens.slice(relIdxStart, relIdxEnd)
    
    def getArg1Pos() = posTags.slice(arg1IdxStart, arg1IdxEnd)
    def getArg2Pos() = posTags.slice(arg2IdxStart, arg2IdxEnd)
    def getRelPos() = posTags.slice(relIdxStart, relIdxEnd)
    
}

// an annotation "fragment" that holds all info about arg1/rel/arg2...
class FragAnno(val startIdx:Integer, val endIdx:Integer, fullTokens:Array[String],
        fullLabels:Array[CoreLabel], fullNERs:Array[String]) {
    val len = endIdx - startIdx
    val tokens = fullTokens.slice(startIdx, endIdx)
    val string = tokens.mkString(" ")
    val labels = fullLabels.slice(startIdx, endIdx)
    val entities = fullNERs.slice(startIdx, endIdx)
    val posTags = labels.map(x => x.tag())
  	val lemmas = labels.map(x => x.lemma()) 
}