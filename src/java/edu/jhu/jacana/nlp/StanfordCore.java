/**
 * 
 */
package edu.jhu.jacana.nlp;

import edu.jhu.jacana.util.FileManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

/**
 * A wrapper for StanfordCoreNLP pipeline. It does the following job:
 * sentence segmentation, tokenization, lemmatization and parsing.
 * POS tags are extracted from the parsing model since the original
 * POS tagger was trained on WSJ while the parser was trained on:
 * http://nlp.stanford.edu/software/parser-faq.shtml#z
 *  1. WSJ sections 1-21
    2. Genia (biomedical English). Originally we used the treebank beta version reformatted by Andrew Clegg, his training split, but more recently (1.6.5+?) we've used the official Treebank, and David McClosky's splits
    3. 2 English Chinese Translation Treebank and 3 English Arabic Translation Treebank files backported to the original treebank annotation standards (by us)
    4. 95 sentences parsed by us (mainly questions and imperatives; a few from recent newswire)
    5. 3924 questions from QuestionBank, with some hand-correction done at Stanford. 
 * So the parser should provide more balanced POS tagging performance.
 * 
 * 
 * @author Xuchen Yao
 *
 */
public class StanfordCore {
	
//	public class ParsedInstance {
//		public ParsedInstance() {};
//		String word_line, pos_line, lab_line, deps_line, ner_line, mstString;
//	}
	
	protected static StanfordCoreNLP pipeline;
    protected static TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	
	public static void init() {
		init(false, false, false);
	}

	public static void initForQuery() {
		// a lot of query data is lower-cased
		init(false, true, true);
	}

	public static void initWithNER() {
		init(false, true, false);
	}
	
	public static void init(boolean tokenizeWithSpace, boolean loadNER, boolean caseless) {
		if (pipeline != null) return;
		Properties props = new Properties();
		// there are really only two steps here since
		// ssplit must follow tokenize
		// lemma must follow pos or parse
		if (loadNER)
			props.put("annotators", "tokenize,ssplit,parse,lemma,ner");
		else
			props.put("annotators", "tokenize,ssplit,parse,lemma");
		if (tokenizeWithSpace)
			props.put("tokenize.whitespace", "true");
		props.put("parse.maxlen", "100");
		props.put("ssplit.eolonly", "true");
		
		if (caseless) {
            props.put( "parse.model", "edu/stanford/nlp/models/lexparser/englishPCFG.caseless.ser.gz" );
			if (loadNER) {
				// props.put("ner.model.3class", "edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz");
				// props.put("ner.model.7class", "edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz");
				// props.put("ner.model.MISCclass", "edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");
				props.put("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz," +
						"edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz," +
						"edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");
			}
		} else {
			// backward compatible (if included the stanford-corenl-model.jar file, then don't need this line):
			props.put("parse.model", FileManager.getResource("resources/model/englishPCFG.ser.gz"));
		}

		// props.put("parse.model", "edu/stanford/nlp/models/lexparser/englishPCFG.caseless.ser.gz");
		pipeline = new StanfordCoreNLP(props);
	}
	
	public static Annotation process(String text) {
		return process(text, false);
	}
	
	public static Annotation processWithSpaceTokenizer(String text) {
		return process(text, true);
	}
	
	public static Annotation process(String text, boolean tokenizeWithSpace) {
		if (pipeline == null)
			init(tokenizeWithSpace, false, false);
		return pipeline.process(text);
	}
	
	public static Annotation processQuery(String text) {
		if (pipeline == null)
			initForQuery();
		return pipeline.process(text);
	}
	/**
	 * get splitted sentences from a document Annotation. 
	 * @param document the returned value of <code>process(String text)</code>
	 * @return a list of sentences
	 */
	public static List<CoreMap> getSentences (Annotation document) {
		return document.get(CoreAnnotations.SentencesAnnotation.class);
	}
	
	/**
	 * get the list of tokens from a sentence. Each token should have its
	 * own POS/lemma/offsets. e.g.:
          <token id="3">
            <word>the</word>
            <lemma>the</lemma>
            <CharacterOffsetBegin>10</CharacterOffsetBegin>
            <CharacterOffsetEnd>13</CharacterOffsetEnd>
            <POS>DT</POS>
          </token>
	 * @param sentence member of returned list form <code>getSentences (Annotation document)</code>
	 * @return a list of tokens
	 */
	//public static List<CoreLabel> getTokens (CoreMap sentence) {
	//	return sentence.get(CoreAnnotations.TokensAnnotation.class);
	//}
	
	public static List<TreeGraphNode> getTokens (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		List<TreeGraphNode> nodes = new ArrayList<TreeGraphNode>();
		
		for (CoreLabel label:labels) {
			nodes.add(new TreeGraphNode(label));
		}
		return nodes;
	}

	public static String[] getTokensInString (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		String[] tokens = new String[labels.size()];
		
		for (int i = 0; i < labels.size(); i++) {
			tokens[i] = labels.get(i).word();
		}
		return tokens;
	}

	public static CoreLabel[] getTokensInLabels (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		return labels.toArray(new CoreLabel[labels.size()]);
	}

	public static String[] getNamedEntitiesInString (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		String[] ner = new String[labels.size()];
		
		for (int i = 0; i < labels.size(); i++) {
			ner[i] = labels.get(i).ner();
		}
		return ner;
	}

	public static int getSentenceLength(CoreMap sentence) {
		return sentence.get(CoreAnnotations.TokensAnnotation.class).size();
	}

	public static CoreLabel[] getLabels (CoreMap sentence) {
		return sentence.get(CoreAnnotations.TokensAnnotation.class).toArray(new CoreLabel[sentence.size()]);
	}

	public static String[] getBasicDepRelations (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		int size = labels.size();
		String[] deps = new String[size];
		// String[] indices = new String[size];
		SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        for (SemanticGraphEdge edge : graph.edgeListSorted()) {
	        // int govId = edge.getSource().index()-1;
	        int depId = edge.getTarget().index()-1;
        	deps[depId] = edge.getRelation().toString();
	        // indices[depId] = Integer.toString(govId+1);
        }
        return deps;
	}

	public static SemanticGraph getCollapsedCCProcessedGraph (CoreMap sentence) {
		return sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
	}

	public static String[] getCollapsedCCProcessedDepRelations (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		int size = labels.size();
		String[] deps = new String[size];
		// String[] indices = new String[size];
		SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
        for (SemanticGraphEdge edge : graph.edgeListSorted()) {
	        // int govId = edge.getSource().index()-1;
	        int depId = edge.getTarget().index()-1;
        	deps[depId] = edge.getRelation().toString();
	        // indices[depId] = Integer.toString(govId+1);
        }
        return deps;
	}
	
	public static List<IntPair> getPhrases (CoreMap sentence, String phrase) {
		Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
		tree.setSpans();
		ArrayList<IntPair> list = new ArrayList<IntPair>();
		for (Tree sub:tree.subTreeList()) {
			if (sub.label().value().equalsIgnoreCase(phrase))
				list.add(sub.getSpan());
		}
		return list;
	}

	public static List<SemanticGraphEdge> getBasicDepEdges (CoreMap sentence) {
		SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
		return graph.edgeListSorted();
	}

	/**
	 * return a pair of word line and pos line, such as:
	 * What    do  practitioners   of  Wicca   worship ?
	 * WP  VBP NNS IN  NNP NN  .
	 */
	public static Pair<String, String> getTokenAndPosInOneLine (CoreMap sentence) {
		List<String> words = new ArrayList<String>();
		List<String> pos = new ArrayList<String>();
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		for (CoreLabel label:labels) {
			words.add(label.word());
			pos.add(label.tag());
		}
		return new Pair<String, String>(StringUtils.join(words, "\t"), StringUtils.join(pos, "\t"));
	}
	
	public static Pair<String, String> getDepAndIndexInOneLine (CoreMap sentence) {
		List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
		int size = labels.size();
		String[] deps = new String[size];
		String[] indices = new String[size];
		SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        for (SemanticGraphEdge edge : graph.edgeListSorted()) {
	        int govId = edge.getSource().index()-1;
	        int depId = edge.getTarget().index()-1;
        	deps[depId] = edge.getRelation().toString();
	        indices[depId] = Integer.toString(govId+1);
        }

        int rootIndex = -1;
        for (int i=0; i<size; i++)
        	if (indices[i] == null) {
        		if (!tlp.isPunctuationTag(labels.get(i).word()) && !labels.get(i).word().equals("?")) {
	        		indices[i] = "0";
	        		deps[i] = "root";
	        		rootIndex = i;
        		}
        	}
        if (rootIndex != -1)
	         for (int i=0; i<size; i++)
	        	if (indices[i] == null) {
	        		indices[i] = Integer.toString(rootIndex+1);
	        		deps[i] = "p";
	        	}
		return new Pair<String, String>(StringUtils.join(deps, "\t"), StringUtils.join(indices, "\t"));
	}
	
	public static List<TypedDependency> getBasicDependencies (CoreMap sentence, List<TreeGraphNode> tokens) {
		List<TypedDependency> depList = new ArrayList<TypedDependency>();
		SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
		TreeGraphNode gov, dep;
        for (SemanticGraphEdge edge : graph.edgeListSorted()) {
	        int govId = edge.getSource().index();
	        int depId = edge.getTarget().index();
	        gov = tokens.get(govId-1);
	        dep = tokens.get(depId-1);
			gov.addChild(dep);
			dep.setParent(gov);
			TypedDependency typedDep = new TypedDependency(edge.getRelation(), gov, dep);
			depList.add(typedDep);
        }
        
        return depList;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String text = "What does the plasma membrane do? Why is it necessary? 7.1 Cellular membranes are fluid mosaics of lipids and proteins (pp. 125-131) The Davson-Danielli sandwich model of the membrane has been replaced by the fluid mosaic model, in which amphipathic proteins are embedded in the phospholipid bilayer.";
		Annotation document = StanfordCore.process(text, true);
		// String text = "who is justin bieber brother? what did natalie portman play in star wars?";
		// Annotation document = StanfordCore.processQuery(text);
		Writer writer = new BufferedWriter(new OutputStreamWriter(System.out));
        try {
			StanfordCore.pipeline.xmlPrint(document, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        for (CoreMap sent:StanfordCore.getSentences(document)) {
        	System.out.println(StanfordCore.getTokens(sent));
        	System.out.println(StanfordCore.getBasicDependencies(sent, StanfordCore.getTokens(sent)));
        	// System.out.println(Arrays.asList(StanfordCore.getNamedEntitiesInString(sent)));
        }
	}

}
