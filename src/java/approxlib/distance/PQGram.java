/*
 * Created on Jan 17, 2005
 */
package approxlib.distance;

import approxlib.hash.FixedLengthHash;

/**
 * @author augsten
 */

public class PQGram implements Comparable {
	
	public static final String NULL_SYMBOL = "*";
	
	private String[] stem;
	private String[] base;
	
	/**
	 * Copy stem and base and form a new pq-gram.
	 * @param stem
	 * @param base
	 */
	public PQGram(String[] stem, String[] base, FixedLengthHash hf) {		
		this.stem = new String[stem.length];
		this.base = new String[base.length];
		
		if (hf == null) {
			for (int i = 0; i < stem.length; i++) {
				this.stem[i] = (stem[i] == null) ? NULL_SYMBOL : stem[i];
			}
			for (int i = 0; i < base.length; i++) {
				this.base[i] = (base[i] == null) ? NULL_SYMBOL : base[i];
			}
		} else {
			String hashedNullSymbol = hf.getHashValue(NULL_SYMBOL).toString();
			for (int i = 0; i < stem.length; i++) {				
				this.stem[i] = (stem[i] == null) ? 
						hashedNullSymbol: hf.getHashValue(stem[i]).toString();
			}
			for (int i = 0; i < base.length; i++) {
				this.base[i] = (base[i] == null) ? 
						hashedNullSymbol : hf.getHashValue(base[i]).toString();
			}			
		}
	}
			
	@Override
	public String toString() {
		String res = "(";
		for (int i = 0; i < stem.length; i++) {
			res += stem[i];
			if (i < stem.length - 1) {
				res += ",";
			}
		}
		res += "){";
		for (int i = 0; i < base.length; i++) {
			res += base[i];
			if (i < base.length - 1) {
				res += ",";
			}
		}
		res += "}";
		return res;
	}
	
	private static int compare(String str1, String str2) {
		if ((str1 == null) && (str2 == null)) {
			return 0;
		} 
		if ((str1 != null) && (str2 == null)) {
			return Integer.MAX_VALUE;
		}
		if ((str1 == null) && (str2 != null)) {
			return Integer.MIN_VALUE;
		}
		return str1.compareTo(str2);
		
	}
	
	public String[] getStem() {
		return stem;
	}
	
	public String[] getBase() {
		return base;
	}
	
	public int getP() {
		return stem.length;
	}
	
	public int getQ() {
		return base.length;
	}

	/**
	 * Compare two pq-gram label tuples. A label tuple is the serialized
	 * sequence of node labels or hash values of the nodes of a pq-gram
	 * in preorder. 
	 */
	public int compareTo(Object o) {
		PQGram pqg = (PQGram)o;
		String[] base2 = pqg.getBase();
		String[] stem2 = pqg.getStem();
		if ((base.length != base2.length) || (stem.length != stem2.length)) {
			throw new RuntimeException("Can not compare pq-grams with different length.");
		}
		int cmp = 0;
		for (int i = 0; i < stem.length; i++) {
			cmp = compare(stem[i], stem2[i]);
			if  (cmp != 0) return cmp;
		}
		for (int i = 0; i < base.length; i++) {
			cmp = compare(base[i], base2[i]);
			if  (cmp != 0) return cmp;
		}
		return cmp;		
	}

	@Override
	public boolean equals(Object arg0) {
		return this.compareTo(arg0) == 0;
	}
	
	
	
}