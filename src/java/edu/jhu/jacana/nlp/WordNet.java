package edu.jhu.jacana.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;



import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.dictionary.Dictionary;


/**
 * This version of WordNet API uses the extJWNL http://extjwnl.sourceforge.net/ package
 * which provides thread-safe access. Unfortunately the API is not compatible with the original
 * WordNet data (including Rion Snow's expanded WordNet). Thus we have to use the provided data
 * jar file: extjwnl-data-wn31-1.2.jar
 * 
 * The folders resources/wordnet and resources/wordnet-snow are thus not used anymore (unless
 * the class WordNetNotThreadsafe is employed instead)
 * 
 * @author Xuchen Yao
 * @version Oct. 6, 2014
 */

public class WordNet{
	/** Indicates that a word is an adjective. */
	public static final POS ADJECTIVE = POS.ADJECTIVE;
	/** Indicates that a word is an adverb. */
	public static final POS ADVERB = POS.ADVERB;
	/** Indicates that a word is a noun. */
	public static final POS NOUN = POS.NOUN;
	/** Indicates that a word is a verb. */
	public static final POS VERB = POS.VERB;
	
	/** Maximum length of a path to an expansion. */
	public static final int MAX_PATH_LENGTH = 1;
	
	// relations for multiple parts of speech
	/** Weight for the relation 'synonym'. */
	private static final double SYNONYM_WEIGHT = 0.9;
	/** Weight for the relation 'hypernym'. */
	private static final double HYPERNYM_WEIGHT = 0.8;
	/** Weight for the relation 'hyponym'. */
	private static final double HYPONYM_WEIGHT = 0.7;
//	/** Weight for the relation 'see-also'. */
//	private static final double SEE_ALSO_WEIGHT = 0.5;
//	/** Weight for the relation 'gloss'. */
//	private static final double GLOSS_WEIGHT = 0.6;
//	/** Weight for the relation 'rgloss'. */
//	private static final double RGLOSS_WEIGHT = 0.2;
	
	// relations for verbs
	/** Weight for the relation 'entailing'. */
	private static final double ENTAILING_WEIGHT = 0.7;
	/** Weight for the relation 'causing'. */
	private static final double CAUSING_WEIGHT = 0.5;
	
	// relations for nouns
	/** Weight for the relation 'member-of'. */
	private static final double MEMBER_OF_WEIGHT = 0.5;
	/** Weight for the relation 'substance-of'. */
	private static final double SUBSTANCE_OF_WEIGHT = 0.5;
	/** Weight for the relation 'part-of'. */
	private static final double PART_OF_WEIGHT = 0.5;
	/** Weight for the relation 'has-member'. */
	private static final double HAS_MEMBER_WEIGHT = 0.5;
	/** Weight for the relation 'has-substance'. */
	private static final double HAS_SUBSTANCE_WEIGHT = 0.5;
	/** Weight for the relation 'has-part'. */
	private static final double HAS_PART_WEIGHT = 0.5;
	
	// a cache for looking up lemmas of words. Generally this operation is
	// very slow so we use a cache
	protected static HashMap<String, String> word2lemma;
	
	public static enum WordnetRelationship {
		HYPERNYM, HYPONYM, SYNONYM, /*DERIVED,*/ ENTAILING, CAUSING,
		MEMBER_OF, HAS_MEMBER, SUBSTANCE_OF, HAS_SUBSTANCE, 
		PART_OF, HAS_PART
	}
	
	public static WordnetRelationship[] getAllRelations() {
		return WordnetRelationship.values();
	}
	
	// relations for adjectives and adverbs
//	/** Weight for the relation 'pertainym'. */
//	private static final double PERTAINYM_WEIGHT = 0.5;
	
	/** WordNet dictionary. */
	private static Dictionary dict = create();
	
	/**
	 * Initializes the wrapper for the WordNet dictionary.
	 */
	public static Dictionary create() {
		return create(true);
	}
	

	public static Dictionary create(boolean useSnow) {
		word2lemma = new HashMap<String, String>();
		if (dict == null)
		try {
			// String fileName;
			// if (useSnow)
			// 	fileName = FileManager.getResource("resources/wordnet-snow/ewn.xml");
			// else
			// 	fileName = FileManager.getResource("resources/wordnet/ewn.xml");
			//JWNL.initialize(new FileInputStream(fileName));
			
			// dict = net.didion.jwnl.dictionary.Dictionary.getInstance();
			// dict = Dictionary.getInstance(new FileInputStream(fileName));
			dict = Dictionary.getDefaultResourceInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dict;
	}
	
	/**
	 * Checks if the word exists in WordNet.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is in WordNet
	 */
	public static boolean isWord(String word) {
		if (dict == null) return false;
		
		IndexWordSet indexWordSet = null;
		try {
			indexWordSet = dict.lookupAllIndexWords(word);
		} catch (JWNLException e) {}
		
		return indexWordSet.size() > 0;
	}
	
	/**
	 * Checks if the word exists in WordNet. Supports multi-token terms.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is in WordNet
	 */
	public static boolean isCompoundWord(String word) {
		if (dict == null) return false;
		
		// do not look up words with special characters other than '.'
		if (word.matches(".*?[^\\w\\s\\.].*+")) return false;
		
		IndexWordSet indexWordSet = null;
		try {
			indexWordSet = dict.lookupAllIndexWords(word);
		} catch (JWNLException e) {}
		
		// ensure that the word, and not just a substring, was found in WordNet
		int wordTokens = word.split("\\s", -1).length;
		int wordDots = word.split("\\.", -1).length;
		for (IndexWord indexWord : indexWordSet.getIndexWordArray()) {
			String lemma = indexWord.getLemma();
			int lemmaTokens = lemma.split("\\s", -1).length;
			int lemmaDots = lemma.split("\\.", -1).length;
			if (wordTokens == lemmaTokens && wordDots == lemmaDots) return true;
		}
		return false;
	}
	
	/**
	 * Checks if the word exists as an adjective.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is an adjective
	 */
	public static boolean isAdjective(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
		    if (word.length() > 20) return false;
			indexWord = dict.lookupIndexWord(POS.ADJECTIVE, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Checks if the word exists as an adverb.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is an adverb
	 */
	public static boolean isAdverb(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
		    if (word.length() > 20) return false;
			indexWord = dict.lookupIndexWord(POS.ADVERB, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Checks if the word exists as a noun.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is a noun
	 */
	public static boolean isNoun(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
		    if (word.length() > 20) return false;
			indexWord = dict.lookupIndexWord(POS.NOUN, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Checks if the word exists as a noun. Supports multi-token terms.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is a noun
	 */
	public static boolean isCompoundNoun(String word) {
		if (dict == null) return false;
		
		// do not look up words with special characters other than '.'
		if (word.matches(".*?[^\\w\\s\\.].*+")) return false;
		
		IndexWord indexWord = null;
		try {
		    if (word.length() > 20) return false;
			indexWord = dict.lookupIndexWord(POS.NOUN, word);
		} catch (JWNLException e) {}
		if (indexWord == null) return false;
		
		// ensure that the word, and not just a substring, was found in WordNet
		int wordTokens = word.split("\\s", -1).length;
		int wordDots = word.split("\\.", -1).length;
		String lemma = indexWord.getLemma();
		int lemmaTokens = lemma.split("\\s", -1).length;
		int lemmaDots = lemma.split("\\.", -1).length;
		return wordTokens == lemmaTokens && wordDots == lemmaDots;
	}
	
	/**
	 * Checks if the word exists as a verb.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is a verb
	 */
	public static boolean isVerb(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
		    if (word.length() > 20) return false;
			indexWord = dict.lookupIndexWord(POS.VERB, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Looks up the lemma of a word.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return lemma or <code>null</code> if lookup failed
	 */
	public static String getLemma(String word, POS pos) {
		if (dict == null) return null;
		
		String key = word+"#"+pos.getLabel();
		if (word2lemma.containsKey(key))
			return word2lemma.get(key);
		
		// code hang at the following "word":
		// JKEpNTGEDiiQml2Tm53HxlpeX65XkF2RW6CXn57IAJTLyi0u4RIFCxSX5RZmpxfoIJkg6Lz8llUu4uCA1NSWpNLcgvqAiPjsjMS8vkRkoWZCZwiXg5Gli5Odp4hrs7Ojl6GdgAdJVUJaZwnabb14gE5BTUsylqQ91D8KaEKMAw1BHc08D0xAvE18vTxP9AmMTMxOQsaVFOQBJuTiO0QAAAA
		// don't know why, just adding a length check.
		if (word.length() > 20) return word;
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(pos, word);
		} catch (JWNLException e) {}
		if (indexWord == null) return word;
		
		String lemma = indexWord.getLemma();
		lemma = lemma.replace("_", " ");
		
		word2lemma.put(key, lemma);
		return lemma;
	}
	
	/**
	 * look up the lemma of a word given its POS tags. Snowball Stemmer is called if the word isn't any
	 * of noun/verb/adj/adv.
	 * @param words the word itself
	 * @param tags pos tags
	 * @return a list of lemmas for the word
	 */
	public static String[] getLemmas(String[] words, String[] tags) {
		String[] lemmas = new String[words.length];
		
		for (int i=0; i<words.length; i++) {
			POS pos = null;
			pos = stringPos2POS(tags[i]);

			if (pos != null)
				lemmas[i] = getLemma(words[i], pos);
			else
				lemmas[i] = SnowballStemmer.stem(words[i]);
		}
		return lemmas;
	}
	
	public static String getLemma(String word, String tag) {
		
		String lemma;
		POS pos = null;
		pos = stringPos2POS(tag);

		if (pos != null)
			lemma = getLemma(word, pos);
		else
			lemma = SnowballStemmer.stem(word);
		return lemma;
	}
	
//	public static void writeLemmasForAllWords(String fileName) throws JWNLException, IOException {
//		POS[] pos_list = new POS[]{POS.NOUN, POS.VERB, POS.ADJECTIVE, POS.ADVERB};
//		String lemma;
//		BufferedWriter writer = FileManager.getWriter(fileName);
//		for (POS pos:pos_list) {
//			Iterator<IndexWord> ite = dict.getIndexWordIterator(pos);
//			while (ite.hasNext()) {
//				IndexWord word = ite.next();
//				lemma = word.getLemma();
//				writer.write(pos.getLabel() + "\t" + word.toString() + "\t" + lemma + "\n");
//			}
//		}
//	}
	
	/**
	 * Given a pos tag (nnp,rb,vbz, etc), return a POS object if it's a noun/verb/adjective/adverb,
	 * or null if it's none of the above
	 * @param tag a POS string from a tagger
	 * @return a wordnet POS object, or null if it's not a noun/verb/adjective/adverb
	 */
	public static POS stringPos2POS(String tag) {
		POS pos = null;
		// http://www.ims.uni-stuttgart.de/projekte/CorpusWorkbench/CQP-HTMLDemo/PennTreebankTS.html
		if (tag.toUpperCase().startsWith("NN"))
			pos = NOUN;
		else if (tag.toUpperCase().startsWith("VB"))
			pos = VERB;
		else if (tag.toUpperCase().startsWith("JJ"))
			pos = ADJECTIVE;
		else if (tag.toUpperCase().startsWith("RB") || tag.toUpperCase().endsWith("RB"))
			// RB, RBR, RBS, WRB
			pos = ADVERB;
		return pos;
	}
	
	/**
	 * Looks up the lemma of a compound word.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return lemma or <code>null</code> if lookup failed
	 */
	public static String getCompoundLemma(String word, POS pos) {
		// do not look up words with special characters other than '.'
		if (word.matches(".*?[^\\w\\s\\.].*+")) return null;
		
		String lemma = getLemma(word, pos);
		if (lemma == null) return null;
		
		// ensure that the word, and not just a substring, was found in WordNet
		int wordTokens = word.split("\\s", -1).length;
		int wordDots = word.split("\\.", -1).length;
		int lemmaTokens = lemma.split("\\s", -1).length;
		int lemmaDots = lemma.split("\\.", -1).length;
		if (wordTokens != lemmaTokens || wordDots != lemmaDots) return null;
		
		return lemma;
	}
	
	/**
	 * Looks up the most common synset of a word.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return synset or <code>null</code> if lookup failed
	 */
	private static Synset getCommonSynset(String word, POS pos) {
		if (dict == null) return null;
		if (pos == null) return null;
		Synset synset = null;
		try {
		    if (word.length() > 20) return null;
			IndexWord indexWord = dict.lookupIndexWord(pos, word);
			if (indexWord == null) return null;
			// senses are sorted by the usage count from small to big
			synchronized (indexWord) {
			    indexWord.sortSenses();
			    List<Synset> list = indexWord.getSenses();
			    synset = list.get(list.size()-1);
			}
		  // System.out.println("common synset for word: " + word);
			// for (Synset s : list) {
			//   System.out.println(s);
			// }
		  // System.out.println();
		} catch (JWNLException e) {}
		
		return synset;
	}
	
	private static HashSet<Synset> getAllSynset(String word) {
		HashSet<Synset> synset = new HashSet<Synset>();
		try {
			IndexWordSet ws = dict.lookupAllIndexWords(word);
			for (IndexWord iw: ws.getIndexWordArray()) {
				for (Synset s:iw.getSenses())
					synset.add(s);
			}
		} catch (JWNLException e) {}
		return synset;
	}
	
	private static HashSet<Synset> getAllSynset(String word, POS pos) {
		HashSet<Synset> synset = new HashSet<Synset>();
		if (dict == null) return synset;
		if (pos == null) return synset;
		
		try {
		    if (word.length() > 20) return new HashSet<Synset>();
			IndexWord indexWord = dict.lookupIndexWord(pos, word);
			if (indexWord == null) return getAllSynset(word);
			for (Synset s:indexWord.getSenses())
				synset.add(s);
		} catch (JWNLException e) {}
		
		return synset;
	}
	
	/**
	 * Looks up the synsets that correspond to the nodes in a node list.
	 * 
	 * @param nodes node list
	 * @return synsets
	 */
	private static Synset[] getSynsets(PointerTargetNodeList nodes) {
		Synset[] synsets = new Synset[nodes.size()];
		
		for (int i = 0; i < nodes.size(); i++) {
			PointerTargetNode node  = (PointerTargetNode) nodes.get(i);
			synsets[i] = node.getSynset();
		}
		
		return synsets;
	}
	
	private static HashSet<Synset> getSynsetsInSet(PointerTargetNodeList nodes) {
		HashSet<Synset> synsets = new HashSet<Synset>();
		
		for (int i = 0; i < nodes.size(); i++) {
			PointerTargetNode node  = (PointerTargetNode) nodes.get(i);
			synsets.add(node.getSynset());
		}
		
		return synsets;
	}
	
	/**
	 * Looks up the lemmas of the words in a synset.
	 * 
	 * @param synset a synset
	 * @return lemmas
	 */
	private static String[] getLemmas(Synset synset) {
		List<Word> words = synset.getWords();
		String[] lemmas = new String[words.size()];
		
		for (int i = 0; i < words.size(); i++) {
			lemmas[i] = words.get(i).getLemma();
			lemmas[i] = lemmas[i].replace("_", " ");
		}
		
		return lemmas;
	}
	
	/**
	 * Looks up the lemmas of the words in all synsets.
	 * 
	 * @param synsets the synsets
	 * @return lemmas
	 */
	private static String[] getLemmas(Synset[] synsets) {
		HashSet<String> lemmaSet = new HashSet<String>();
		
		for (Synset synset : synsets) {
			String[] lemmas = getLemmas(synset);
			for (String lemma : lemmas) lemmaSet.add(lemma);
		}
		
		return lemmaSet.toArray(new String[lemmaSet.size()]);
	}
	
	private static HashSet<String> getLemmasSet(Synset[] synsets) {
		HashSet<String> lemmaSet = new HashSet<String>();
		
		for (Synset synset : synsets) {
			String[] lemmas = getLemmas(synset);
			for (String lemma : lemmas) lemmaSet.add(lemma);
		}
		
		return lemmaSet;
	}
	
	// relations for multiple parts of speech
	
	/**
	 * Check whether two words share the same synset
	 */
	public static boolean sameSynset(String word1, String pos1, String word2, String pos2) {
		Synset s1 = getCommonSynset(word1, stringPos2POS(pos1));
		Synset s2 = getCommonSynset(word2, stringPos2POS(pos2));
		if (s1 != null && s2 != null && s1.equals(s2))
//		if (s1.getGloss().equals(s2.getGloss()))
			return true;
		else
			return false;
	}
	
	/**
	 * Looks up synonyms of the given word, assuming that it is used in its most
	 * common sense.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return synonyms or <code>null</code> if lookup failed
	 */
	public static String[] getSynonyms(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		return getLemmas(synset);
	}
	
	// loop through all POS
	public static String[] getSynonyms(String word) {
		ArrayList<Synset> results = new ArrayList<Synset>();
		for (POS pos:(List<POS>)POS.getAllPOS()) {
			Synset synset = getCommonSynset(word, pos);
			if (synset == null) continue;
			
			results.add(synset);
		}
		
		return getLemmas(results.toArray(new Synset[results.size()]));
	}
	
	public static HashSet<String> getSynonymsSet(String word, String pos) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getSynonyms(word, stringPos2POS(pos));
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
		
	public static HashSet<String> getSynonymsSet(String word) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getSynonyms(word);
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
	public static HashSet<String> getAllSynonyms(String word, String pos) {
		HashSet<Synset> synset = getAllSynset(word, stringPos2POS(pos));
		if (synset == null || synset.size() == 0) return new HashSet<String>();
		
		HashSet<String> synonyms = new HashSet<String>();
		for (Synset syn:synset) {
			for (String s:getLemmas(syn))
				synonyms.add(s);
		}
		return synonyms;
	}
	
	/**
	 * Looks up hypernyms of the given word, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return hypernyms or <code>null</code> if lookup failed
	 */
	public static String[] getHypernyms(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		Synset[] hypernyms = getHypernymSynsets(synset);
		if (hypernyms == null) return null;
		
		return getLemmas(hypernyms);
	}
	
	// loop through all POS
	public static String[] getHypernyms(String word) {
		ArrayList<Synset> results = new ArrayList<Synset>();
		for (POS pos:(List<POS>)POS.getAllPOS()) {
			Synset synset = getCommonSynset(word, pos);
			if (synset == null) continue;
			
			Synset[] hypernyms = getHypernymSynsets(synset);
			if (hypernyms == null) continue;
			for (Synset x:hypernyms)
				results.add(x);
		}
		
		return getLemmas(results.toArray(new Synset[results.size()]));
	}
	
	public static HashSet<String> getHypernymsSet(String word, String pos) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getHypernyms(word, stringPos2POS(pos));
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
	
		
	// loop through all POS
	public static HashSet<String> getHypernymsSet(String word) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getHypernyms(word);
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
	
	/**
	 * Retrieve the hypernym with a depth of <code>level</code>. The top of hierarchy (entity) has
	 * level 0.
	 * @param word
	 * @param pos
	 * @param level
	 * @return
	 */
	public static String getHypernymByLevel(String word, String pos, int level) {
		Synset synset = getCommonSynset(word, stringPos2POS(pos));
		if (synset == null) return null;
		ArrayList<Synset> list = new ArrayList<Synset>();
		HashSet<Synset> set = new HashSet<Synset>();
		Synset[] hypernyms = getHypernymSynsets(synset);
		while (hypernyms != null && hypernyms.length > 0) {
			if (set.contains(hypernyms[0]))
				break;
			else
				set.add(hypernyms[0]);
			list.add(hypernyms[0]);
			hypernyms = getHypernymSynsets(hypernyms[0]);
		}
		if (list.size() > level)
			return getLemmas(list.get(list.size() - level  - 1))[0].replaceAll(" ", "_");
		else if (list.size() > 0)
			return getLemmas(list.get(0))[0].replaceAll(" ", "_");
		else return null;
	}

	/**
	 * returns the lemma of all hypernyms (all the way up to 'entity') of the most common synset of a word
	 * @param word a word, not in lemma
	 * @param pos POS tag of this word
	 * @return a set of hypernyms in lemma
	 */
	public static HashSet<String> getAllCommonHypernyms(String word, String pos) {
		Synset synset = getCommonSynset(word, stringPos2POS(pos));
		if (synset == null) return new HashSet<String>();
		
		HashSet<String> hypernymSet = new HashSet<String>();
		HashSet<Synset> hypernyms = getAllHypernymSynsets(synset);
		if (hypernyms == null) return new HashSet<String>();
		for (Synset syn:hypernyms)
			for (String s:getLemmas(syn))
				hypernymSet.add(s);
		
		return hypernymSet;
	}

	public static boolean isHypernymWithDirection(String hyper, String hyperPos, 
								String hypo, String hypoPos) {
		HashSet<Synset> hyperSynset = getAllSynset(hyper, stringPos2POS(hyperPos));
		HashSet<Synset> hypoSynset = getAllSynset(hypo, stringPos2POS(hypoPos));
		for (Synset s:hypoSynset) {
			HashSet<Synset> set = getAllHypernymSynsets(s);
			set.retainAll(hyperSynset);
			if (set.size() > 0) return true;
		}
		return false;
	}
	
	public static boolean isHypernym(String w1, String pos1, String w2, String pos2) {
		if (isHypernymWithDirection(w1, pos1, w2, pos2) || isHypernymWithDirection(w2, pos2, w1, pos1))
			return true;
		return false;
	}
	
	/**
	 * returns the lemma of all hypernyms (all the way up to 'entity') of all synsets of a word
	 * @param word a word, not in lemma
	 * @param pos POS tag of this word
	 * @return a set of hypernyms in lemma
	 */
	public static HashSet<String> getAllHypernyms(String word, String pos) {
		HashSet<Synset> synset = getAllSynset(word, stringPos2POS(pos));
		if (synset == null || synset.size() == 0) return new HashSet<String>();
		
		HashSet<String> hypernymSet = new HashSet<String>();
		HashSet<Synset> hypernyms = new HashSet<Synset>();
		for (Synset s:synset) {
			HashSet<Synset> set = getAllHypernymSynsets(s);
			if (set != null)
				hypernyms.addAll(set);
		}
		if (hypernyms.size() == 0) return new HashSet<String>();
		for (Synset syn:hypernyms)
			for (String s:getLemmas(syn))
				hypernymSet.add(s);
		
		return hypernymSet;
	}
	
	// get 'hypernym' synsets
	private static Synset[] getHypernymSynsets(Synset synset) {
		PointerTargetNodeList hypernyms = null;
		try {
			hypernyms = PointerUtils.getDirectHypernyms(synset);
		} catch (JWNLException e) {}
		if (hypernyms == null) return null;
		
		return getSynsets(hypernyms);
	}
	
	/*
	public static HashSet<String> getDerivedSet(String word, String pos) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getDerived(word, stringPos2POS(pos));
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
	*/
	
	/*
	public static String[] getDerived(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		Synset[] derived = getDerivedSynsets(synset);
		if (derived == null) return null;
		
		return getLemmas(derived);
	}
	
	private static Synset[] getDerivedSynsets(Synset synset) {
		PointerTargetNodeList derived = null;
		try {
			derived = PointerUtils.getDerived(synset);
			//derived = new PointerTargetNodeList(synset.getTargets(new PointerType("DERIVED", "DERIVED_KEY", 1|2|4|8)));
			
			//PointerUtils.getInstance().
		} catch (JWNLException e) {}
		if (derived == null) return null;
		
		return getSynsets(derived);
	}
	*/
	
//	private static HashSet<Synset> getAllHypernymSynsets(Synset synset) {
//		PointerTargetNodeList hypernyms = null;
//		try {
//			hypernyms = PointerUtils.getInstance().getDirectHypernyms(synset);
//		} catch (JWNLException e) {}
//		if (hypernyms == null) return null;
//		HashSet<Synset> set = new HashSet<Synset>();
//		for (Synset s:getSynsetsInSet(hypernyms)) {
//			if (set.contains(s)) continue;
//			set.add(s);
//			HashSet<Synset> hySet = getAllHypernymSynsets(s);
//			if (hySet != null)
//				set.addAll(hySet);
//		}
//		return set;
//	}
	
	private static HashSet<Synset> getAllHypernymSynsets(Synset synset) {
		HashSet<Synset> set = new HashSet<Synset>();
		getAllHypernymSynsets(synset, set);
		return set;
	}
	
	private static void getAllHypernymSynsets(Synset synset, HashSet<Synset> set) {
		PointerTargetNodeList hypernyms = null;
		try {
			hypernyms = PointerUtils.getDirectHypernyms(synset);
		} catch (JWNLException e) {}
		if (hypernyms == null) return;

		for (Synset s:getSynsetsInSet(hypernyms)) {
			if (set.contains(s)) continue;
			set.add(s);
			getAllHypernymSynsets(s, set);
		}
	}
	
	public static boolean isOfRelationWithDirection(String hyper, String hyperPos, 
								String hypo, String hypoPos, WordnetRelationship relation) {
		HashSet<Synset> hyperSynset = getAllSynset(hyper, stringPos2POS(hyperPos));
		HashSet<Synset> hypoSynset = getAllSynset(hypo, stringPos2POS(hypoPos));
		for (Synset s:hypoSynset) {
			HashSet<Synset> set = getAllSynsetsOfRelation(s, relation);
			set.retainAll(hyperSynset);
			if (set.size() > 0) return true;
		}
		return false;
	}
	
	public static boolean isOfRelation(String w1, String pos1, String w2, String pos2, WordnetRelationship relation) {
		if (isOfRelationWithDirection(w1, pos1, w2, pos2, relation) || isOfRelationWithDirection(w2, pos2, w1, pos1, relation))
			return true;
		return false;
	}

	
	public static HashSet<Synset> getAllSynsetsOfRelation(Synset synset, WordnetRelationship relation) {
		HashSet<Synset> set = new HashSet<Synset>();
		getAllSynsetsOfRelation(synset, set, relation);
		return set;
	}
	
	public static void getAllSynsetsOfRelation(Synset synset, HashSet<Synset> set, WordnetRelationship relation) {
		PointerTargetNodeList hypernyms = null;
		try {
			switch (relation) {
				case HYPERNYM: 
					hypernyms = PointerUtils.getDirectHypernyms(synset); break;
				case HYPONYM:
					hypernyms = PointerUtils.getDirectHyponyms(synset); break;
				case SYNONYM:
					hypernyms = PointerUtils.getSynonyms(synset); break;
				case CAUSING:
					hypernyms = PointerUtils.getCauses(synset); break;
				case ENTAILING:
					hypernyms = PointerUtils.getEntailments(synset); break;
				// case DERIVED:
				//	hypernyms = PointerUtils.getDerived(synset); break;
				case MEMBER_OF:
					hypernyms = PointerUtils.getMemberMeronyms(synset); break;
				case HAS_MEMBER:
					hypernyms = PointerUtils.getMemberHolonyms(synset); break;
				case SUBSTANCE_OF:
					hypernyms = PointerUtils.getSubstanceMeronyms(synset); break;
				case HAS_SUBSTANCE:
					hypernyms = PointerUtils.getSubstanceHolonyms(synset); break;
				case PART_OF:
					hypernyms = PointerUtils.getPartMeronyms(synset); break;
				case HAS_PART:
					hypernyms = PointerUtils.getPartHolonyms(synset); break;
					
			}
		} catch (JWNLException e) {}
		if (hypernyms == null) return;

		for (Synset s:getSynsetsInSet(hypernyms)) {
			if (set.contains(s)) continue;
			set.add(s);
			getAllSynsetsOfRelation(s, set, relation);
		}
	}	
	
	/**
	 * Looks up hyponyms of the given word, assuming that it is used in its most
	 * common sense.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return hyponyms or <code>null</code> if lookup failed
	 */
	public static String[] getHyponyms(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		Synset[] hyponyms = getHyponymSynsets(synset);
		if (hyponyms == null) return null;
		
		return getLemmas(hyponyms);
	}
	
	// loop through all POS
	public static String[] getHyponyms(String word) {
		ArrayList<Synset> results = new ArrayList<Synset>();
		for (POS pos:(List<POS>)POS.getAllPOS()) {
			Synset synset = getCommonSynset(word, pos);
			if (synset == null) continue;
			
			Synset[] hyponyms = getHyponymSynsets(synset);
			if (hyponyms == null) continue;
			for (Synset x:hyponyms)
				results.add(x);
		}
		
		return getLemmas(results.toArray(new Synset[results.size()]));
	}
	
	public static HashSet<String> getHyponymsSet(String word, String pos) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getHyponyms(word, stringPos2POS(pos));
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
		
	public static HashSet<String> getHyponymsSet(String word) {
		HashSet<String> set = new HashSet<String>();
		String[] ret = getHyponyms(word);
		if (ret != null) {
			for (String s:ret)
				set.add(s);
		}
		return set;
	}
	/**
	 * Looks up hyponyms of the synset with the given POS and offset.
	 * 
	 * @param pos POS of the synset
	 * @param offset offset of the synset
	 * @return hyponyms or <code>null</code> if lookup failed
	 */
	public static String[] getHyponyms(POS pos, long offset) {
		Synset synset = null;
		try {
			synset = dict.getSynsetAt(pos, offset);
		} catch (JWNLException e) {}
		if (synset == null) return null;
		
		Synset[] hyponyms = getHyponymSynsets(synset);
		if (hyponyms == null) return null;
		
		return getLemmas(hyponyms);
	}
	
	/**
	 * Looks up hyponyms of the synset with POS "noun" and the given offset.
	 * 
	 * @param offset offset of the synset
	 * @return hyponyms or <code>null</code> if lookup failed
	 */
	public static String[] getNounHyponyms(long offset) {
		return getHyponyms(POS.NOUN, offset);
	}
	
	// get 'hyponym' synsets
	private static Synset[] getHyponymSynsets(Synset synset) {
		PointerTargetNodeList hyponyms = null;
		try {
			hyponyms = PointerUtils.getDirectHyponyms(synset);
		} catch (JWNLException e) {}
		if (hyponyms == null) return null;
		
		return getSynsets(hyponyms);
	}
	
	public static HashSet<String> getAllHyponyms(String word, String pos) {
		HashSet<Synset> synset = getAllSynset(word, stringPos2POS(pos));
		if (synset == null || synset.size() == 0) return new HashSet<String>();
		
		HashSet<String> hyponymSet = new HashSet<String>();
		HashSet<Synset> hyponyms = new HashSet<Synset>();
		for (Synset s:synset) {
			HashSet<Synset> set = getAllHyponymSynsets(s);
			if (set != null)
				hyponyms.addAll(set);
		}
		if (hyponyms.size() == 0) return new HashSet<String>();
		for (Synset syn:hyponyms)
			for (String s:getLemmas(syn))
				hyponymSet.add(s);
		
		return hyponymSet;
	}
	
	private static HashSet<Synset> getAllHyponymSynsets(Synset synset) {
		HashSet<Synset> set = new HashSet<Synset>();
		getAllHyponymSynsets(synset, set);
		return set;
	}

	private static void getAllHyponymSynsets(Synset synset, HashSet<Synset> set) {
		PointerTargetNodeList hyponyms = null;
		try {
			hyponyms = PointerUtils.getDirectHyponyms(synset);
		} catch (JWNLException e) {}
		if (hyponyms == null) return;

		for (Synset s:getSynsetsInSet(hyponyms)) {
			if (set.contains(s)) continue;
			set.add(s);
			getAllHyponymSynsets(s, set);
		}
	}

	// relations for verbs
	
	/**
	 * Looks up verbs that entail the given verb, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param verb a verb
	 * @return entailing verbs or <code>null</code> if lookup failed
	 */
	public static String[] getEntailing(String verb) {
		Synset synset = getCommonSynset(verb, VERB);
		if (synset == null) return null;
		
		Synset[] entailing = getEntailingSynsets(synset);
		if (entailing == null) return null;
		
		return getLemmas(entailing);
	}
	
	public static HashSet<String> getEntailingSet(String verb) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getEntailing(verb);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
	
	public static HashSet<String> getEntailingSet(String verb, String pos) {
		if (!pos.toLowerCase().startsWith("vb"))
			return new HashSet<String>();
		HashSet<String> set = new HashSet<String>();
		String[] ss = getEntailing(verb);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
	
	// get 'entailing' synsets
	private static Synset[] getEntailingSynsets(Synset synset) {
		PointerTargetNodeList entailing = null;
		try {
			entailing = PointerUtils.getEntailments(synset);
		} catch (JWNLException e) {}
		if (entailing == null) return null;
		
		return getSynsets(entailing);
	}
	
	/**
	 * Looks up verbs that cause the given verb, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param verb a verb
	 * @return causing verbs or <code>null</code> if lookup failed
	 */
	public static String[] getCausing(String verb) {
		Synset synset = getCommonSynset(verb, VERB);
		if (synset == null) return null;
		
		Synset[] causing = getCausingSynsets(synset);
		if (causing == null) return null;
		
		return getLemmas(causing);
	}
		
	public static HashSet<String> getCausingSet(String verb) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getCausing(verb);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
			
	public static HashSet<String> getCausingSet(String verb, String pos) {
		if (!pos.toLowerCase().startsWith("vb"))
			return new HashSet<String>();
		return getCausingSet(verb);
	}
	
	// get 'causing' synsets
	private static Synset[] getCausingSynsets(Synset synset) {
		PointerTargetNodeList causing = null;
		try {
			causing = PointerUtils.getCauses(synset);
		} catch (JWNLException e) {}
		if (causing == null) return null;
		
		return getSynsets(causing);
	}
	
	// relations for nouns
	
	/**
	 * Looks up member holonyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return member holonyms or <code>null</code> if lookup failed
	 */
	public static String[] getMembersOf(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] membersOf = getMemberOfSynsets(synset);
		if (membersOf == null) return null;
		
		return getLemmas(membersOf);
	}
	
	public static HashSet<String> getMembersOfSet(String noun) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getMembersOf(noun);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
		
	public static HashSet<String> getMembersOfSet(String noun, String pos) {
		if (!pos.toLowerCase().startsWith("nn"))
			return new HashSet<String>();
		return getMembersOfSet(noun);
	}
	
	// get 'member-of' synsets
	private static Synset[] getMemberOfSynsets(Synset synset) {
		PointerTargetNodeList membersOf = null;
		try {
			membersOf = PointerUtils.getMemberHolonyms(synset);
		} catch (JWNLException e) {}
		if (membersOf == null) return null;
		
		return getSynsets(membersOf);
	}
	
	/**
	 * Looks up substance holonyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return substance holonyms or <code>null</code> if lookup failed
	 */
	public static String[] getSubstancesOf(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] substancesOf = getSubstanceOfSynsets(synset);
		if (substancesOf == null) return null;
		
		return getLemmas(substancesOf);
	}
	
	public static HashSet<String> getSubstancesOfSet(String noun) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getSubstancesOf(noun);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
			
	public static HashSet<String> getSubstancesOfSet(String noun, String pos) {
		if (!pos.toLowerCase().startsWith("nn"))
			return new HashSet<String>();
		return getSubstancesOfSet(noun);
	}
	
	// get 'substance-of' synsets
	private static Synset[] getSubstanceOfSynsets(Synset synset) {
		PointerTargetNodeList substancesOf = null;
		try {
			substancesOf = PointerUtils.getSubstanceHolonyms(synset);
		} catch (JWNLException e) {}
		if (substancesOf == null) return null;
		
		return getSynsets(substancesOf);
	}
	
	/**
	 * Looks up part holonyms of the given noun, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param noun a noun
	 * @return part holonyms or <code>null</code> if lookup failed
	 */
	public static String[] getPartsOf(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] partsOf = getPartOfSynsets(synset);
		if (partsOf == null) return null;
		
		return getLemmas(partsOf);
	}
	
	public static HashSet<String> getPartsOfSet(String noun) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getPartsOf(noun);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
				
	public static HashSet<String> getPartsOfSet(String noun, String pos) {
		if (!pos.toLowerCase().startsWith("nn"))
			return new HashSet<String>();
		return getPartsOfSet(noun);
	}
	
	// get 'part-of' synsets
	private static Synset[] getPartOfSynsets(Synset synset) {
		PointerTargetNodeList partsOf = null;
		try {
			partsOf = PointerUtils.getPartHolonyms(synset);
		} catch (JWNLException e) {}
		if (partsOf == null) return null;
		
		return getSynsets(partsOf);
	}
	
	/**
	 * Looks up member meronyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return member meronyms or <code>null</code> if lookup failed
	 */
	public static String[] getHaveMember(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] haveMember = getHasMemberSynsets(synset);
		if (haveMember == null) return null;
		
		return getLemmas(haveMember);
	}
	
	public static HashSet<String> getHaveMemberSet(String noun) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getHaveMember(noun);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
					
	public static HashSet<String> getHaveMemberSet(String noun, String pos) {
		if (!pos.toLowerCase().startsWith("nn"))
			return new HashSet<String>();
		return getHaveMemberSet(noun);
	}
	
	// get 'has-member' synsets
	private static Synset[] getHasMemberSynsets(Synset synset) {
		PointerTargetNodeList haveMember = null;
		try {
			haveMember = PointerUtils.getMemberMeronyms(synset);
		} catch (JWNLException e) {}
		if (haveMember == null) return null;
		
		return getSynsets(haveMember);
	}
	
	/**
	 * Looks up substance meronyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return substance meronyms or <code>null</code> if lookup failed
	 */
	public static String[] getHaveSubstance(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] haveSubstance = getHasSubstanceSynsets(synset);
		if (haveSubstance == null) return null;
		
		return getLemmas(haveSubstance);
	}
		
	public static HashSet<String> getHaveSubstanceSet(String noun) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getHaveSubstance(noun);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
						
	public static HashSet<String> getHaveSubstanceSet(String noun, String pos) {
		if (!pos.toLowerCase().startsWith("nn"))
			return new HashSet<String>();
		return getHaveSubstanceSet(noun);
	}
	
	// get 'has-substance' synsets
	private static Synset[] getHasSubstanceSynsets(Synset synset) {
		PointerTargetNodeList haveSubstance = null;
		try {
			haveSubstance = PointerUtils.getSubstanceMeronyms(synset);
		} catch (JWNLException e) {}
		if (haveSubstance == null) return null;
		
		return getSynsets(haveSubstance);
	}
	
	/**
	 * Looks up part meronyms of the given noun, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param noun a noun
	 * @return part meronyms or <code>null</code> if lookup failed
	 */
	public static String[] getHavePart(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] havePart = getHasPartSynsets(synset);
		if (havePart == null) return null;
		
		return getLemmas(havePart);
	}
		
	public static HashSet<String> getHavePartSet(String noun) {
		HashSet<String> set = new HashSet<String>();
		String[] ss = getHavePart(noun);
		if (ss != null) {
			for (String s:ss)
				set.add(s);
		}
		return set;
	}
							
	public static HashSet<String> getHavePartSet(String noun, String pos) {
		if (!pos.toLowerCase().startsWith("nn"))
			return new HashSet<String>();
		return getHavePartSet(noun);
	}
	
	// get 'has-part' synsets
	private static Synset[] getHasPartSynsets(Synset synset) {
		PointerTargetNodeList havePart = null;
		try {
			havePart = PointerUtils.getPartMeronyms(synset);
		} catch (JWNLException e) {}
		if (havePart == null) return null;
		
		return getSynsets(havePart);
	}
	
	// implement the interface 'Ontology'
	
	/**
	 * Looks up a word.
	 * 
	 * @param word the word to look up
	 * @return <code>true</code> iff the word was found
	 */
	public boolean contains(String word) {
//		// look for compound nouns and verbs
//		return isCompoundWord(word);
		// only look for compound nouns
		return isCompoundNoun(word);
	}
	
	public static void main(String[] args) throws JWNLException, IOException {
		WordNet.create();
		
		
		System.out.println(WordNet.isHypernym("squirrel", "nn", "animals", "nns"));
		
		HashSet<String> set = WordNet.getAllHypernyms("squirrel", "nn");
		System.out.println(set);
		set = WordNet.getHypernymsSet("squirrel", "nn");
		System.out.println(set);

		System.out.println();
		System.out.println(WordNet.isHypernym("apple", "nn", "fruit", "nn"));
		set = WordNet.getAllHypernyms("apple", "nn");
		System.out.println(set);
		set = WordNet.getHypernymsSet("apple", "nn");
		System.out.println(set);

		System.out.println();
		System.out.println(WordNet.isHypernym("girl", "nn", "person", "nn"));
		set = WordNet.getAllHypernyms("girl", "nn");
		System.out.println(set);
		set = WordNet.getHypernymsSet("girl", "nn");
		System.out.println(set);
		
		set = WordNet.getSynonymsSet("like", "vb");
		System.out.println(set);

		
		System.out.println(WordNet.isOfRelation("tree", "nn", "trunk", "nn", WordnetRelationship.HAS_PART));
		System.out.println(WordNet.isOfRelation("kill", "vb", "die", "vb", WordnetRelationship.CAUSING));
		// System.out.println(WordNetThreadSafe.isOfRelation("kill", "vb", "die", "vb", WordnetRelationship.DERIVED));
		System.out.println(WordNet.isOfRelation("like", "vb", "love", "vb", WordnetRelationship.SYNONYM));
		System.out.println(WordNet.isOfRelation("squirrel", "nn", "animals", "nns", WordnetRelationship.HYPERNYM));
		System.out.println(WordNet.getAllHyponyms("rock", "nn"));
		//HashSet<String> set = WordNet.getAllHyponyms("rock", "nn");
//		HashSet<String> set = WordNet.getAllHypernyms("kept", "vbn");
//		System.out.println(WordNet.getHypernymByLevel("gagged", "vbn",4));
//		System.out.println(WordNet.sameSynset("red", "nn", "cherry-red", "nn"));
//		System.out.println(WordNet.getDerivedSet("player", "nn"));
//		System.out.println(WordNet.getDerivedSet("play", "vb"));
//		System.out.println(WordNet.getDerivedSet("hitter", "nn"));
//		System.out.println(WordNet.getDerivedSet("hit", "vbz"));
//		System.out.println(WordNet.getDerivedSet("lover", "nn"));
//		System.out.println(WordNet.getDerivedSet("love", "vb"));
//		System.out.println(WordNet.getDerivedSet("coldly", "rb"));
//		System.out.println(WordNet.getDerivedSet("cold", "adj"));
//		System.out.println(WordNet.getLemmas(new String[]{"JKEpNTGEDiiQml2Tm53HxlpeX65XkF2RW6CXn57IAJTLyi0u4RIFCxSX5RZmpxfoIJkg6Lz8llUu4uCA1NSWpNLcgvqAiPjsjMS8vkRkoWZCZwiXg5Gli5Odp4hrs7Ojl6GdgAdJVUJaZwnabb14gE5BTUsylqQ91D8KaEKMAw1BHc08D0xAvE18vTxP9AmMTMxOQsaVFOQBJuTiO0QAAAA"}, new String[]{"nn"}));
//		System.out.println(WordNet.getLemma("hold", "vb"));
//		System.out.println(WordNet.getLemma("held", "vbd"));
//		System.out.println(WordNet.getLemma("maker", "nn"));
//		System.out.println(WordNet.getLemma("makes", "vbz"));
//		System.out.println(SnowballStemmer.stem("maker"));
//		System.out.println(SnowballStemmer.stem("makes"));
//		System.out.println(WordNet.getLemma("executed", "vbz"));
//		System.out.println(WordNet.getLemma("execution", "nn"));
//		System.out.println(WordNet.getLemma("Australian", "adj"));
//		System.out.println(WordNet.getLemma("Australia", "nn"));
//		System.out.println(WordNet.getLemma("catch", "vb"));
//		System.out.println(WordNet.getLemma("caught", "vbd"));
//		System.out.println(WordNet.getLemma("is", "vb"));
//		System.out.println(WordNet.getLemma("was", "vbd"));
//		System.out.println(WordNet.getLemma("were", "vbd"));
//		System.out.println(WordNet.getLemma("has", "vb"));
//		System.out.println(WordNet.getLemma("had", "vbd"));
	}

}
