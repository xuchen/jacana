package edu.jhu.jacana.nlp;

import edu.jhu.jacana.util.RegexConverter;
import edu.jhu.jacana.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools13.coref.LinkerMode;
import opennlp.tools13.coref.mention.DefaultParse;
import opennlp.tools13.coref.mention.Mention;
import opennlp.tools13.lang.english.PosTagger;
import opennlp.tools13.lang.english.SentenceDetector;
import opennlp.tools13.lang.english.Tokenizer;
import opennlp.tools13.lang.english.TreebankChunker;
import opennlp.tools13.lang.english.TreebankLinker;
import opennlp.tools13.lang.english.TreebankParser;
import opennlp.tools13.parser.Parse;
import opennlp.tools13.parser.ParserME;
import opennlp.tools13.postag.POSDictionary;

import edu.jhu.jacana.util.FileManager;

/**
 * <p>This class provides a common interface to the
 * <a href="http://opennlp.sourceforge.net/">OpenNLP</a> toolkit.</p>
 * 
 * <p>It supports the following natural language processing tools:
 * <ul>
 * <li>Sentence detection</li>
 * <li>Tokenization/untokenization</li>
 * <li>Part of speech (POS) tagging</li>
 * <li>Chunking</li>
 * <li>Full parsing</li>
 * <li>Coreference resolution</li>
 * </ul>
 * </p>
 * 
 * @author Nico Schlaefer
 * @version 2006-05-20
 * 
 * @author Xuchen Yao
 * @version 2012-10-22
 * 
 * Added models trained on bio domain. Default to models trained on GENIA.
 * For general domain, use StanfordCoreNLP
 */
public class OpenNLP {
	public enum DOMAIN {
		GENIA,
		PENNBIO,
		GENERAL
	}
	/** Pattern for abundant blanks. More specific rules come first. T.b.c. */
	private static final Pattern ABUNDANT_BLANKS = Pattern.compile("(" +
		"\\d (st|nd|rd)\\b"			+ "|" +  // 1 st -> 1st
		"[A-Z] \\$"					+ "|" +  // US $ -> US$
		"\\d , \\d\\d\\d\\D"		+ "|" +  // 1 , 000 -> 1,000
		"\\d (\\.|:) \\d"			+ "|" +  // 1 . 99 -> 1.99
		"\\B(\\$|€|¢|£|¥|¤) \\d"	+ "|" +  // $ 100 -> $100
		"\\d (\\$|€|¢|£|¥|¤)"		+ "|" +  // 100 $ -> 100$
		" (-|/) "					+ "|" +  // one - third -> one-third
		"(\\(|\\[|\\{) "			+ "|" +  // ( ... ) -> (... )
		" (\\.|,|:|\\)|\\]|\\})"	+ ")");  // Prof . -> Prof.
	
	/** Sentence detector from the OpenNLP project. */
	private static SentenceDetector sentenceDetector;
	/** Tokenizer from the OpenNLP project. */
	private static Tokenizer tokenizer;
	/** Part of speech tagger from the OpenNLP project. */
	private static PosTagger tagger;
	/** Chunker from the OpenNLP project. */
	private static TreebankChunker chunker;
	/** Full parser from the OpenNLP project. */
	private static ParserME parser;
	/** Linker from the OpenNLP project. */
	private static TreebankLinker linker;
	
	protected static DOMAIN domain = DOMAIN.GENIA;
	
	public static void setDomain(DOMAIN _domain) {domain = _domain;}
	
	public static boolean createSentenceDetector() {
		if (domain == DOMAIN.GENIA)
			return createSentenceDetector(FileManager.getResource("resources/model/SentDetectGenia.bin.gz"));
		else if (domain == DOMAIN.PENNBIO)
			return createSentenceDetector(FileManager.getResource("resources/model/SentDetectPennBio.bin.gz"));
		else
			return createSentenceDetector(FileManager.getResource("resources/model/EnglishSD.bin.gz"));
	}
	/**
	 * Creates the sentence detector from a model file.
	 * 
	 * @param model model file
	 * @return true, iff the sentence detector was created successfully
	 */
	public static boolean createSentenceDetector(String model) {
		if (sentenceDetector == null) {
			try {
				sentenceDetector = new SentenceDetector(model);
			} catch (IOException e) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean createTokenizer() {
		if (domain == DOMAIN.GENIA)
			return createTokenizer(FileManager.getResource("resources/model/TokenizerGenia.bin.gz"));
		else if (domain == DOMAIN.PENNBIO)
			return createTokenizer(FileManager.getResource("resources/model/TokenizerPennBioIE.bin.gz"));
		else
			return createTokenizer(FileManager.getResource("resources/model/EnglishTok.bin.gz"));
	}
	/**
	 * Creates the tokenizer from a model file.
	 * 
	 * @param model model file
	 * @return true, iff the tokenizer was created successfully
	 */
	public static boolean createTokenizer(String model) {
		if (tokenizer == null){
			try {
				tokenizer = new Tokenizer(model);
			} catch (IOException e) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean createPosTagger() {
		if (domain == DOMAIN.GENIA)
			return createPosTagger(FileManager.getResource("resources/model/Tagger_Genia.bin.gz"), FileManager.getResource("resources/model/tagdict-genia"));
		else if (domain == DOMAIN.PENNBIO)
			return createPosTagger(FileManager.getResource("resources/model/Tagger_PennBio_bin.gz"), FileManager.getResource("resources/model/tagdictPennBioIE"));
		else
			return createPosTagger(FileManager.getResource("resources/model/tag.bin.gz"), FileManager.getResource("resources/model/tagdict"));
	}
	/**
	 * Creates the part of speech tagger from a model file and a case sensitive
	 * tag dictionary.
	 * 
	 * @param model model file
	 * @param tagdict case sensitive tag dictionary
	 * @return true, iff the POS tagger was created successfully
	 */
	public static boolean createPosTagger(String model, String tagdict) {
		if (tagger == null) {
			try {
				// create POS tagger, use case sensitive tag dictionary
				tagger = new PosTagger(model, new POSDictionary(tagdict, true));
			} catch (IOException e) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean createChunker() {
		if (domain == DOMAIN.GENIA)
			return createChunker(FileManager.getResource("resources/model/Chunker_Genia.bin.gz"));
		else if (domain == DOMAIN.PENNBIO)
			return createChunker(FileManager.getResource("resources/model/ChunkPennBioIE.bin.gz"));
		else
			return createChunker(FileManager.getResource("resources/model/EnglishChunk.bin.gz"));
	}
	/**
	 * Creates the chunker from a model file.
	 * 
	 * @param model model file
	 * @return true, iff the chunker was created successfully
	 */
	public static boolean createChunker(String model) {
		if (chunker == null) {
			try {
				chunker = new TreebankChunker(model);
			} catch (IOException e) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Creates the parser from a directory containing models.
	 * 
	 * @param dir model directory
	 * @return true, iff the parser was created successfully
	 */
	public static boolean createParser(String dir) {
		try {
			// create parser, use default beamSize and advancePercentage
			parser = TreebankParser.getParser(dir);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	/**
	 * Creates the linker from a directory containing models.
	 * 
	 * @param dir model directory
	 * @return true, iff the linker was created successfully
	 */
	public static boolean createLinker(String dir) {
		try {
			// create linker that works on unannotated text (TEST mode)
		    linker = new TreebankLinker(dir, LinkerMode.TEST);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Splits a text into sentences.
	 * 
	 * @param text sequence of sentences
	 * @return array of sentences in the text
	 */
	public static String[] sentDetect(String text) {
		if (sentenceDetector == null)
			createSentenceDetector();
		return sentenceDetector.sentDetect(text);
	}
	
	/**
	 * A model-based tokenizer used to prepare a sentence for POS tagging.
	 * 
	 * @param text text to tokenize
	 * @return array of tokens 
	 */
	public static String[] tokenize(String text) {
		if (tokenizer == null)
			createTokenizer();
		return tokenizer.tokenize(text);
	}
	
	/**
	 * Applies the model-based tokenizer and concatenates the tokens with
	 * spaces.
	 * 
	 * @param text text to tokenize
	 * @return string of space-delimited tokens or <code>null</code>, if the
	 * 		   tokenizer is not initialized
	 */
	public static String tokenizeWithSpaces(String text) {
		String[] tokens = tokenize(text);
		return (tokens != null) ? StringUtils.joinWithSpaces(tokens) : null;
	}
	
	/**
	 * <p>Untokenizes a text by removing abundant blanks.</p>
	 * 
	 * <p>Note that it is not guaranteed that this method exactly reverts the
	 * effect of <code>tokenize()</code>.</p>
	 * 
	 * @param text text to untokenize
	 * @return text without abundant blanks
	 */
	public static String untokenize(String text) {
		Matcher m = ABUNDANT_BLANKS.matcher(text);
		while (m.find()) {
			String noBlank = "";
			for (String token : m.group(0).split(" ")) noBlank += token;
			text = text.replace(m.group(0), noBlank);
		}
		return text;
	}
	
	/**
	 * <p>Untokenizes a text by mapping it to a string that contains the
	 * original text as a subsequence.</p>
	 * 
	 * <p>Note that it is not guaranteed that this method exactly reverts the
	 * effect of <code>tokenize()</code>.</p>
	 * 
	 * @param text text to untokenize
	 * @param original string that contains the original text as a subsequence
	 * @return subsequence of the original string or the input text, iff there
	 * 		   is no such subsequence
	 */
	public static String untokenize(String text, String original) {
		// try with boundary matchers
		String regex = RegexConverter.strToRegexWithBounds(text);
		regex = regex.replace(" ", "\\s*+");
		Matcher m = Pattern.compile(regex).matcher(original);
		if (m.find()) return m.group(0);
		
		// try without boundary matchers
		regex = RegexConverter.strToRegex(text);
		regex = regex.replace(" ", "\\s*+");
		m = Pattern.compile(regex).matcher(original);
		if (m.find()) return m.group(0);
		
		// untokenization failed
		return text;
	}
	
	/**
	 * Assigns POS tags to a sentence of space-delimited tokens.
	 * 
	 * @param sentence sentence to be annotated with POS tags
	 * @return tagged sentence 
	 */
	public static String tagPos(String sentence) {
		if (tagger == null)
			createPosTagger();
		return tagger.tag(sentence);
	}
	
	/**
	 * Assigns POS tags to an array of tokens that form a sentence.
	 * 
	 * @param sentence array of tokens to be annotated with POS tags
	 * @return array of POS tags or <code>null</code>, if the tagger is not
	 * 		   initialized
	 */
	public static String[] tagPos(String[] sentence) {
		return (tagger != null) ? tagger.tag(sentence) : null;
	}
	
	/**
	 * Assigns chunk tags to an array of tokens and POS tags.
	 * 
	 * @param tokens array of tokens
	 * @param pos array of corresponding POS tags
	 * @return array of chunk tags 
	 */
	public static String[] tagChunks(String[] tokens, String[] pos) {
		if (chunker == null)
			createChunker();
		return chunker.chunk(tokens, pos);
	}
	
	/**
	 * Peforms a full parsing on a sentence of space-delimited tokens.
	 * 
	 * @param sentence the sentence
	 * @return parse of the sentence or <code>null</code>, if the parser is not
	 * 		   initialized or the sentence is empty
	 */
	public static Parse parse(String sentence) {
		return (parser != null && sentence.length() > 0)
			// only get first parse (that is most likely to be correct)
			? TreebankParser.parseLine(sentence, parser, 1)[0]
			: null;
	}
	
	/**
	 * Identifies coreferences in an array of full parses of sentences.
	 * 
	 * @param parses array of full parses of sentences
	 */
	public static void link(Parse[] parses) {
		int sentenceNumber = 0;
		List<Mention> document = new ArrayList<Mention>();
		
		for (Parse parse : parses) {
			DefaultParse dp = new DefaultParse(parse, sentenceNumber);
			Mention[] extents =	linker.getMentionFinder().getMentions(dp);
			
			//construct new parses for mentions which do not have constituents
			for (int i = 0; i < extents.length; i++)
				if (extents[i].getParse() == null) {
					Parse snp = new Parse(parse.getText(), extents[i].getSpan(),
										  "NML", 1.0);
					parse.insert(snp);
					extents[i].setParse(new DefaultParse(snp,sentenceNumber));
				}
			
			document.addAll(Arrays.asList(extents));
			sentenceNumber++;
	    }
		
		if (document.size() > 0) {
//			Mention[] ms = document.toArray(new Mention[document.size()]);
//			DiscourseEntity[] entities = linker.getEntities(ms);
//			TODO return results in an appropriate data structure
		}
	}
	
	private static HashSet<String> unJoinablePrepositions = new HashSet<String>(); 
	static {
		unJoinablePrepositions.add("that");
		unJoinablePrepositions.add("than");
		unJoinablePrepositions.add("which");
		unJoinablePrepositions.add("whose");
		unJoinablePrepositions.add("if");
		unJoinablePrepositions.add("such");
		unJoinablePrepositions.add("whether");
		unJoinablePrepositions.add("when");
		unJoinablePrepositions.add("where");
		unJoinablePrepositions.add("who");
	}
	
	public static String[] joinNounPhrases(String[] tokens, String[] chunkTags) {
		if (chunkTags.length < 2) return chunkTags;
		
		String[] newChunkTags = new String[chunkTags.length];
		newChunkTags[0] = chunkTags[0];
		
		for (int t = 1; t < chunkTags.length; t++) {
			if ("B-NP".equals(chunkTags[t]) && ("B-NP".equals(chunkTags[t - 1]) || "I-NP".equals(chunkTags[t - 1]))) {
				newChunkTags[t] = "I-NP";
			} else if ((t != 1) && "B-NP".equals(chunkTags[t]) && "B-PP".equals(chunkTags[t - 1]) && !unJoinablePrepositions.contains(tokens[t-1]) && ("B-NP".equals(chunkTags[t - 2]) || "I-NP".equals(chunkTags[t - 2]))) {
				newChunkTags[t - 1] = "I-NP";
				newChunkTags[t] = "I-NP";
			} else newChunkTags[t] = chunkTags[t];
		}
		
		return newChunkTags;
	}
	
	public static void main(String[] args) {
		System.out.println(OpenNLP.tagPos("A study on the Prethcamide"));
		System.out.println(OpenNLP.tokenizeWithSpaces("CD44, at any stage, is a XYZ, Dr. Johnson (pp. 85)."));
		String[] sents = OpenNLP.sentDetect("What does the plasma membrane do? Why is it necessary? 7.1 Cellular membranes are fluid mosaics of lipids and proteins (pp. 125-131) The Davson-Danielli sandwich model of the membrane has been replaced by the fluid mosaic model, in which amphipathic proteins are embedded in the phospholipid bilayer.");
		for (String s:sents)
			System.out.println(s);
	}
}
