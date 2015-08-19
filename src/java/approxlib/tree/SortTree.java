package approxlib.tree;

import java.util.Arrays;
import java.util.Enumeration;

public class SortTree {
	
	/**
	 * Sort siblings using the sort order defined by {@link tree.LblValTree#compareTo(Object)}.
	 * The order of siblings that are equal with respect to the sort order is retained.
	 * 
	 * @param t
	 * @return
	 */
	public static LblValTree sortTree(LblValTree t) {
		LblValTree[] siblingSet = new LblValTree[t.getChildCount()];
		int pos = 0;
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			siblingSet[pos++] = (LblValTree)e.nextElement();
		}
		Arrays.sort(siblingSet);
		LblValTree s = new LblValTree(t.getLabel(), t.getValue(), t.getTreeID());
		// attach sorted children
		for (int i = 0; i<siblingSet.length; i++) {
			s.add(sortTree(siblingSet[i]));
		}
		return s;
	}

	/**
	 * Sort siblings using the sort order defined by {@link tree.LblValTree#compareTo(Object)}.
	 * The order of siblings that are equal with respect to the sort order is retained.
	 * 
	 * @param t
	 * @return
	 */
	public static LblTree sortTree(LblTree t) {
		LblTree[] siblingSet = new LblTree[t.getChildCount()];
		int pos = 0;
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			siblingSet[pos++] = (LblTree)e.nextElement();
		}
		Arrays.sort(siblingSet);
		LblTree s = new LblTree(t.getLabel(), t.getTreeID());
		// attach sorted children
		for (int i = 0; i<siblingSet.length; i++) {
			s.add(sortTree(siblingSet[i]));
		}
		return s;
	}

}
