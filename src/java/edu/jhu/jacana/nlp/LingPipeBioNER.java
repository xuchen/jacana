/**
 * 
 */
package edu.jhu.jacana.nlp;

import java.io.File;
import java.io.IOException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;

import edu.jhu.jacana.util.FileManager;

/**
 * A simple NER chunker based on the trained model on GENIA from LingPipe.
 * The NER results are sometimes inconsistent: the same unambiguous term
 * can be tagged with different entity names. Also, the IndoEuropeanTokenizerFactory
 * handles (trained on MUC6) tokenization internally so there can be a mismatch if later
 * another tokenization (e.g., Stanford tokenizer, trained on PTB) is applied on the same sentence.
 * @author Xuchen Yao
 *
 */
public class LingPipeBioNER {
	
	private static Chunker chunker;
	
	public static void init() {
		if (chunker != null) return;
		try {
			chunker = (Chunker) AbstractExternalizable.readObject(new File(FileManager.getResource("resources/model/ne-en-bio-genia.TokenShapeChunker")));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void getNERs(String sent) {
		if (chunker == null) init();
		Chunking chunking = chunker.chunk(sent);
		System.out.println(chunking);
		for (Chunk chunk:chunking.chunkSet()) {
			System.out.println(String.format("%d-%d|%s:%s", chunk.start(), chunk.end(), sent.substring(chunk.start(), chunk.end()), chunk.type()));
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String sent = "In step 6, electrons are transferred not to NAD+, but to FAD, which accepts 2 electrons and 2 protons to become FADH2."; 
		LingPipeBioNER.getNERs(sent);
	}

}
