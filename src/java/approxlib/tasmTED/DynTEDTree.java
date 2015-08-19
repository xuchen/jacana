package approxlib.tasmTED;

import approxlib.tree.LblTree;
import approxlib.util.DynIntArray;
import approxlib.util.LabelDictionary;

public class DynTEDTree extends TEDTree {

	/* (non-Javadoc)
	 * @see tasmTED.TEDTree#getGlobalID(int)
	 */
	@Override
	public int getGlobalID(int i) {
		return i + start;
	}

	private DynIntArray llds;
	private DynIntArray lblIDs;
	private LabelDictionary lblDict;
	private int nodeCount;
	private int start;
	/**
	 * Number of leaf nodes. This is not necessarily initialized.
	 * Iff leafCount > 0 then it is assumed to be correct.
	 */
	private int leafCount;
	
	/**
	 * Used to produce subtrees.
	 * 
	 * @param start
	 * @param nodeCount
	 * @param labelDictionary
	 * @param llds
	 * @param labels
	 * @param leafCount
	 */
	private DynTEDTree(int start, int nodeCount, 
			LabelDictionary labelDictionary, DynIntArray llds, 
			DynIntArray labels, int leafCount) {
		this.start = start;
		this.nodeCount = nodeCount;
		this.lblDict = labelDictionary;
		this.llds = llds;
		this.lblIDs = labels;
		this.leafCount = leafCount;
	}
	
	public DynTEDTree(LabelDictionary labelDictionary, int capacity) {
		this(0, 0, labelDictionary, 
				new DynIntArray(capacity), new DynIntArray(capacity), 
				Integer.MIN_VALUE);
	}

	public DynTEDTree(LabelDictionary labelDictionary, StaticTEDTree ted) {
		this(labelDictionary, ted.getNodeCount());
		// copy data from static TEDTree
		for (int i = 1; i <= ted.getNodeCount(); i++) {
			this.setLld(i, ted.lld(i));
			this.setLabel(i, ted.getLabel(i));
		}
		nodeCount = ted.getNodeCount();
		leafCount = ted.getLeafCount();
	}
	
	public DynTEDTree(LabelDictionary labelDictionary, LblTree t) {
		this(labelDictionary, new StaticTEDTree(t));
	}

	/* (non-Javadoc)
	 * @see tasmTED.LabelDictionaryTEDTree#getLabelID(int)
	 */
	@Override
	public int getLabelID(int i) {
		return lblIDs.get(i + start - 1);
	}
	
	/* (non-Javadoc)
	 * @see distance.TEDTree#getLabel(int)
	 */
	@Override
	public String getLabel(int i) {
		return lblDict.read(this.getLabelID(i));
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
	public DynTEDTree getSubtree(int i) {
		int subStart = lld(i) + start - 1;
		int subNodeCount = i - lld(i) + 1;
		return new DynTEDTree(subStart, subNodeCount, this.lblDict, this.llds, this.lblIDs, Integer.MIN_VALUE);
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#lld(int)
	 */
	@Override
	public int lld(int i) {		
		return llds.get(i + start - 1) - start + 1;
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#setLabel(int, java.lang.String)
	 */
	@Override
	public void setLabel(int i, String newLabel) {
		int labelID = lblDict.store(newLabel);
		lblIDs.setElementAt(i + start - 1, labelID);
		if (i >= nodeCount) {
			nodeCount = i;
		}		
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#setLld(int, int)
	 */
	@Override
	public void setLld(int i, int newLld) {
		if (leafCount > 0) {
			leafCount = Integer.MIN_VALUE;
		}
		llds.setElementAt(i + start - 1, newLld + start - 1);		
		if (i >= nodeCount) { // new node is added	
			nodeCount = i;
		} 
	}

	/**
	 * Get the capacity of the dynamic arrays in this class. 
	 * Only for testing the class.
	 * 
	 * @return length of the dynamic arrays in this class
	 */
	public int getCapacity() {
		return llds.getData().length;
	}

	/* (non-Javadoc)
	 * @see distance.TEDTree#getLeafCount()
	 */
	@Override
	public int getLeafCount() {
		if (this.leafCount <= 0) {
			this.leafCount = super.getLeafCount();
		}
		return this.leafCount;
	}

	/* (non-Javadoc)
	 * @see tasmTED.LabelDictionaryTEDTree#getLabelDictionary()
	 */
	@Override
	public LabelDictionary getLabelDictionary() {
		return this.lblDict;
	}	

}
