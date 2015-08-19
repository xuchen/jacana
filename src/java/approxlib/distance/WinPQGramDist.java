package approxlib.distance;

import approxlib.hash.FixedLengthHash;
import approxlib.hash.StringHash;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

import approxlib.tree.LblTree;

/**
 * Calculates the windowed pq-gram distance of two trees (q=2).
 * 
 * CAUTION: !!! Very preliminary code testing !!! 
 * 
 */
public class WinPQGramDist extends ProfileDist {	

	private int p;  // p value of pq-gram distance
	private int w; // window size
	private int q = 2;
	private FixedLengthHash hf;

	/**
	 * Windowed pq-gram distance for q=2, w>=2. If w < 2 then w=2 will be used.
	 * 
	 * @param p
	 * @param w
	 * @param hf
	 * @param normalized
	 */
	public WinPQGramDist(int p, int w, FixedLengthHash hf, boolean normalized) {
		super(normalized);
		this.p = p;
		this.w = w;
		if (w < this.q) {
			throw new RuntimeException("The window size w for the windowed pq-gram distance must be at least 2.");
		}
		this.hf = hf;
	}	

	public WinPQGramDist(int p, int w, boolean normalized) {
		this(p, w, new StringHash(5), normalized);
	}
	/* (non-Javadoc)
	 * @see distance.ProfileDist#createProfile(tree.LblTree)
	 */
	@Override
	public Profile createProfile(LblTree t) {
		VectorProfile prof;
		int n = t.getNodeCount();
		// |prof|=(n-1)(w-1)+l < n*w 
		prof = new PQGramProfile(n * this.getW(), n);
		getPQGrams(prof, t);
		return prof;
	}

	/**
	 * Recursive method to calculate the windowed pq-gram profile of a tree (q=2). 
	 * Is called by {@link #createProfile(LblTree)}.
	 * 
	 * @param prof pq-grams will be added to this profile
	 * @param anchor start with this node
	 * @param p
	 * @param q
	 */
	private void getPQGrams(VectorProfile prof, LblTree anchor) {


		// initialize stem for anchor node 
		String[] stem = new String[p];
		LblTree n = anchor;
		for (int i = p - 1; i >= 0; i--) {
			if (n != null) {
				stem[i] = n.getLabel();
				n = (LblTree)n.getParent();
			} else {
				stem[i] = PQGram.NULL_SYMBOL;
			}
		}

		// anchor node is a leaf
		if (anchor.isLeaf()) {
			// produce a pq-gram with a dummy base
			prof.add(new PQGram(stem, new String[] {PQGram.NULL_SYMBOL, PQGram.NULL_SYMBOL}, hf));
		} else {

			// initialize sibling sequence of the anchor node
			int len = Math.max(anchor.getChildCount(), w);
			LblTree[] siblingSet = new LblTree[len];
			int childPos = 0;
			for (Enumeration e = anchor.children(); e.hasMoreElements();) {
				LblTree child = (LblTree)e.nextElement();
				siblingSet[childPos++]=child;
				getPQGrams(prof, child);
			}

			// sort sibling sequence of the anchor node
			Arrays.sort(siblingSet, new Comparator<LblTree>() {
				public int compare(LblTree t1, LblTree  t2) {
					if ((t1 == null) && (t2 == null)) return 0;
					if (t1 == null) return 1;
					if (t2 == null) return -1;
					return t1.compareTo(t2);
				}
			}
			);

			// get hashed labels of sibling sequence
			String[] siblingLabels = new String[len];
			for (int i = 0; i < len; i++) {
				// if dummy node
				if (siblingSet[i] == null) {
					siblingLabels[i] = PQGram.NULL_SYMBOL;
				} else {
					siblingLabels[i] = siblingSet[i].getLabel();
				}
			}
			
			// produce pq-grams
			String[] base = new String[2]; 
			for (int i = 0; i < len; i++) {
				base[0] = siblingLabels[i];
				for (int j = i + 1; j < i + this.w; j++) {
					base[1] = siblingLabels[j % len];
					prof.add(new PQGram(stem, base, hf));
				}
			}		
		}
}		


/**
 * @return Returns the p.
 */
public int getP() {
	return p;
}

@Override
public String toString() {
	return super.toString() + "[w=" + this.getW() 
	        + ",p=" + this.getP() + ",q=" + this.q 
	        + "," + this.hf + "]";
}

public int getW() {
	return w;
}		

}
