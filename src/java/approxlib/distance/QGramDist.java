/*
 * Created on Jul 10, 2008
 */
package approxlib.distance;

public class QGramDist implements StringDist {

	private static boolean verbose = true;
	private int q;
	
	
	public QGramDist(int q) {
		this.q = q;
	}
	
	public double normStringDist(String s1, String s2) {
		int ps1 = s1.length() == 0 ? 0 : s1.length() + this.q - 1;
		int ps2 = s2.length() == 0 ? 0 : s2.length() + this.q - 1;
		if (ps1 + ps2 > 0) {
			return this.stringDist(s1, s2) / (ps1 + ps2);
		} else {
			return 0;
		}
	}

	public double stringDist(String s1, String s2) {
		return qg(s1, s2, this.q);
	}
	
	public static int qg(String s1, String s2, int q) {
		if ((s1.length() == 0) && (s2.length() == 0)) {
			return 0;
		} 
		if (s1.length() == 0) {
			return s2.length() + q - 1;
		}
		if (s2.length() == 0) {
			return s1.length() + q - 1;
		}
		String[] xq = new String[s1.length() + q - 1];
		String[] yq = new String[s2.length() + q - 1];
		for (int i = 0; i < q - 1; i++) {
			s1 = '#' + s1 + '#';
			s2 = '#' + s2 + '#';
		}
		for (int i = 0; i < xq.length; i++) {
			xq[i] = s1.substring(i, i + q);
			if (verbose) {
				System.out.print(xq[i] + " ");
			}
		}
		if (verbose) {
			System.out.println();
		}
		for (int i = 0; i < yq.length; i++) {
			yq[i] = s2.substring(i, i + q);
			if (verbose) {
				System.out.print(yq[i] + " ");
			}
		}
		if (verbose) {
			System.out.println();
		}
		java.util.Arrays.sort(xq);
		java.util.Arrays.sort(yq);
		int insec = countIntersection(xq, yq);
		int dist = (xq.length + yq.length) - 2 * insec;
		verbose = false;
		return dist;
	}
	
	public static int countIntersection(String[] a, String[] b) {
		int i = 0;
		int j = 0;
		int count = 0;
		while ((i < a.length) && (j < b.length)) {
			int cmp = a[i].compareTo(b[j]);
			if (cmp == 0) {       // a[j] == b[j]
				count++;
				i++;
				j++;
			} else if (cmp < 0) { // a[i] < b[j] 
				i++;
			} else {              // a[i] > b[j]
				j++;
			}			
		}
		return count;
	}

	public int getQ() {
		return q;
	}

	public void setQ(int q) {
		this.q = q;
	}
}
