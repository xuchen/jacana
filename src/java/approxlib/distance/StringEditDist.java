/*
 * Created on Jul 10, 2008
 */
package approxlib.distance;

public class StringEditDist implements StringDist {

	public double normStringDist(String s1, String s2) {
		if (Math.max(s1.length(), s2.length()) > 0) {
			return stringDist(s1, s2) / Math.max(s1.length(), s2.length());
		} else {
			return 0;
		}
	}
	
	public double stringDist(String t1, String t2) {		
		return edLinearSpace(t1, t2);
	}
	
	public static int edLinearSpace(String x, String y) {
		if (x.length() > y.length()) {
			String tmp = x;
			x = y;
			y = tmp;
		}
		int[] cp = new int[x.length() + 1];
		int[] cc = new int[x.length() + 1];
		for (int i = 0; i <= x.length(); i++) {
			cp[i] = i;
		}
		for (int j = 1; j <= y.length(); j++) {
			cc[0] = j;
			for (int i = 1; i <= x.length(); i++) {
				char cx = x.charAt(i - 1);
				char cy = y.charAt(j - 1);
				cc[i] = min(
						cp[i - 1] + ((cx == cy) ? 0 : 1),
						cc[i - 1] + 1,
						cp[i] + 1
						);
					
			}
			int[] tmp = cp;
			cp = cc;
			cc = tmp;
		}
		return cp[x.length()]; // note: cc is asigned to cp, cc is overwritten!
	}

	private static int min(int a, int b, int c) {
		return Math.min(a, Math.min(b, c));
	}

}
