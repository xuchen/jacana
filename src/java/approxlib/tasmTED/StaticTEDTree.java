package approxlib.tasmTED;

import java.util.Enumeration;

import approxlib.tree.LblTree;
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
/**
 * @author Nikolaus Augsten
 *
 */
public class StaticTEDTree extends TEDTree {
	
	private int start; // internal array position of leafmost leaf descendant of the root node
	private int nodeCount; // number of nodes
	private int[] llds; // llds[i] stores the postorder-ID of the 
			   // left-most leaf descendant of the i-th node in postorder 
	private String[] labels; // labels[i] is the label of the i-th node in postorder

	/**
	 * Number of leaf nodes. This is not necessarily initialized.
	 * Iff leafCount > 0 then it is assumed to be correct.
	 */
	private int leafCount; // number of leaf nodes

	public StaticTEDTree(int size) {
		this(0, 0, new int[size], new String[size], Integer.MIN_VALUE);
	}
	
	/**
	 * Private constructure.
	 * 
	 * @param start
	 * @param nodeCount
	 * @param llds
	 * @param labels
	 * @param leafCount
	 */
	private StaticTEDTree(int start, int nodeCount, int[] llds, String[] labels, int leafCount) {
		super();
		this.start = start;
		this.nodeCount = nodeCount;
		this.llds = llds;
		this.labels = labels;
		this.leafCount = leafCount;
	}
	
	/**
	 * Construct a TEDTree from a LblTree
	 * @param t a LblTree
	 */
	public StaticTEDTree(LblTree t) {
		this.start = 0;
		this.nodeCount = t.getNodeCount();
		this.leafCount = 0;
		this.llds = new int[start + nodeCount];
		this.labels = new String[start + nodeCount];
		int i = 1;
		for (Enumeration e = t.postorderEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			// add postorder number to node
			n.setTmpData(i);	    	    
			// label
			this.setLabel(i, n.getLabel());
			// left-most leaf
			this.setLld(i, (Integer)((LblTree)n.getFirstLeaf()).getTmpData());
			if (isLeaf(i)) {
				leafCount++;
			}
			i++;
		}
		t.clearTmpData();
	}
	

	/* (non-Javadoc)
	 * @see distance.TEDTree#setLld(int, int)
	 */
	@Override
	public void setLld(int i, int newLld) {
		llds[i + start - 1] = newLld + start - 1;
		if (nodeCount < i) {
			nodeCount = i;
		}
	}
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#lld(int)
	 */
	@Override
	public int lld(int i) {
		return llds[i + start - 1] - start + 1;
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#getLabel(int)
	 */
	@Override
	public String getLabel(int i) {
		return labels[i + start - 1];
	}
	

	/* (non-Javadoc)
	 * @see distance.TEDTree#setLabel(int, java.lang.String)
	 */
	@Override
	public void setLabel(int i, String newLabel) {
		labels[i + start - 1] = newLabel;			
		if (nodeCount < i) {
			nodeCount = i;
		}
	}
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#getNodeCount()
	 */
	@Override
	public int getNodeCount() {
		return nodeCount;
	}
	
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#getLeafCount()
	 */
	@Override
	public int getLeafCount() {
		if (leafCount > 0) {
			return leafCount;
		} else {
			return super.getLeafCount();
		}
	}
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#getSubtree(int)
	 */
	@Override
	public StaticTEDTree getSubtree(int i) {
		int subStart = lld(i) + start - 1;
		int subNodeCount = i - lld(i) + 1;
		StaticTEDTree subtree = new StaticTEDTree(subStart, subNodeCount, this.llds, this.labels, Integer.MIN_VALUE);
		return subtree;
	}

	/* (non-Javadoc)
	 * @see tasmTED.TEDTree#getGlobalID(int)
	 */
	@Override
	public int getGlobalID(int i) {
		return i + start;
	}

	/* (non-Javadoc)
	 * @see tasmTED.TEDTree#getLabelDictionary()
	 */
	@Override
	public LabelDictionary getLabelDictionary() {
		return null;
}

	/* (non-Javadoc)
	 * @see tasmTED.TEDTree#getLabelID(int)
	 */
	@Override
	public int getLabelID(int i) {
		throw new RuntimeException("Label IDs not implemented in " + this.getClass().getName());
	}

}
