package approxlib.distance;

import java.util.Arrays;
import java.util.Enumeration;

import approxlib.tree.LblTree;

/**
 * This is the straight forward dynamic programming solution for the tree edit distance.
 * If you need scalability, used {@link EditDist}. 
 * @author naugsten
 *
 */
public class RecursiveEditDist extends EditBasedDist {

	public static boolean DEBUG = true;
	
	// call initKeyroots to init the following arrays!
	int[] l1, l2;   // post-order number of left-most leaf descendant of t1 (t2)
	String[] lbl1, lbl2; // label of i-th node in postorder of t1 (t2)
	int nc1, nc2; // node count of t1 (t2)

	double forestdist[][]; // store distance between subforests

	public RecursiveEditDist(boolean normalized) {
		this(1, 1, 1, normalized);
	}
	
	public RecursiveEditDist(double ins, double del, double update, boolean normalized) {
		super(ins, del, update, normalized);
	}
	
	// l (left-most leaves), lbl (labels) with t (tree)
	private static void init(int[] l, String[] lbl, LblTree t) {
		int i = 0;
		for (Enumeration e = t.postorderEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			// add postorder number to node
			n.setTmpData(new Integer(i));	    	    
			// label
			lbl[i] = n.getLabel();
			// left-most leaf
			l[i] = ((Integer)((LblTree)n.getFirstLeaf()).getTmpData()).intValue();	    
			i++;
		}
		t.clearTmpData();
	}


	@Override
	public double nonNormalizedTreeDist(LblTree t1, LblTree t2) {

		// System.out.print(t1.getTreeID() + "|" + t2.getTreeID() + "|");

		nc1 = t1.getNodeCount();
		l1 = new int[nc1];
		lbl1 = new String[nc1];

		nc2 = t2.getNodeCount();
		l2 = new int[nc2];
		lbl2 = new String[nc2];

		init(l1, lbl1, t1);
		init(l2, lbl2, t2);

		forestdist = new double[nc1*(nc1+1)/2 + 1][nc2*(nc2+1)/2 + 1];
		for (int i = 0; i < forestdist.length; i++) {
			Arrays.fill(forestdist[i], -1.0);
		}

		int[] astart1 = new int[forestdist.length];
		int[] astop1 = new int[forestdist.length];
		astart1[0] = 0;
		astop1[0] = 0;
		int k = 1;
		for (int i = nc1 - 1; i >= 0; i--) {
			for (int j = i; j < nc1; j++) {
				astart1[k] = i;
				astop1[k] = j;
				k++;
			}
		}		
		double dist = treeEditDist(0, nc1 - 1, 0, nc2 - 1);
		if (DEBUG) {
			for (int i = 0; i < forestdist.length; i++) {
				System.out.print(astart1[i] + ".." + astop1[i] + " ");
				//int pos = getId(astart1[i], astop1[i], nc1);
				//System.out.print(pos + " ");
				for (int j = 0; j < forestdist[i].length; j++) {
					if (forestdist[i][j] < 0) { 
						System.out.print("* ");	
					}  else	{
						System.out.print(((int)forestdist[i][j]) + " ");
					}
				}
				System.out.println();
			}
		}
		return dist;
	}

	private int getId(int start, int stop, int nc) {
		if (start > stop) {
			return 0;
		}
		int x = nc - 1 - start;
		int pos = x * (x + 1) / 2 + (stop - start) + 1;
		return pos;
	}
	
	private double min(double a, double b, double c) {
		return Math.min(Math.min(a, b), c);
	}
	
	private double treeEditDist(int start1, int stop1, int start2, int stop2) {
		double computed = forestdist[getId(start1, stop1, nc1)][getId(start2, stop2, nc2)];
		if (computed >= 0) {
			return computed;
		}
		double dist;
		if ((start1 > stop1) && (start2 > stop2)) {
			dist = 0;
		} else if (start2 > stop2) {
			dist = treeEditDist(start1, stop1 - 1, start2, stop2) + this.getIns();
		} else if (start1 > stop1) {
			dist = treeEditDist(start1, stop1, start2, stop2 - 1) + this.getDel();
		} else {
			double d = treeEditDist(start1, stop1 - 1, start2, stop2) + this.getDel();
			double i = treeEditDist(start1, stop1, start2, stop2 - 1) + this.getIns();
			double r = treeEditDist(this.l1[stop1], stop1 -1, this.l2[stop2], stop2 - 1) + 
					treeEditDist(start1, this.l1[stop1] - 1, start2, this.l2[stop2] - 1) +
					(lbl1[stop1].equals(lbl2[stop2]) ? 0 : this.getUpdate());
			dist = (min(d, i, r));
		}
		forestdist[getId(start1, stop1, nc1)][getId(start2, stop2, nc2)] = dist;
		return dist;
		
	}	

}

