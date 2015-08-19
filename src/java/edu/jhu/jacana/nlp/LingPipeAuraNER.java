/**
 * 
 */
package edu.jhu.jacana.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

import edu.jhu.jacana.aura.AuraConcept;
import edu.jhu.jacana.aura.AuraConceptManager;
import edu.jhu.jacana.util.FileManager;
import edu.jhu.jacana.util.StringUtils;

/**
 * A simple NER chunker based on exact dictionary-based chunking.
 * http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html
 * This class utilizes AuraConceptManager and tag tokens with concept names.
 * @author Xuchen Yao
 *
 */
public class LingPipeAuraNER {
	
	static final double CHUNK_SCORE = 1.0;
    static MapDictionary<String> dictionary;
    static ExactDictionaryChunker chunker;

    public static void init () {
    	if (chunker != null) return;
    	dictionary = new MapDictionary<String>();
    	AuraConceptManager.create();
    	for (Entry<String, AuraConcept> entry:AuraConceptManager.getPhrase2concept().entrySet()) {
    		dictionary.addEntry(new DictionaryEntry<String>(entry.getKey(), entry.getValue().getName(),CHUNK_SCORE));
    	}
    	
        chunker = new ExactDictionaryChunker(dictionary, IndoEuropeanTokenizerFactory.INSTANCE, true, false);
    }
    
	public static String getNERsInOneLine(String sent) {
		return getNERsInOneLineFuzzyMatch(sent);
	}
	
	public static String getNERsInUiucFormat(String sent) {
		return sent+"\n"+getNERsInOneLineFuzzyMatch(sent)+"\n"+sent+"\n\n";
	}
	
	/**
	 * Get the fuzzy matching of each tokens being what NER in a sentence.
	 * If due to different tokenization the character offset if off by 1,
	 *  it still tries to find the closest token.
	 *  A robust method.
	 * @param sent a sentence
	 * @return NERs separated by tabs
	 */
	public static String getNERsInOneLineFuzzyMatch(String sent) {
		if (chunker == null) init();
	    Chunking chunking = chunker.chunk(sent);
		//System.out.println(chunking);
	    sent = sent.trim();
	    Pattern blank = Pattern.compile("\\s+");
	    Matcher m = blank.matcher(sent);
	    
	    // marks each character belongs to which token
	    int[] tokenIndices = new int[sent.length()];
	    
	    int start = 0, end = 0, tokenIdx = 0;
	    while (m.find()) {
	    	for (int i=start; i<m.start(); i++)
	    		tokenIndices[i] = tokenIdx;
	    	tokenIdx++;
	    	start = m.start();
	    }
    	for (int i=start; i<sent.length(); i++)
    		tokenIndices[i] = tokenIdx;
    	
    	// named entities
    	String[] splits = sent.split("\\s+");
    	Arrays.fill(splits, "-");
		for (Chunk chunk:chunking.chunkSet()) {
			//System.out.println(String.format("%d-%d|%s:%s", chunk.start(), chunk.end(), sent.substring(chunk.start(), chunk.end()), chunk.type()));
			start = tokenIndices[chunk.start()];
			end = tokenIndices[chunk.end()-1];
			for (int i=start; i<= end; i++) {
				if (i == start)
					splits[i] = "B-"+chunk.type();
				else
					splits[i] = "I-"+chunk.type();
			}
		}
		return StringUtils.joinWithTabs(splits);
	}
	
	/**
	 * Get the exact matching of each tokens being what NER in a sentence.
	 * If due to different tokenization the character offset if off by 1,
	 * this method fails and throws an uncatched exception.
	 * @param sent a sentence
	 * @return NERs separated by tabs
	 */
	public static String getNERsInOneLineExactMatch(String sent) {
		if (chunker == null) init();
	    Chunking chunking = chunker.chunk(sent);
		//System.out.println(chunking);
	    sent = sent.trim();
	    Pattern blank = Pattern.compile("\\s+");
	    Matcher m = blank.matcher(sent);
	    
	    // map the start and end of each token's char offset to token index
	    HashMap<Integer, Integer> charOffStart2tokenIdx = new HashMap<Integer, Integer>(); 
	    HashMap<Integer, Integer> charOffEnd2tokenIdx = new HashMap<Integer, Integer>(); 
	    int start = 0, end = 0, tokenIdx = 0;
    	charOffStart2tokenIdx.put(start, tokenIdx);
	    while (m.find()) {
	    	charOffEnd2tokenIdx.put(m.start(), tokenIdx++);
	    	charOffStart2tokenIdx.put(m.end(), tokenIdx);
	    }
    	charOffEnd2tokenIdx.put(sent.length(), tokenIdx);
    	
    	// named entities
    	String[] splits = sent.split("\\s+");
    	Arrays.fill(splits, "-");
		for (Chunk chunk:chunking.chunkSet()) {
			//System.out.println(String.format("%d-%d|%s:%s", chunk.start(), chunk.end(), sent.substring(chunk.start(), chunk.end()), chunk.type()));
			start = charOffStart2tokenIdx.get(chunk.start());
			end = charOffEnd2tokenIdx.get(chunk.end());
			for (int i=start; i< end; i++) {
				if (i == start)
					splits[i] = "B-"+chunk.type();
				else
					splits[i] = "I-"+chunk.type();
			}
		}
		return StringUtils.joinWithTabs(splits);
	}
    
	public static void getNERs(String sent) {
		if (chunker == null) init();
	    Chunking chunking = chunker.chunk(sent);
		//System.out.println(chunking);
		for (Chunk chunk:chunking.chunkSet()) {
			System.out.println(String.format("%d-%d|%s:%s", chunk.start(), chunk.end(), sent.substring(chunk.start(), chunk.end()), chunk.type()));
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			BufferedReader reader = FileManager.getReader(args[0]);
			String line;
			while ((line=reader.readLine()) != null) {
				System.out.println(getNERsInUiucFormat(line));
			}
			reader.close();
		} else {
			String sent = "to NAD + , but to FAD , which accepts 2 electrons and 2 protons to become FADH2 ."; 
			getNERs(sent);
			System.out.println(getNERsInOneLine(sent));
		}
	}

}
