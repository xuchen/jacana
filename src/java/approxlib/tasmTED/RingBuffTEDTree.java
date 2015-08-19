/**
 * 
 */
package approxlib.tasmTED;

import approxlib.util.LabelDictionary;


/**
 * @author Nikolaus Augsten
 *
 */
public class RingBuffTEDTree extends TEDTree {

	protected int[] llds;
	protected String[] labels;
	private int nodeCount;
	protected int start;	
	private LabelDictionary lblDict;
	protected int[] lblIDs;

	/**
	 * @param llds
	 * @param labels
	 * @param nodeCount
	 * @param start
	 */
	protected RingBuffTEDTree(int start, int nodeCount, 
			int[] llds, String[] labels, int[] lblIDs, LabelDictionary lblDict) {
		super();
		this.llds = llds;
		this.labels = labels;
		this.nodeCount = nodeCount;
		this.start = start;
		this.lblIDs = lblIDs;
		this.lblDict = lblDict;
	}
	
	public RingBuffTEDTree(int bufferSize, LabelDictionary lblDict) {
		this(0, 0, new int[bufferSize], new String[bufferSize], new int[bufferSize], lblDict);
	}

	public RingBuffTEDTree(int bufferSize, TEDTree ted, LabelDictionary lblDict) {
		this(bufferSize, lblDict);
		// copy data from TEDTree until buffer is full
		int first = Math.max(1, ted.getNodeCount() - bufferSize + 1);
		for (int i = first; i <= ted.getNodeCount(); i++) {
			this.setLld(i, ted.lld(i));
			this.setLabel(i, ted.getLabel(i));
		}

	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#lld(int)
	 */
	@Override
	public int lld(int i) {
		if (this.getNodeCount() - i >= llds.length ||
				i > this.getNodeCount()) {
			throw new ArrayIndexOutOfBoundsException("Element expired in ring buffer: " + i);
		}
		return llds[(i + start - 1) % llds.length] - start + 1;
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#getLabel(int)
	 */
	@Override
	public String getLabel(int i) {
		if (this.getNodeCount() - i >= labels.length ||
				i > this.getNodeCount()) {
			throw new ArrayIndexOutOfBoundsException("Element expired in ring buffer.");
		}
		return labels[(i + start - 1) % labels.length];
	}
		
	/* (non-Javadoc)
	 * @see distance.TEDTree#setLld(int, int)
	 */
	@Override
	public void setLld(int i, int newLld) {
		if (this.getNodeCount() - i >= llds.length) {
			throw new ArrayIndexOutOfBoundsException("Element accessed by setLld(int,int) expired in ring buffer.");
		}
		if (nodeCount < i) {
			nodeCount = i;
		}
		llds[(i + start - 1) % llds.length] = newLld + start - 1;
	}
	

	/* (non-Javadoc)
	 * @see distance.TEDTree#setLabel(int, java.lang.String)
	 */
	@Override
	public void setLabel(int i, String newLabel) {
		if (this.getNodeCount() - i >= llds.length) {
			throw new ArrayIndexOutOfBoundsException("Element accessed by setLld(int,int) expired in ring buffer.");
		}
		if (nodeCount < i) {
			nodeCount = i;
		}
		labels[(i + start - 1) % labels.length] = newLabel;		
		if (lblDict != null) {
			int labelID = lblDict.store(newLabel);
			lblIDs[(i + start - 1) % lblIDs.length] = labelID;
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
	 * @see distance.TEDTree#getSubtree(int)
	 */
	@Override
	public RingBuffTEDTree getSubtree(int i) {
		if (this.getNodeCount() - lld(i) >= llds.length) {
			throw new ArrayIndexOutOfBoundsException("Element accessed by setLld(int,int) expired in ring buffer.");
		}		
		int subStart = lld(i) + start - 1;
		int subNodeCount = i - lld(i) + 1;
		RingBuffTEDTree subtree = 
			new RingBuffTEDTree(subStart, subNodeCount, 
					this.llds, this.labels, this.lblIDs, this.lblDict);
		return subtree;
	}

	/**
	 * Only for debugging and testing.
	 * 
	 * @return internal ring buffer for leaf-most leaf descendants
	 */
	public int[] getLldsBuff() {
		return this.llds;
	}
	
	/**
	 * Only for debugging and testing.
	 * 
	 * @return internal ring buffer for node labels
	 */
	public String[] getLabelsBuff() {
		return this.labels;
	}
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		RingBuffTEDTree ted = (RingBuffTEDTree) obj;
		if (this.getNodeCount() != ted.getNodeCount()) {
			return false;
		}
		if (this.llds.length != ted.llds.length) {
			return false;
		}
		if (this.labels.length != ted.labels.length) {
			return false;
		}
		int start = Math.max(1, this.getNodeCount() - llds.length + 1);
		for (int i = start; i <= this.getNodeCount(); i++) {
				if (this.lld(i) != ted.lld(i)) {
					return false;
				}
		}
		for (int i = start; i <= this.getNodeCount(); i++) {			
				if (!this.equalsLbl(i, ted, i)) {
					return false;
				}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#toString()
	 */
	@Override
	public String toString() {
		int smallestID = smallestValidID();
		StringBuffer sb = new StringBuffer("[");
		if (smallestID != 1) {
			sb.append("...,");
		}
		for (int i = smallestID; i <= this.getNodeCount(); i++) {
			sb.append(String.format("(%s,%d)", this.getLabel(i), this.lld(i)));
			if (i != this.getNodeCount()) {
				sb.append(",");
			}
		}
		return sb.toString() + "]";
	}

	/* (non-Javadoc)
	 * @see tasmTED.TEDTree#smallestValidID()
	 */
	@Override
	public int smallestValidID() {
		int smallestValidID = super.smallestValidID();
		return Math.max(smallestValidID, this.getNodeCount() - llds.length + 1);
	}
	
	/**
	 * Maximum number of values in this ring buffer.
	 * @return
	 */
	public int getBufferSize() {
		return llds.length;
	}

	/* (non-Javadoc)
	 * @see tasmTED.TEDTree#getGlobalID(int)
	 */
	@Override
	public int getGlobalID(int i) {
		return start + i;
	}
	
	/* (non-Javadoc)
	 * @see tasmTED.LabelDictionaryTEDTree#getLabelDictionary()
	 */
	@Override
	public LabelDictionary getLabelDictionary() {
		return this.lblDict;
}

	/* (non-Javadoc)
	 * @see tasmTED.LabelDictionaryTEDTree#getLabelID(int)
	 */
	@Override
	public int getLabelID(int i) {
		if (lblDict == null) {
			throw new RuntimeException("label IDs are not defined as dictionary is 'null'");
		} else if (this.getNodeCount() - i >= labels.length ||
				i > this.getNodeCount()) {
			throw new ArrayIndexOutOfBoundsException("Element expired in ring buffer.");
		} 
		return lblIDs[(i + start - 1) % lblIDs.length];
	}
	
}
