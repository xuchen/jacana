/**
 * 
 */
package edu.jhu.jacana.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import edu.stanford.nlp.ling.WordLemmaTag;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class DependencyTree {
	
	protected List<WordLemmaTag> labels;
	protected List<TreeGraphNode> tree;
	protected List<TypedDependency> dependencies;
	protected List<String> entities; // named entity types (PERSON-B/I, ..., O)
	protected List<String> answers; // answer types (ANS-B, ANS-I, O)
	protected List<Integer> align_indices; // alignment index
	protected List<String> posTags; // pos tags
	protected String sentence = null; // original sentence lower-cased
	protected String origSentence = null; // original sentence appeared in the input file
	protected TreeGraphNode root = null;
	protected HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep = null;
	protected HashMap<TreeGraphNode, Integer> node2idx = null;

	//private static Logger logger = Logger.getLogger(DependencyTree.class.getName());
	
	// a temp hack for the place holder of qContentWordIdx used for only the target of question
	// word when the tree is for questions, such as the idx of "sport" for "what sport does ... play ?"
	protected int qContentWordIdx = -1;
	public void setQContentWordIdx (int idx) {this.qContentWordIdx = idx;}
	public int getQContentWordIdx () {return this.qContentWordIdx;}
	
	public DependencyTree() {
		labels = new ArrayList<WordLemmaTag>();
		tree = new ArrayList<TreeGraphNode>();
		dependencies = new ArrayList<TypedDependency>();
		node2idx = new HashMap<TreeGraphNode, Integer>();
		entities = new ArrayList<String>();
		posTags = new ArrayList<String>();
		align_indices = new ArrayList<Integer>();
		answers = new ArrayList<String>();
	}
	
	public DependencyTree(List<WordLemmaTag> labels, List<TreeGraphNode> tree, 
			List<TypedDependency> dependencies) {
		this.labels = labels;
		this.tree = tree;
		this.dependencies = dependencies;
		node2idx = new HashMap<TreeGraphNode, Integer>();
		entities = new ArrayList<String>();
		posTags = new ArrayList<String>();
		answers = new ArrayList<String>();
	}

	public int getSize() { return this.tree.size();}

	public List<WordLemmaTag> getLabels() {	return labels;}

	public List<String> getEntities() {	return entities;}

	public List<String> getAnswers() { return answers;}
	
	public List<Integer> getAlignIndices() { return align_indices;}
	
	public boolean isAligned() { return align_indices.size() != 0;}


	/**
	 * returns a mapping between a tree node and it's index in the original word order (starting from 0). 
	 */
	public HashMap<TreeGraphNode, Integer> getNode2idx() {
		if (this.node2idx.size() == 0) {
			for (int idx=0; idx < this.tree.size(); idx++) {
				node2idx.put(this.tree.get(idx), idx);
			}
		}
		return node2idx;
	}
	
	public String getOrigSentence() { return this.origSentence; }
	public void setOrigSentence(String sent) { this.origSentence = sent; }
	
	/**
	 * return the sentence in lower case
	 * @return a string
	 */
	public String getSentence() {
		if (sentence != null) return sentence;
		String[] words = new String[labels.size()];
		
		for (int i=0; i<labels.size(); i++) {
			WordLemmaTag label = labels.get(i);
			words[i] = label.word();
		}
		sentence = StringUtils.join(words, " ");
		return sentence;
	}
	
	public List<String> getPosTags() {
		if (posTags.size() != 0) return posTags;
		for (int i=0; i<labels.size(); i++) {
			WordLemmaTag label = labels.get(i);
			posTags.add(label.tag());
		}
		return posTags;
	}

	public List<TreeGraphNode> getTree() {
		return tree;
	}

	public List<TypedDependency> getDependencies() {
		return dependencies;
	}
	
	/**
	 * Given a node, return it's index in word order of the sentence
	 * @param node
	 */
	public int getIdxOfNode(TreeGraphNode node) {
		if (this.node2idx.size() == 0) {
			getNode2idx();
		}
		return this.node2idx.get(node);
	}

	public void setRoot(TreeGraphNode node) {
		this.root = node;
	}
	
	/**
	 * Print the dependency tree in RTED format:
	 * -1:{ -1:root { 2:node { 0:node } } { 3:node } ... }
	 * In the final output all spaces are stripped. 
	 * RTED format assumes no labels for edges, but dependency trees do.
	 * Thus we insert the edge label as a single node in between governor and dependent.
	 * The first -1 is the ID for the entire tree.
	 * The second -1 is the ID for the root node.
	 * Other numbers before: are IDs for individual nodes in word order (starting from 0)
	 * @return a string
	 */
	public String stringInRTEDtree () {
		return stringInRTEDtree(true);
	}
	
	public String toCompactString() {
		return stringInRTEDtree(false);
	}
	
	public String toCompactString(String rootName) {
		return stringInRTEDtree(false, rootName);
	}
	
	protected String stringInRTEDtree (boolean depAsSingleNode, String rootName) {
		// the 'fake' root node has an index of -1
		// other nodes start from 0 in word order
		if (depAsSingleNode)
			return "-1:{-1:" + rootName + this.stringInRTEDnode(null, this.getRoot()).trim() + "}";
		else
			return "-1:{-1:" + rootName + this.stringInRTEDnode(rootName, this.getRoot()).trim() + "}";
	}
	
	/**
	 * If depAsSingleNode is set true, then the edge labels (i.e., dependency relation names)
	 * are treated as a single node, node names look like lemma/pos; if false, then they are 
	 * appended to node names, e.g., lemma/pos/dep 
	 * @param depAsSingleNode
	 * @return
	 */
	protected String stringInRTEDtree (boolean depAsSingleNode) {
		return stringInRTEDtree(depAsSingleNode, "root");
	}
	
	
	protected String stringInRTEDnode (String rel, TreeGraphNode node) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(this.getIdxOfNode(node));
		sb.append(":");
		sb.append(node.label().word());
		sb.append("/");
		sb.append(node.label().lemma());
		sb.append("/");
		sb.append(node.label().tag());
		if (rel != null) {
			sb.append("/"+rel);
		}
		
		for (TreeGraphNode n:node.children()) {
			TypedDependency dep = this.nodes2dep().get(new Pair<TreeGraphNode, TreeGraphNode>(node, n));
			if (rel == null) {
				if (dep != null)
					sb.append("{" + dep.reln().toString());
				sb.append(this.stringInRTEDnode(null, n));
				if (dep != null)
					sb.append("}");
			} else {
				sb.append(this.stringInRTEDnode(dep.reln().toString(), n));
			}
		}
		sb.append("}");
		return sb.toString();
	}

	public TreeGraphNode getRoot() {
		if (this.root != null) return this.root;

		boolean loop = false;
		HashSet<TreeGraphNode> visited = new HashSet<TreeGraphNode>();
		for (int i=0; i<this.tree.size() && !loop; i++) {
			this.root = this.tree.get(i);
			visited.add(this.root);
			while (this.root.parent() != null) {
				this.root = (TreeGraphNode) this.root.parent();
				if (visited.contains(this.root)) {
					loop = true; break;
				}
			}
			// important: this version of dependencies have a lot of
			// "islands", i.e., nodes without parents or children.
			// especially the quote symbols. ('Mr. Smith lost ...', he said)
			if (this.root.children().length != 0) {
				break;
			}
		}
		return this.root;
	}

	protected boolean isIsland(int i) {
		if (this.tree.get(i).parent() == null && (this.tree.get(i).children() == null || this.tree.get(i).children().length == 0)) {
			return true;
		}
		return false;
	}

	/**
	 * get the mapping between two nodes and their dependencies. The order of the node pair
	 * is always <governer/parent, dependent/child>.
	 * @return
	 */
	public HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep() {
		if (this.nodes2dep != null) return this.nodes2dep;
		this.nodes2dep = new HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency>();
		for (TypedDependency dep:this.dependencies) {
			this.nodes2dep.put(new Pair<TreeGraphNode, TreeGraphNode>(dep.gov(), dep.dep()), dep);
		}
		return this.nodes2dep;
	}
}
