/*
 * Created on 22-Apr-06
 */
package approxlib.tree;

import approxlib.hash.FixedLengthHash;
import approxlib.hash.HashValue;

import java.util.Enumeration;
/**
 * A main memory tree with (label,value) nodes.
 * 
 * @author augsten
 */
public class LblValTree extends LblTree {

	String value;
	
	/**
	 * @param label
	 * @param treeID
	 */
	public LblValTree(String label, String value, int treeID) {
		super(label, treeID);
		this.value = value;
	}
	
	
	/**
	 * Creates a new lblValTree from a {@link lblTree}; 
	 * the values of the new tree are null.
	 * 
	 * @param t
	 */
	public LblValTree(LblTree lblTree) {
		this(lblTree.getLabel(), null, lblTree.getTreeID());
		for (Enumeration e = lblTree.children(); e.hasMoreElements(); ) {
			this.add(new LblValTree((LblTree)e.nextElement()));
		}		
	}
	
	/**
	 * Converts this tree to a {@link lblTree}. The label of lblTree is the
	 * hash concatenation of label and value of this tree. If the hash is null,
	 * label and value are concatenated, separated with comma (",").
	 * 
	 * @param hf
	 * @return
	 */
	public LblTree getLblTree(FixedLengthHash hf) {
		LblTree lblTree;
		if (hf != null) {
			lblTree = new LblTree(hf.h(this.getLabel(), this.getValue()).toString(), 
					this.getTreeID());			
		} else {
			lblTree = new LblTree(this.getLabel() + "," + this.getValue(), 
					this.getTreeID());						
		}
		for (Enumeration e = this.children(); e.hasMoreElements(); ) {
			lblTree.add(((LblValTree)e.nextElement()).getLblTree(hf));
		}
		return lblTree;
	}
	
	/**
	 * "Comb" through the LblValueTree, such that each leaf with (lbl,val) 
	 * is split into an inner node with label (lbl) and a leaf with label (val).
	 * 
	 * @return
	 */
	public static LblTree expandToLblTree(LblValTree lvt) {	
		int treeID = lvt.getTreeID();
		LblTree n = new LblTree(lvt.getLabel(), treeID);
		if (lvt.isLeaf()) {
			String val = lvt.getValue();
			if (val != null) {
				n.add(new LblTree(val, treeID));
			}
		} else {
			for (Enumeration<LblValTree> e = lvt.children(); e.hasMoreElements(); ) { 
				n.add(expandToLblTree(e.nextElement()));
			}
		}	
		return n;
		
	}
	
	/**
	 * 
	 */
	public static LblValTree deepCopy(LblValTree t) {
		LblValTree nt = new LblValTree(t.getLabel(), t.getValue(), t.getTreeID());
		for (Enumeration e = t.children(); e.hasMoreElements(); ) {
			nt.add(deepCopy((LblValTree)e.nextElement()));
		}
		return nt;
	}
	
	/**
	 * @return Returns the value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value The value to set.
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Compares the labels. If they are equal, values are compared. 
	 * Values can be null (null > any-string). 
	 */
	@Override
	public int compareTo(Object o) {
		LblValTree t = (LblValTree)o;
		int cmp = this.getLabel().compareTo(t.getLabel());
		if (cmp != 0) {
			return cmp;
		}
		if ((this.getValue() == null) && (t.getValue() == null)) {
			return 0;
		}
		if ((this.getValue() == null) && (t.getValue() != null)) {
			return Integer.MIN_VALUE;
		}
		if ((this.getValue() != null) && (t.getValue() == null)) {
			return Integer.MAX_VALUE;
		}
		return this.getValue().compareTo(t.getValue());
	}
	
	/* (non-Javadoc)
	 * @see tree.LblTree#showNode()
	 */
	@Override
	public String showNode() {
		if ((this.getValue() != null) && (!this.getValue().equals(""))) {
			return this.getLabel() + "," + this.getValue();
		} else {
			return this.getLabel();
		}
	}

	/* (non-Javadoc)
	 * @see tree.LblTree#getNodeHash(hash.FixedLengthHash)
	 */
	@Override
	public HashValue getNodeHash(FixedLengthHash hf) {
		return hf.h(this.getLabel(), this.getValue());
	}


	/**
	 * Constructs an LblValTree from a string representation of tree. The
	 * treeID in the String representation is optional; if no treeID is given,
	 * the treeID of the returned tree will be NO_ID.
	 * Only labels are supported; the value of the LblValTree is always null.
	 *
	 * @param s string representation of a tree. Format: "treeID:{rootlabel{...}}".
	 * @return tree represented by s
	 */
	public static LblValTree fromString(String s) {
		return new LblValTree(LblTree.fromString(s));
	}
	
	
}
