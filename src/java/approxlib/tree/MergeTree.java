/*
 * Created on 17-Oct-06
 */
package approxlib.tree;

import java.util.Enumeration;

public class MergeTree extends LblTree {
	
	private int treeID1;
	private int treeID2;
	
	public MergeTree(String label, int treeID1, int treeID2) {
		super(label, Node.NO_TREE_ID);
		this.treeID1 = treeID1;
		this.treeID2 = treeID2;
	}
	
	/**
	 * Converts a LblTree to a MergeTree. 
	 * @param t tree to convert
	 * @param treeID1 treeID of t if t is the first tree, Node.NO_TREE_ID otherwise
	 * @param treeID2 treeID of t if t is the second tree, Node.NO_TREE_ID otherwise
	 * @return
	 */
	public static MergeTree getMergeTree(LblTree t, int treeID1, int treeID2) {
		MergeTree mt = new MergeTree(t.getLabel(), treeID1, treeID2);
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			mt.add(getMergeTree((LblTree)e.nextElement(), treeID1, treeID2));
		}
		return mt;
	}

	/**
	 * Merge two trees into one. Each node of the MergeTree has two treeIDs.
	 * They are equal for nodes that are in t1 as well as in t2. The first treeID is
	 * Node.NO_TREE_ID for nodes that are only in t2, but not in t1, an vice versa.
	 * 
	 * The siblings of each node are sorted before they are merged. They must implement 
	 * the Compareable interface. 
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static MergeTree mergeTrees(LblTree t1, LblTree t2) {
		if (t1.getLabel().equals(t2.getLabel())) {
			MergeTree mt = new MergeTree(t1.getLabel(), t1.getTreeID(), t2.getTreeID());
			return mergeTrees(mt, getChildren(t1), getChildren(t2));
		} else {
			MergeTree mt = new MergeTree(t1.getLabel() + " <<=>> " + t2.getLabel(), Node.NO_TREE_ID, Node.NO_TREE_ID);
			return mergeTrees(mt, getChildren(t1), getChildren(t2));
		}
	}
	

	private static LblTree[] getChildren(LblTree t) {
		LblTree[] childArr = new LblTree[t.getChildCount()];
		int i = 0;
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			childArr[i] = (LblTree)e.nextElement();
			i++;
		}
		// if you have presorted trees, drop this!
		//java.util.Arrays.sort(chArr);
		return childArr;
	}
	
	private static MergeTree mergeTrees(MergeTree mt, LblTree[] childArr1, LblTree[] childArr2) {
		if (childArr1.length == 0) {
			for (int i = 0; i < childArr2.length; i++) {
				LblTree c2 = childArr2[i];				
				mt.add(getMergeTree(c2, Node.NO_TREE_ID, c2.getTreeID()));
			}
			return mt;
		}
		if (childArr2.length == 0) {
			for (int i = 0; i < childArr1.length; i++) {
				LblTree c1 = childArr1[i];				
				mt.add(getMergeTree(c1, c1.getTreeID(), Node.NO_TREE_ID));
			}
			return mt;
		}
		int i = 0, j=0;
		while ((i < childArr1.length) && (j < childArr2.length)) {
			int cmp = childArr1[i].compareTo(childArr2[j]);
			if (cmp == 0) { // chArr1[i] == chArr2[j]
				MergeTree c = new MergeTree(childArr1[i].getLabel(), childArr1[i].getTreeID(), childArr2[j].getTreeID()); 
				mt.add(mergeTrees(c, getChildren(childArr1[i]), getChildren(childArr2[j])));
				i++;
				j++;
			} else if (cmp < 0) { // chArr1[i] < chArr2[j]
				mt.add(getMergeTree(childArr1[i], childArr1[i].getTreeID(), Node.NO_TREE_ID));
				i++;
			} else { // chArr1[i] > chArr2[j]
				mt.add(getMergeTree(childArr2[j], Node.NO_TREE_ID, childArr2[j].getTreeID()));
				j++;
			}
		} 
		while (i < childArr1.length) {
			mt.add(getMergeTree(childArr1[i], childArr1[i].getTreeID(), Node.NO_TREE_ID));
			i++;
		}
		while (j < childArr2.length) {
			mt.add(getMergeTree(childArr2[j], Node.NO_TREE_ID, childArr2[j].getTreeID()));
			j++;
		}	
		return mt;
	}
	
	
	@Override
	public void prettyPrint() {
		for (int i = 0; i < getLevel(); i++) {
			System.out.print(TAB_STRING);
		}
		if (!isRoot()) {
			System.out.print(BRANCH_STRING);
		} else {
			System.out.println("treeID: " + getTreeID());
			System.out.print(ROOT_STRING);
		}
		System.out.print(this.showNode());
		System.out.println();
		for (Enumeration e = children(); e.hasMoreElements();) {
			((LblTree)e.nextElement()).prettyPrint();
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see tree.LblTree#showNode()
	 */
	@Override
	public String showNode() {
		String s;
		if (treeID1 == Node.NO_TREE_ID) {
			s = "<";
		} else if (treeID2 == Node.NO_TREE_ID) {
			s = ">";
		} else {
			s = " ";
		}
		return  s + s + "'" + this.getLabel() + "'"; 
	}
	
	/**
	 * @return Returns the treeID1.
	 */
	public int getTreeID1() {
		return treeID1;
	}

	/**
	 * @return Returns the treeID2.
	 */
	public int getTreeID2() {
		return treeID2;
	}

}
