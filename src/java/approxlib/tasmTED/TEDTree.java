package approxlib.tasmTED;

import java.util.Arrays;

import approxlib.util.LabelDictionary;


/**
 * Tree structure used for the tree edit distance (TED) computation.
 * The nodes in the tree are accessed by their postorder number: Node
 * i is the i-th node in postorder.
 * The postorder count starts with 1, i.e., the leaf-most 
 * leaf descendant of the root is the first node in postorder. 
 * 
 * @author naugsten
 *
 */
abstract public class TEDTree implements PostorderQueue, PostorderSource {
	
	/**
	 * Test if a node is a leaf in constant time.
	 * 
	 * @param i postorder number of a node
	 * @return true if node i is a leaf
	 */
	public boolean isLeaf(int i) {
		return this.lld(i) == i;
	}
	
	/* (non-Javadoc)
	 * @see distance.PostorderQueue#append(java.lang.String, int)
	 */
	public void append(String label, int subtreeSize) {
		int newNodeID = this.getNodeCount() + 1;
		this.setLabel(newNodeID, label);
		this.setLld(newNodeID, newNodeID - subtreeSize + 1);
	}

	/**
	 * Set the left-most leaf descendant of a node.
	 * 
	 * @param i postorder number of a node
	 * @param newLld new value for the left-most leaf descendent of i
	 */
	abstract public void setLld(int i, int newLld);
	
	/**
	 * Get the left-most leaf descendant of a node.
	 * 
	 * @param i postorder number of a node
	 * @return left-most leaf descendant of i
	 */
	abstract public int lld(int i); 

	/**
	 * Get the label of a node.
	 * 
	 * @param i postorder number of a node
	 * @return label of node i
	 */
	abstract public String getLabel(int i);
	
	/**
	 * Set the label of a node.
	 * 
	 * @param i postorder number of a node
	 * @param newLabel new value for the label of node i
	 */
	abstract public void setLabel(int i, String newLabel);
	
	/**
	 * Test label equality.
	 * 
	 * @param i postorder number of a node in this tree
	 * @param t1 other tree
	 * @param j postorder number of a node in t1
	 * @return true iff nodes i and j have the same label
	 */
	public boolean equalsLbl(int i, TEDTree t1, int j) {
		if (this.getLabelDictionary() != null &&
				this.getLabelDictionary() == t1.getLabelDictionary()) {
			return this.getLabelID(i) == t1.getLabelID(j);
		} else {
			return getLabel(i).equals(t1.getLabel(j));
		}
		
	}
	
	/**
	 * Get number of nodes in constant time.
	 * 
	 * @return number of tree nodes
	 */
	abstract public int getNodeCount();
	
	/**
	 * Compute the keyroots of a TEDtree in O(n) time, where n is the size of the tree.
	 * 
	 * @return key roots kr, kr[i], is the i-th key root in postorder, kr[0] is not defined  
	 */
	public static int[] getKeyRoots(TEDTree ted) {
		int[] kr = new int[ted.getLeafCount() + 1];
		boolean[] visited = new boolean[ted.getNodeCount() + 1];
		Arrays.fill(visited, false);
		int k = kr.length - 1;
		for (int i = ted.getNodeCount(); i >= 1; i--) {
			if (!visited[ted.lld(i)]) {
				kr[k] = i;
				visited[ted.lld(i)] = true;
				k--;
			}
		}
		return kr;
	}
	
	/**
	 * Get the fanout of a node in O(f) time, where f is the fanout.
	 *  
	 * @param i postorder number of a node
	 * @return
	 */
	public int getFanout(int i) {
		// leaf
		if (this.isLeaf(i)) {
			return 0;
		}
		// non-leaf
		int fanout = 1;
		int child = i - 1;
		while (this.lld(child) > this.lld(i)) {
			fanout++;
			child = this.lld(child) - 1;
		}
		return fanout;
	}
	
	/**
	 * Count the number of leaf nodes. 
	 * This implementation takes O(n) time. 
	 * 
	 * @return number of leaf nodes of this tree
	 */
	public int getLeafCount() {
		int leafCount = 0;
		for (int i = 1; i <= this.getNodeCount(); i++) {
			if (this.isLeaf(i)) {
				leafCount++;
			}
		}
		return leafCount;
	}
	
	/**
	 * Get the subtree rooted in a node in constant time.
	 * The subtree data is not copied, but only a reference is produced. 
	 * If the relevant parts of this tree change, 
	 * also the subtree will change. 
	 * 
	 * @param i postorder number of subtree root
	 * @return reference to subtree rooted at node i
	 */
	abstract public TEDTree getSubtree(int i);

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 1; i <= this.getNodeCount(); i++) {
			sb.append(String.format("(%s,%d)", this.getLabel(i), this.lld(i)));
			if (i != this.getNodeCount()) {
				sb.append(",");
			}
		}
		return sb.toString() + "]";
	}

	/**
	 * The postorder ID of the root node is equal to the number of nodes. 
	 * 
	 * @return postorder ID of the root node
	 */
	public int root() {
		return getNodeCount();
	}

	/**
	 * Get the smallest postorder number for which {@link #lld(int)} and {@link #getLabel(int)}
	 * can be called without getting an error. 
	 * 
	 * @return smallest valid postorder number if the tree contains nodes, 0 otherwise
	 */
	public int smallestValidID() {
		if (this.getNodeCount() > 0) {
			return 1;
		} else {
			throw new RuntimeException("Called smallestValidID() for an empty tree.");
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		TEDTree ted = (TEDTree) obj;
		if (this.getNodeCount() != ted.getNodeCount()) {
			return false;
		}
		for (int i = 1; i <= this.getNodeCount(); i++) {
			if (this.lld(i) != ted.lld(i)) {
				return false;
			}
			if (!this.equalsLbl(i, ted, i)) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see tasmTED.PostorderSource#appendTo(tasmTED.PostorderQueue)
	 */
	public void appendTo(PostorderQueue postorderQueue) {
		for (int i = 1; i <= getNodeCount(); i++) {
			postorderQueue.append(getLabel(i), i - lld(i) + 1);
		}	
	}
	
	/**
	 * If this tree is a subtree of an other tree (the supertree)
	 * then the postorder numbers of the nodes between this 
	 * tree and the supertree might be different. This method computes
	 * the postorder ID of a given node in the supertree.  
	 *  
	 * @param i postorder ID of a node in this tree
	 * @return postorder ID of i in the supertree
	 * 
	 * @see #getSubtree(int)
	 */
	abstract public int getGlobalID(int i);
	
	/**
	 * A label dictionary assigns integer keys to labels. This is usesful
	 * to store a document (with repeating labels) in smaller space and
	 * to compute label equality faster. 
	 * 
	 * @return return the label dictionary of this tree or null if this does 
	 * not implement a label dictionary.
	 * 
	 * @see util.LabelDictionary
	 */
	public abstract LabelDictionary getLabelDictionary();

	/**
	 * Compute the label ID of a given node. 
	 *  
	 * @param i label ID of the i-th node in postorder of this tree
	 */
	public abstract int getLabelID(int i);

}
