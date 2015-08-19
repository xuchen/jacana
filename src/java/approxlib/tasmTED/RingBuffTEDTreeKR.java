/**
 * 
 */
package approxlib.tasmTED;

import approxlib.util.LabelDictionary;


/**
 * This class differs from its super calls in that 
 * {@link #setLabel(int, String)} updates the key root values, 
 * which can be looked up with {@link #getLeafKeyRoot(int)} (only
 * for leaf nodes!).
 * 
 * @author Nikolaus Augsten
 */
public class RingBuffTEDTreeKR extends RingBuffTEDTree {

	/**
	 * @param bufferSize
	 */
	public RingBuffTEDTreeKR(int bufferSize, LabelDictionary lblDict) {
		super(bufferSize, lblDict);
	}
	
	/**
	 * @param llds
	 * @param labels
	 * @param nodeCount
	 * @param start
	 */
	private RingBuffTEDTreeKR(int start, int nodeCount, 
			int[] llds, String[] labels, int[] lblIDs, LabelDictionary lblDict) {
		super(start, nodeCount, llds, labels, lblIDs, lblDict);
	}


	/**
	 * @param bufferSize
	 * @param ted
	 */
	public RingBuffTEDTreeKR(int bufferSize, TEDTree ted, LabelDictionary lblDict) {
		this(bufferSize, lblDict);
		// copy data from TEDTree until buffer is full
		int first = Math.max(1, ted.getNodeCount() - bufferSize + 1);
		for (int i = first; i <= ted.getNodeCount(); i++) {
			super.setLld(i, ted.lld(i));
			super.setLabel(i, ted.getLabel(i));
		}
		for (int i = first; i <= ted.getNodeCount(); i++) {
			this.setLld(i, ted.lld(i));
		}
	}

	/* (non-Javadoc)
	 * @see tasmTED.RingBuffTEDTree#lld(int)
	 */
	@Override
	public int lld(int i) {
		return Math.min(i, super.lld(i));
	}

	/* (non-Javadoc)
	 * @see tasmTED.RingBuffTEDTree#setLld(int, int)
	 */
	@Override
	public void setLld(int i, int newLld) {
		super.setLld(i, newLld);
		if (newLld >= this.smallestValidID() &&
				this.getLeafKeyRoot(newLld) < i) {
			super.setLld(newLld, i);
		}
	}
	
	/**
	 * Computes the key root that is a ancestor of a given leaf node. 
	 * There is exaclty one such node for each leaf in a tree. 
	 * (A node is an ancestor of itself.)
	 * 
	 * @param i postorder ID of the leaf node
	 * @return postorder ID of the key root that an ancestor of this node  
	 * @throws RuntimeException if i is not a leaf 
	 */
	public int getLeafKeyRoot(int i) {
		// if i is a leaf node
		if (this.lld(i) == i) {
			// return the key root value stored for i
			return super.lld(i);
		} else {
			throw new RuntimeException("Called getLeafKeyRoot(int) for a non-leaf.");
		}
	}	
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#getSubtree(int)
	 */
	@Override
	public RingBuffTEDTreeKR getSubtree(int i) {
		if (this.getNodeCount() - lld(i) >= llds.length) {
			throw new ArrayIndexOutOfBoundsException("Element accessed by getSubtree(int) expired in ring buffer: " + i);
		}		
		int subStart = lld(i) + start - 1;
		int subNodeCount = i - lld(i) + 1;
		RingBuffTEDTreeKR subtree = 
			new RingBuffTEDTreeKR(subStart, subNodeCount, 
					this.llds, this.labels, this.lblIDs, this.getLabelDictionary());
		return subtree;
	}
	
}
