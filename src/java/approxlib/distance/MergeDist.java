/*
 * Created on Jan 3, 2008
 */
package approxlib.distance;

import java.util.Enumeration;

import approxlib.tree.LblTree;
import approxlib.tree.MergeTree;
import approxlib.tree.Node;

/**
 * @author naugsten
 */
public class MergeDist extends EditBasedDist {

	public MergeDist(boolean normalized) {
		super(normalized);
	}

	/**
	 * @param ins
	 * @param del
	 * @param update
	 */
	public MergeDist(double ins, double del, double update, boolean normalized) {
		super(ins, del, update, normalized);
	}

	@Override
	public double nonNormalizedTreeDist(LblTree t1, LblTree t2) {
		MergeTree mt = MergeTree.mergeTrees(t1, t2);
		return getMergeDistance(mt, 0);
	}

	private double getMergeDistance(MergeTree t, double dist) {
		for (Enumeration e = t.children(); e.hasMoreElements();) {
			dist += getMergeDistance((MergeTree)e.nextElement(), 0);
		}
		if ((t.getTreeID1() == Node.NO_TREE_ID) && (t.getTreeID2() == Node.NO_TREE_ID)) {
			dist += this.getUpdate();
		} else {
			if (t.getTreeID1() == Node.NO_TREE_ID) {
				dist += this.getIns();
			} 
			if (t.getTreeID2() == Node.NO_TREE_ID) {
				dist += this.getDel();
			}	
		}
		return dist;
	}
	
}
