/*
 * Created on Apr 20, 2007
 */
package approxlib.distmat;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

import approxlib.util.FormatUtilities;

/**
 * Write the distance matrix to some output device (database, file, screen)
 * and read it from from an input device (database, file).
 * 
 * @author naugsten
 */

public class DistMatrixIO {
	
	public static void toDB(DistMatrix dm, DistMatrixTbl dmTbl) throws SQLException {
		dmTbl.reset();
		for (int row = 0; row < dm.getRowNum(); row++) {
			for (int col = 0; col < dm.getColNum(); col++) {
				dmTbl.getInsBuff().insert("(" + row + "," + col + "," + 
						dm.getIdRow(row) + "," + dm.getIdCol(col) + "," + 
						dm.distAt(row, col) + ")");
			}				
		}
		dmTbl.getInsBuff().close();
	}
	
	public static int[] getRowToId(DistMatrixTbl dmTbl) throws SQLException {
		return getRowColToId(dmTbl, dmTbl.getAtbRow(), dmTbl.getAtbRowId());
	}
	
	public static int[] getColToId(DistMatrixTbl dmTbl) throws SQLException {
		return getRowColToId(dmTbl, dmTbl.getAtbCol(), dmTbl.getAtbColId());
	}
	
	private static int[] getRowColToId(DistMatrixTbl dmTbl, 
			String atbRowOrCol, String atbId) throws SQLException {
		long card = dmTbl.getAtbCard(atbRowOrCol);
		int[] map = new int[(int)card];
		
		String qry = "SELECT DISTINCT " 
			+ atbRowOrCol + "," + atbId
			+ " FROM `" + dmTbl.getTblName() + "`";
		ResultSet rs = dmTbl.getStatement().executeQuery(qry);
		while (rs.next()) {
			map[rs.getInt(atbRowOrCol)] = rs.getInt(atbId);
		}
		return map;		
	}

	public static DistMatrix fromDB(DistMatrixTbl dmTbl, int streamSize) throws SQLException {
		DistMatrix dm = new DistMatrix(
				DistMatrixIO.getRowToId(dmTbl),
				DistMatrixIO.getColToId(dmTbl));

		String qry =
			"SELECT * FROM `" + dmTbl.getTblName() + "`";
		ResultSet rs = dmTbl.getStreamStatement(streamSize).executeQuery(qry); 
		while (rs.next()) {
			dm.setDistAt(rs.getInt(dmTbl.getAtbRow()), 
					rs.getInt(dmTbl.getAtbCol()), 
					rs.getDouble(dmTbl.getAtbDist()));
		}
		return dm;
	}
	
	public static DistMatrix fromDB(DistMatrixTbl dmTbl) throws SQLException {
		DistMatrix dm = new DistMatrix(
					DistMatrixIO.getRowToId(dmTbl),
					DistMatrixIO.getColToId(dmTbl));
		
		String qry =
			"SELECT * FROM `" + dmTbl.getTblName() + "`";
		ResultSet rs = dmTbl.getStatement().executeQuery(qry); 
		while (rs.next()) {
			dm.setDistAt(rs.getInt(dmTbl.getAtbRow()), 
					rs.getInt(dmTbl.getAtbCol()), 
					rs.getDouble(dmTbl.getAtbDist()));
		}
		return dm;
	}
	
	public static void toFile(DistMatrix dm, String filename, char separator) throws IOException {
		BufferedWriter out = 
			new BufferedWriter(new FileWriter(filename));
		
		for (int row = 0; row < dm.getRowNum(); row++) {
			for (int col = 0; col < dm.getColNum(); col++) {
				String outline = dm.getIdRow(row) + "" + separator + "" + dm.getIdCol(col) + "" + separator + "" + dm.distAt(row, col) + "\n";
				out.write(outline, 0, outline.length());
			}
		}
		out.close();
	}
	
	public static DistMatrix fromFile(String filename, char separator)  throws IOException {
		LineNumberReader in = 
			new LineNumberReader(new FileReader(filename));
		
		String inline;
		TreeMap<Integer, Integer> id1ToRow = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> id2ToCol = new TreeMap<Integer, Integer>();
		while ((inline = in.readLine()) != null) {
			String[] fields = FormatUtilities.getFields(inline, separator);

			Integer id1 = new Integer(Integer.parseInt(fields[0]));
			Integer id2 = new Integer(Integer.parseInt(fields[1]));
			
			if (!id1ToRow.containsKey(id1)) {
				id1ToRow.put(id1, id1ToRow.size());
			}
			if (!id2ToCol.containsKey(id2)) {
				id2ToCol.put(id2, id2ToCol.size());
			}
		}
		in.close();
		DistMatrix dm = new DistMatrix(id1ToRow, id2ToCol);
		// restart from the beginning		
		in = new LineNumberReader(new FileReader(filename));		
		while ((inline = in.readLine()) != null) {
			String[] fields = FormatUtilities.getFields(inline, separator);
			int id1 = Integer.parseInt(fields[0]);
			int id2 = Integer.parseInt(fields[1]);
			double d = Double.parseDouble(fields[2]);
			dm.setDistAtId(id1, id2, d);			
		}
		in.close();
		return dm;
	}

}
