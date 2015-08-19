/**
 * 
 */
package edu.jhu.jacana.reader;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.tek271.memoize.RememberFactory;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

import edu.jhu.jacana.bioqa.VulcanInputInstance;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.nlp.SnowballStemmer;
import edu.jhu.jacana.nlp.WordNet;
import edu.jhu.jacana.nlp.WordNetMemoizable;
import edu.jhu.jacana.util.FileManager;
import edu.stanford.nlp.ling.WordLemmaTag;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * This class reads files in MST format:
 * http://www.seas.upenn.edu/~strctlrn/MSTParser/README
 * 
 * 
Each sentence in the data is represented by 3 or 4 lines and sentences are
space separated. The general format is:

w1    w2    ...    wn
p1    p2    ...    pn
l1    l2    ...    ln
d1    d2    ...    d2

....


Where,
- w1 ... wn are the n words of the sentence (tab deliminated)
- p1 ... pn are the POS tags for each word
- l1 ... ln are the labels of the incoming edge to each word
- d1 ... dn are integers representing the postition of each words parent

For example, the sentence "John hit the ball" would be:

John	hit	the	ball
N	V	D	N
SBJ	ROOT	MOD	OBJ
2	0	4	2

 * @author Xuchen Yao
 *
 */
public class MstReaderMemoizable {
	
	protected static Logger logger = Logger.getLogger(MstReaderMemoizable.class.getName());
	
	public static DependencyTree read(String word_line, String pos_line, String lab_line, String deps_line) {
		return read(word_line, pos_line, lab_line, deps_line, false);
	}
	
	public static DependencyTree read(String word_line, String pos_line, String lab_line, String deps_line, boolean dummyRoot) {
		return read(word_line, pos_line, lab_line, deps_line, null, null, dummyRoot);
	}
	
	
	public static DependencyTree read(VulcanInputInstance.AnalyzedInstance ins, boolean dummyRoot) {
		return read(ins.word_line, ins.pos_line, ins.lab_line, ins.deps_line, ins.ner_line, null, dummyRoot);
	}
	
	public static DependencyTree read(String word_line, String pos_line, String lab_line, String deps_line, String ner_line, String ans_line, boolean dummyRoot) {
		return read(word_line, pos_line, lab_line, deps_line, ner_line, ans_line, dummyRoot, true);
	}
	/**
	 * Parse each line and output a dependency tree. If <code>dummyRoot=true</code>, then insert a dummy root node
	 * on top of the original root node. This is for the RTE task where the premise or hypothesis contains more than
	 * one sentence.
	 * @param word_line
	 * @param pos_line
	 * @param lab_line
	 * @param deps_line
	 * @param ans_line an answer line with index, such as "2   3   #   3   4   MULTIPLE_ANSWER"
	 * @param dummyRoot
	 * @param getLemma: whether to use WordNet to obtain lemma of each word. When set to true, the code is about 70 times slower (0.1ms/instance -> 7ms/instance)
	 * @return
	 */
	public static DependencyTree read(String word_line, String pos_line, String lab_line, String deps_line, String ner_line, String ans_line, boolean dummyRoot, boolean getLemma) {
		
		String[] toks = word_line.toLowerCase().replaceAll(":", "#colon#").replaceAll("/", "#slash#")
				.replaceAll("\\{", "#left_curley_bracket#").replaceAll("\\}", "#right_curley_bracket#").split("\t");
	    //String[] stems = SnowballStemmer.stemAllTokens(toks);
	    String[] pos = pos_line.toLowerCase().replaceAll(":", "#colon#").split("\t");
	    String[] stems;
	    if (getLemma) {
		    stems = WordNetMemoizable.getLemmas(toks, pos);
	    } else
		    stems = toks;
	    
	    String[] labs = lab_line.toLowerCase().replaceAll(":", "#colon#").split("\t");
	    String[] deps = deps_line.toLowerCase().split("\t");
	    
	    String[] entities = ner_line==null ? null : ner_line.split("\t");
	    int len = toks.length;
	    
	    String[] answers = null;
	    if (ans_line != null) {
	    	answers = new String[toks.length];
	    	Arrays.fill(answers, "O");
	    	String[] splits = ans_line.split("\t");
	    	for (String split:splits) {
	    		if (split.equals("#")) continue;
	    		int idx = Integer.parseInt(split)-1;
	    		if (idx-1 >= 0 && !answers[idx-1].equals("O"))
	    			answers[idx] = "ANSWER-I"; // inside an answer
	    		else
	    			answers[idx] = "ANSWER-B"; // beginning of an answer
	    	}
	    }
	    
	    DependencyTree tree = new DependencyTree(); 
	    tree.setOrigSentence(word_line);

	    // the RTE data might come with multiple roots (from multiple sentences)
	    // in this case insert a dummy root and connect all original roots to this dummy one
	    int rootIndex = 0;

		//System.err.println(word_line);
	    for(int i = 0; i < toks.length; i++) {
			WordLemmaTag curToken = new WordLemmaTag(toks[i], stems[i], pos[i]);
			tree.getLabels().add(curToken);
			TreeGraphNode treeNode = new TreeGraphNode(curToken);
			treeNode.label().setTag(curToken.tag());
			treeNode.label().setLemma(stems[i]);
			tree.getTree().add(treeNode);
			if (entities == null || entities[i].equals("-"))
				tree.getEntities().add("O");
			else
				tree.getEntities().add(entities[i]);
			if (answers == null)
				tree.getAnswers().add("O");
			else
				tree.getAnswers().add(answers[i]);
	    }
	    if (dummyRoot) {
	    	String dummy = "dummyroot";
			WordLemmaTag curToken = new WordLemmaTag(dummy, dummy, dummy);
			tree.getLabels().add(curToken);
			TreeGraphNode treeNode = new TreeGraphNode(curToken);
			treeNode.label().setTag(curToken.tag());
			treeNode.label().setLemma(dummy);
			tree.getTree().add(treeNode);
			tree.setRoot(treeNode);
	    	rootIndex = labs.length;
	    }
	    
	    boolean hasRoot = false;
	    
	    for(int i = 0; i < toks.length; i++) {
			
			int govId = Integer.parseInt(deps[i]), depId = i;
			if (govId == 0)
				hasRoot = true;
			else if (govId > len) {
				logger.warning("Wrong root:");
				logger.warning(word_line);
			}
			
			if (govId != 0 || dummyRoot) {
				TreeGraphNode gov;
				if (dummyRoot && govId == 0)
					gov = tree.getTree().get(rootIndex);
				else
					// 0 is for ROOT
					gov = tree.getTree().get(govId - 1);
				TreeGraphNode dep = tree.getTree().get(depId);

				// Add gov/dep to TreeGraph
				gov.addChild(dep);
				dep.setParent(gov);

				// Create the typed dependency
				TypedDependency typedDep = new TypedDependency(GrammaticalRelation.valueOf(labs[i]), gov, dep);
				tree.getDependencies().add(typedDep);
			} else if (rootIndex == 0) {
				// single root case
				tree.setRoot(tree.getTree().get(depId));
				TreeGraphNode gov = null;
				TreeGraphNode dep = tree.getTree().get(depId);
				dep.setParent(gov);
				TypedDependency typedDep = new TypedDependency(GrammaticalRelation.valueOf(labs[i]), gov, dep);
				tree.getDependencies().add(typedDep);
			}
	    }
	    
	    if (!hasRoot) {
		    logger.warning("Warning, root not found in sentence:");
		    logger.warning(word_line);
	    }
	    return tree;		
	}

	public static List<DependencyTree> read (String fileName) {
		return read(fileName, false);
	}
	/**
	 * Given a raw file in MST format, output all trees it contains.
	 * @param fileName the raw source file
	 * @return a list of trees
	 */
	public static List<DependencyTree> read (String fileName, boolean dummyRoot) {
		List<DependencyTree> treeList = new ArrayList<DependencyTree>();
		try {
			BufferedReader in = FileManager.getReader(fileName);
			String line;
			
			while ((line = in.readLine()) != null) {
			    String pos_line = in.readLine();
			    String lab_line = in.readLine();
			    String deps_line = in.readLine();
			    in.readLine(); // NER line
			    in.readLine(); // blank line

			    DependencyTree tree = read(line, pos_line, lab_line, deps_line, dummyRoot); 
			    treeList.add(tree);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return treeList;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<DependencyTree> trees = MstReaderMemoizable.read("tree-edit-data/answerSelectionExperiments/training/1-100/test", false);
		// q/a pairs
		for (int i=0; i<trees.size()/2; i++) {
			DependencyTree questionTree = trees.get(2*i);
			DependencyTree answerTree = trees.get(2*i+1);
//			String qString = questionTree.stringInRTEDtree();
//			String aString = answerTree.stringInRTEDtree();
			String qString = questionTree.toCompactString("qRoot");
			String aString = answerTree.toCompactString("aRoot");
			System.out.println(qString);
			System.out.println(aString);
			LblTree qTree = LblTree.fromString(qString);
			LblTree aTree = LblTree.fromString(aString);
			
			qTree.prettyPrint();
			aTree.prettyPrint();
			
			EditDist dis2 = new EditDist(10,10,3,false);
			System.out.println(dis2.treeDist(aTree, qTree));			
			dis2.printForestDist();
			dis2.printTreeDist();
			dis2.printBackPointer();
			dis2.printEditMatrix();
			System.out.println(dis2.printEditScript());
			System.out.println(dis2.printHumaneEditScript());
			System.out.println(dis2.printCompactEditScript());
		}
	}

}
