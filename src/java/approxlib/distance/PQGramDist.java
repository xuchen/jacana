package approxlib.distance;

import approxlib.hash.FixedLengthHash;
import approxlib.hash.StringHash;

import java.util.Enumeration;
import java.util.LinkedList;

import approxlib.tree.LblTree;

/**
 * Calculates the pq-gram distance of two trees.
 */
public class PQGramDist extends ProfileDist {	
	
	private int p;  // p value of pq-gram distance
	private int q;  // q value of pq-gram distance
	private FixedLengthHash hf;

	public PQGramDist(int p, int q, FixedLengthHash hf, boolean normalized) {
		super(normalized);
		this.p = p;
		this.q = q;
		this.hf = hf;
	}	
	
	public PQGramDist(int p, int q, boolean normalized) {
		super(normalized);
		this.p = p;
		this.q = q;
		this.hf = new StringHash(5);
	}
	
	/**
	 * Check, whether p and q are the same.
	 * 
	 * @see distance.ProfileDist#getStoredProfile(LblTree)
	 */
	@Override
	public Profile getStoredProfile(LblTree t) {
		VectorProfile prof = null;
		if (t.getTmpData() != null) {
			try {
				prof = (VectorProfile)t.getTmpData();
				if (prof.size() == 0) {
					prof = null;
				} else {
					PQGram pqg = (PQGram)prof.elementAt(0);
					if ((pqg.getP() != p) || (pqg.getQ() != q)) {
						prof = null;
					}	
				}
			} catch (ClassCastException e) {
				prof = null;
			}
		} 
		return prof;
	}
	
	/* (non-Javadoc)
	 * @see distance.ProfileDist#createProfile(tree.LblTree)
	 */
	@Override
	public Profile createProfile(LblTree t) {
		VectorProfile prof;
		int n = t.getNodeCount();
		// |prof|=2l+qi-1; we estimate l=i=n/2 
		prof = new PQGramProfile(n * (this.q + 2) / 2, n);
		getPQGrams(prof, t);
		return prof;
	}
	
	/**
	 * Recursive method to calculate pq-gram profile of a tree. Is called 
	 * by {@link #createProfile(LblTree)}.
	 * 
	 * @param prof pq-grams will be added to this profile
	 * @param node start with this node
	 * @param p
	 * @param q
	 */
	private void getPQGrams(VectorProfile prof, LblTree node) {
		
		String[] pNodes = new String[p];
		
		LblTree n = node;
		int i = p - 1;
		while ((n != null) && (i >= 0)) {
			pNodes[i] = n.getLabel();
			n = (LblTree)n.getParent();
			i--;
		}
		
		LinkedList qNodesList = new LinkedList();
		for (i = 0; i < q; i++) {
			qNodesList.add(null);
		}	
		
		if (q > 0) {
			for (Enumeration e = node.children(); e.hasMoreElements();) {
				String nextLabel = ((LblTree)e.nextElement()).getLabel();
				qNodesList.add(nextLabel);
				qNodesList.removeFirst();
				String[] qNodes = (String[])qNodesList.toArray(new String[q]);
				prof.add(new PQGram(pNodes, qNodes, hf));
			}
			if (node.getChildCount() > 0) {
				for (i = 0; i < q - 1; i++) {
					qNodesList.add(null);
					qNodesList.removeFirst();
					String[] qNodes = (String[])qNodesList.toArray(new String[q]);
					prof.add(new PQGram(pNodes, qNodes, hf));
				}
			} else {
				String[] qNodes = (String[])qNodesList.toArray(new String[q]);
				prof.add(new PQGram(pNodes, qNodes, hf));
			}
		} else if (p > 0) {
			prof.add(new PQGram(pNodes, new String[q], hf));
		}
		
		for (Enumeration e = node.children(); e.hasMoreElements();) {
			getPQGrams(prof, (LblTree)e.nextElement());
		}   		
		
	}		
	
		
	/**
	 * @return Returns the p.
	 */
	public int getP() {
		return p;
	}
	/**
	 * @param p The p to set.
	 */
	public void setP(int p) {
		this.p = p;
	}
	/**
	 * @return Returns the q.
	 */
	public int getQ() {
		return q;
	}
	/**
	 * @param q The q to set.
	 */
	public void setQ(int q) {
		this.q = q;
	}

	@Override
	public String toString() {
		return super.toString() + "[p=" + this.getP() + ",q=" + this.getQ() + "," + this.hf + "]";
	}		

}
