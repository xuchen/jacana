/**
 * 
 */
package edu.jhu.jacana.nlp;
import java.util.HashMap;

import org.tartarus.snowball.ext.frenchStemmer;

import edu.jhu.jacana.util.StringUtils;

/**
 * unfortunately snowball.jar doesn't provide a wrapper class for stemmers of all languages.
 * @author Xuchen Yao
 *
 */
public class SnowballStemmerFrench {
	/** Snowball stemmer for the French language. */
	private static frenchStemmer stemmer = new frenchStemmer();
	protected static HashMap<String, String> word2stem = new HashMap<String, String>();
	
	
    /**
     * Stems a single English word.
     * 
     * @param word the word to be stemmed
     * @return stemmed word
     */
	public static String stem(String word) {
		if (word2stem.containsKey(word)) {
			return word2stem.get(word);
		}
		stemmer.setCurrent(word);
		stemmer.stem();
		String stem = stemmer.getCurrent();
		word2stem.put(word, stem);
		return stem;
    }
	
	/**
	 * Stems all tokens in a string of space-delimited English words.
	 * 
	 * @param tokens string of tokens to be stemmed
	 * @return string of stemmed tokens
	 */
	public static String stemAllTokens(String tokens) {
		String[] tokenArray = tokens.split(" ");
		
		return StringUtils.joinWithSpaces(stemAllTokens(tokenArray));
	}
	
	/**
	 * Stems all tokens in an array
	 * 
	 * @param tokenArray array of tokens to be stemmed
	 * @return 
	 */
	public static String[] stemAllTokens(String[] tokenArray) {
		String[] stemmed = new String[tokenArray.length];
		
		for (int i = 0; i < tokenArray.length; i++)
			stemmed[i] = stem(tokenArray[i]);
		
		return stemmed;
	}
	
	public static void main(String[] args) {
		System.out.println(SnowballStemmerFrench.stem("continué"));
		System.out.println(SnowballStemmerFrench.stem("continuité"));
	}
}
