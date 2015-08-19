/**
 *
 */
package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet.NONE_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet.NULL_STATE
import edu.jhu.jacana.align.IndexLabelAlphabet
import edu.jhu.jacana.align.Alphabet


/**
 * @author Xuchen Yao
 *
 */
object ChunkingAlignFeature extends AlignFeature {
	object Types {
	    val CHUNK_SRC_PHRASE = "chunk.src_is_phrase"
	    val CHUNK_TGT_PHRASE = "chunk.tgt_is_phrase"
	    // if both source and target are phrases, whether they are identical or not
	    val CHUNK_IDENTICAL = "chunk.identical"
	    val CHUNK_DIFFERENT = "chunk.different"
	    // if both source and target are phrases,  append the mapping
	    // such as "chunk.map.NP-NP"
	    val CHUNK_MAP = "chunk.map."
    }
    
    import Types._
    
	def addPhraseBasedFeature(pair: AlignPair, ins:AlignFeatureVector, i:Int, srcSpan:Int, j:Int, tgtSpan:Int, currState:Int, featureAlphabet: Alphabet, labelAlphabet:IndexLabelAlphabet){
		// Array(B-NP, I-NP, B-VP, B-PP, B-NP, B-NP, I-NP, I-NP, I-NP, O)
        val (srcIsPhrase, srcPhrase) = getPhrase(pair.srcChunks, i, i+srcSpan)
		if (srcIsPhrase)
			ins.addFeature(CHUNK_SRC_PHRASE, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet) 
		if (j == -1) {
		} else {
			val (tgtIsPhrase, tgtPhrase) = getPhrase(pair.tgtChunks, j, j+tgtSpan)
			if (tgtIsPhrase)
				ins.addFeature(CHUNK_TGT_PHRASE, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet) 
			if (srcIsPhrase && tgtIsPhrase) {
			    if (srcPhrase == tgtPhrase)
			    	ins.addFeature(CHUNK_IDENTICAL, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet) 
			    else
			    	ins.addFeature(CHUNK_DIFFERENT, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
		    	//ins.addFeature(CHUNK_MAP+srcPhrase+"-"+tgtPhrase, NONE_STATE, currState, 1.0, srcSpan, featureAlphabet)
			}
		}
	}
	
	/**
	 * given a list of chunks (for the FULL sentence) such as Array(B-NP, I-NP, B-VP),
	 * and chunk start <code>cStart</code> (inclusive) and end <code>cEnd</code> (not inclusive),
	 * return a tuple of: boolean -- whether this is a phrase
	 * and String -- if phrase, then what phrase (such as "NP").
	 * 
	 * the above example of Array(B-NP, I-NP, B-VP) would return (false, null)
	 */
	def getPhrase(chunks:Array[String], cStart:Int, cEnd:Int): Tuple2[Boolean, String] = {
		// Array(B-NP, I-NP, B-VP, B-PP, B-NP, B-NP, I-NP, I-NP, I-NP, O)
	    var isPhrase = true
	    var phrase = ""
	    var i = cStart
	    if (chunks(i).startsWith("I"))
	        // starts with "I", such as "I-NP"
	        isPhrase = false
        else if (chunks(i).startsWith("B"))
            phrase = chunks(i).split("-")(1)
        else
            phrase = "O"
        i += 1
	    while (isPhrase && i < cEnd) {
	        if (chunks(cStart) == "O") {
	            if (chunks(i) != "O")
	                isPhrase = false
	        } else {
	            if (!chunks(i).startsWith("I"))
	                // a "B-" must be followed by "I-"
	                isPhrase = false
	        }
	        i += 1
	    }
	    if (isPhrase && (cEnd < chunks.length && chunks(cEnd).startsWith("I")))
	        // beyond [cStart, cEnd) there's still an "I-"
            isPhrase = false
	    return (isPhrase, if (isPhrase) phrase else null)
	}
}