/*
 * Created on Sep 25, 2006
 */
package approxlib.distmat;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.TreeMap;

import approxlib.join.Intersect;
import approxlib.join.ProfileSizeTbl;
import java.sql.SQLException;

import approxlib.util.FormatUtilities;

/**
 * This is a m x n matrix that stores distances (double values) between objects.
 * Each object has an ID that is independent of the row/column number. The distance between
 * two objects can be queried by object ID or by row/column number.
 * 
 * @see DistMatrixIO, DistMatrixStatistics
 * 
 * @author augsten
 */
public class DistMatrix {

	/**
	 * Stores (dist, row/col number)-pairs.
	 * 
	 * @author augsten
	 */
	private class PosDist implements Comparable {

		public double dist;

		public int pos;

		/**
		 * @param dist distance
		 * @param pos row or column position
		 */
		public PosDist(double dist, int pos) {
			this.dist = dist;
			this.pos = pos;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object arg0) {
			double dist2 = ((PosDist) arg0).dist;
			if (dist < dist2) {
				return -1;
			} else if (dist == dist2) {
				return 0;
			} else {
				return 1;
			}
		}

	}

	private double[][] dist;
	
	private int[] rowToId;
	private int[] colToId;
	
	private TreeMap id1ToRow;
	private TreeMap id2ToCol;
	

	/**
	 * Initialize the distance matrix by giving the translation 
	 * tables from row/column numbers to object ID.
	 * 
	 * @param rowToId rowToId[i] is the ID of row i
	 * @param colToId cosToId[i] is the ID of row i
	 */
	public DistMatrix(int[] rowToId, int[] colToId) {
		dist = new double[rowToId.length][colToId.length];
		this.rowToId = rowToId;
		this.colToId = colToId;
		id1ToRow = new TreeMap();
		for (int i = 0; i < rowToId.length; i++) {
			id1ToRow.put(new Integer(rowToId[i]), new Integer(i));
		}		
		id2ToCol = new TreeMap();
		for (int i = 0; i < colToId.length; i++) {
			id2ToCol.put(new Integer(colToId[i]), new Integer(i));
		}				
	}

	/**
	 * Initialize the distance matrix by giving the translation 
	 * "tables" from object IDs to row/column numbers. 

	 * @param id1ToRow the key is the object ID, the value is the respective row number
	 * @param id2ToCol the key is the object ID, the value is the respective column number
	 */
	public DistMatrix(TreeMap<Integer,Integer> id1ToRow, TreeMap<Integer,Integer> id2ToCol) {
		dist = new double[id1ToRow.size()][id2ToCol.size()];
		this.rowToId = new int[id1ToRow.size()];
		this.id1ToRow = id1ToRow;
		for (java.util.Iterator<Integer> it = id1ToRow.keySet().iterator(); it.hasNext(); ) {
			Integer id = it.next();
			rowToId[(id1ToRow.get(id)).intValue()] = id.intValue();
		}
		this.id2ToCol = id2ToCol;
		this.colToId = new int[id2ToCol.size()];
		for (java.util.Iterator<Integer> it = id2ToCol.keySet().iterator(); it.hasNext(); ) {
			Integer id = it.next();
			colToId[(id2ToCol.get(id)).intValue()] = id.intValue();
		}
	}
	
	// TODO remove this
	/**
	 * 
	 */
	public DistMatrix(Intersect is, ProfileSizeTbl ps1, ProfileSizeTbl ps2) throws SQLException {
		loadProfileSizeTbls(ps1, ps2);
		dist = new double[rowToId.length][colToId.length];
		for (int i = 0; i < dist.length; i++) {
			Arrays.fill(dist[i], 1.0);
		}
		dist = loadDists(is, dist);
	}	

	// TODO remove this
	private void loadProfileSizeTbls(ProfileSizeTbl ps1, ProfileSizeTbl ps2) throws SQLException {
		rowToId = ps1.getTreeIDs();
		id1ToRow = new TreeMap();
		for (int i = 0; i < rowToId.length; i++) {
			id1ToRow.put(new Integer(rowToId[i]), new Integer(i));
		}		
		colToId = ps2.getTreeIDs();
		id2ToCol = new TreeMap();
		for (int i = 0; i < colToId.length; i++) {
			id2ToCol.put(new Integer(colToId[i]), new Integer(i));
		}		
	}
	
	// TODO remove this
	private double[][] loadDists(Intersect is, double[][] dist) throws SQLException {
		String qry =
			"SELECT "  
			+ is.getAtbTreeID1() + "," 
			+ is.getAtbTreeID2() + "," 
			+ is.getAtbDist() 
			+ " FROM " + is.getTblName();
		Statement s = is.getStreamStatement();
		ResultSet rs =
			approxlib.sqltools.SQLTools.executeQuery(s, qry, "Loading distances into matrix...");
		while (rs.next()) {
			int treeID1 = rs.getInt(is.getAtbTreeID1());
			int treeID2 = rs.getInt(is.getAtbTreeID2());
			double d = rs.getDouble(is.getAtbDist());
			setDistAtId(treeID1, treeID2, d);
		}
		return dist;		
	}
	
	/**
	 * @return Returns the translation table from column numbers to object IDs.
	 */
	public int[] getColToId() {
		return colToId;
	}
	/**
	 * @return Returns the translation table from row numbers to object IDs.
	 */
	public int[] getRowToId() {
		return rowToId;
	}
	
	/**
	 * Get the distance between the objects at row and column.
	 * 
	 * @param row row number
	 * @param col column number
	 * @return
	 */
	public double distAt(int row, int col) {
		return dist[row][col];
	}

	/**
	 * Get the distance between the objects with IDs idRow and idCol.
	 * @param idRow ID of a row object
	 * @param idCol ID of a column object
	 * @return
	 */
	public double distAtId(int idRow, int idCol) {
		return dist[getRow(idRow)][getCol(idCol)];
	}

	/**
	 * Set the distance between the objects at row and column.
	 * 
	 * @param row row number
	 * @param col column number
	 * @param d distance between the two objects
	 */
	public void setDistAt(int row, int col, double d) {
		dist[row][col] = d;
	}
	
	/**
	 * Set the distance between the objects with IDs idRow and idCol.
	 *	
	 * @param idRow ID of a row object
	 * @param idCol ID of a column object
	 * @param d distance between the two objects
	 */
	public void setDistAtId(int idRow, int idCol, double d) {
		dist[getRow(idRow)][getCol(idCol)] = d;
	}
	
	/**
	 * @param idRow a row object
	 * @return row number of the object
	 */
	public int getRow(int idRow) {
		return ((Integer)id1ToRow.get(new Integer(idRow))).intValue();
	}

	/**
	 * @param idCol a column object
	 * @return column number of the object 
	 */
	public int getCol(int idCol) {
		return ((Integer)id2ToCol.get(new Integer(idCol))).intValue();		
	}
	
	/**
	 * @param row row number of an object
	 * @return ID of the object
	 */
	public int getIdRow(int row) {
		return rowToId[row];
	}
	
	/**
	 * @param col column number of an object
	 * @return ID of the object
	 */
	public int getIdCol(int col) {
		return colToId[col];
	}

	/**
	 * @return number of row objects
	 */
	public int getRowNum() {
		return rowToId.length;
	}

	/**
	 * @return number of column objects
	 */
	public int getColNum() {
		return colToId.length;
	}
	

	/**
	 * @return distance matrix without reference to object IDs
	 */
	public double[][] getDist() {
		return dist;
	}
	
	/**
	 * @param row row number of an object
	 * @return list of column numbers, sorted in ascending order by the distances of the respective column object to the row object
	 */
	public int[] getRowPrefs(int row) {
		PosDist[] prefList = new PosDist[getColNum()];
		for (int col = 0; col < prefList.length; col++) {
			prefList[col] = new PosDist(this.distAt(row, col), col);
		}
		Arrays.sort(prefList);
		int[] result = new int[prefList.length];
		for (int i = 0; i < prefList.length; i++) {
			result[i] = prefList[i].pos;
		}
		return result;
	}
	
	/**
	 * @param col column number of an object
	 * @return list of row numbers, sorted in ascending order by the distances of the respective row object to the column object
	 */
	public int[] getColPrefs(int col) {
		PosDist[] prefList = new PosDist[getRowNum()];
		for (int row = 0; row < prefList.length; row++) {
			prefList[row] = new PosDist(this.distAt(row, col), row);
		}
		Arrays.sort(prefList);
		int[] result = new int[prefList.length];
		for (int i = 0; i < prefList.length; i++) {
			result[i] = prefList[i].pos;
		}
		return result;
	}
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		int commas = 4;
		int fieldLen = commas + 3;
		StringBuffer sb = new StringBuffer();
		sb.append(FormatUtilities.resizeFront("", fieldLen));
		for (int col = 0; col < this.getColNum(); col++) {
			sb.append(FormatUtilities.resizeFront("[" + this.getIdCol(col) + "]", fieldLen));
		}
		sb.append("\n");
		for (int row = 0; row < this.getRowNum(); row++) {
			//sb.append(FormatUtilities.resizeEnd("[" + this.getId1(row) + "]", fieldLen));
			sb.append("$\\strA{" + this.getIdRow(row) + "}$ & ");
			for (int col = 0; col < this.getColNum(); col++) {
				double multi = Math.pow(10, commas);
				double x = Math.round(this.distAt(row, col) * multi) / multi;
				sb.append(" " + FormatUtilities.resizeFront(x + "", fieldLen - 1) + " & ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
}

