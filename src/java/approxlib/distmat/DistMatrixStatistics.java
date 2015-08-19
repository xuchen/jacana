/*
 * Created on Apr 19, 2007
 */
package approxlib.distmat;

public class DistMatrixStatistics {
	
	public static double averageDist(DistMatrix dm) {
		double sum = 0;
		for (int row = 0; row < dm.getRowNum(); row++) {
			for (int col = 0; col < dm.getColNum(); col++) {
				sum += dm.distAt(row, col);
			}
		}
		return sum / (dm.getRowNum() * dm.getColNum());
	}

	public static int[] histo(DistMatrix dm, int buckets) {
		int[] histo = new int[buckets];
		double step = 1.0 / buckets;
		for (int r = 0; r < dm.getRowNum(); r ++) {
			for (int c = 0; c < dm.getColNum(); c++) {
				double d = dm.distAt(r,c);
				int pos = Math.min((int)(d / step), buckets - 1);
				//System.out.println(d + " goes to bucket " + pos + " [" + (pos * step) + ".." + ((pos + 1) * step ) + ")");
				histo[pos]++;
			}
		}
		return histo;
	}			
	
}
