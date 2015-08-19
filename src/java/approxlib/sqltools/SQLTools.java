/*
 * Created on Sep 26, 2005
 */
package approxlib.sqltools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author augsten
 */
public class SQLTools {
	
	public static boolean DEBUG = false;
	
	/**
	 * Execute a query and output elapsed time and query string, if {@link #DEBUG} is true.
	 * @param s
	 * @param query
	 * @param comment
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet executeQuery(Statement s, String query, String comment) throws SQLException {
		if (DEBUG) {
			System.err.println("\n" + comment);
			System.err.println(query);
		}
		long start = System.currentTimeMillis();
		ResultSet rs = s.executeQuery(query);
		if (DEBUG) {
			System.err.println("...took me " + (System.currentTimeMillis() - start) + "ms");
			System.err.println("---");
		}
		return rs;
	}

	/**
	 * Execute a query and output elapsed time and query string, if {@link #DEBUG} is true.
	 * 
	 * @param query
	 * @param comment
	 * @throws SQLException
	 */
	public static int executeUpdate(Statement s, String query, String comment) throws SQLException {
		if (DEBUG) {
			System.err.println("\n" + comment);
			System.err.println(query);
		}
		long start = System.currentTimeMillis();
		int res = s.executeUpdate(query);
		if (DEBUG) {
			System.err.println("...took me " + (System.currentTimeMillis() - start) + "ms");
			System.err.println("---");
		}
		return res;
	}	
	
	/**
	 * Execute a query and output elapsed time and query string, if {@link #DEBUG} is true.
	 * 
	 * @param query
	 * @param comment
	 * @throws SQLException
	 */
	public static void execute(Statement s, String query, String comment) throws SQLException {
		if (DEBUG) {
			System.err.println("\n" + comment);
			System.err.println(query);
		}
		long start = System.currentTimeMillis();
		s.execute(query);
		if (DEBUG) {
			System.err.println("...took me " + (System.currentTimeMillis() - start) + "ms");
			System.err.println("---");
		}
	}
	


	/**
	 * Escape single quotes in a string.
	 * 
	 * @param s
	 * @return <code>s</code> with single quotes escaped
	 */
	public static String escapeSingleQuote(String s) {
		StringBuffer res = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
			case '\'': 
				res.append("''");
				break;
			case '\\':
				res.append("\\\\");
				break;
			default:
				res.append(s.charAt(i));
			}
		}
		return res.toString();
	}
	
	public static void debugMsg(String msg, long startTime) {
		if (DEBUG) {
			System.out.println("\n" + msg + " (" 
					+ (System.currentTimeMillis() - startTime) + "ms)");
		}
	}
	
	public static ResultSetMetaData getMetaData(String tblName, Connection con) throws SQLException {
		String qry = "SELECT *  FROM `" + tblName + "` LIMIT 1";
		ResultSet rs =
			executeQuery(con.createStatement(), qry,
					"Retrieving metadata of table '" 
					+ tblName  + "'");
		return rs.getMetaData();
	}
	
	/**
	 * <p>
	 * Computes the cardinality of set-minus, |A\B| for two tables A and B.
	 * The first n columns of each table are projected, where n is the smaller number of columns between A and B.
	 * The projections must be UNION-compatible. 
	 *  
	 * <p>
	 * Does a LEFT JOIN on the tables and reads in the result. May be very slow if no keys are defined!
	 * 
	 * @param tblA table A
	 * @param tblB table B
	 * @param nonNullColB a column in B that is not null (e.g. a unique key)
	 * @param con
	 * @return |A\B|
	 * @throws SQLException
	 */
	public static int cardSetMinus(String tblA, String tblB, Connection con) 
		throws SQLException {
		ResultSetMetaData m1 = getMetaData(tblA, con);
		ResultSetMetaData m2 = getMetaData(tblB, con);

		// build condition for join
		int colNum = Math.min(m1.getColumnCount(), m2.getColumnCount());
		StringBuffer condition = new StringBuffer();
		for (int i = 1; i <= colNum; i++) {
			String c1 = m1.getColumnName(i);
			String c2 = m2.getColumnName(i);
			if ((m1.isNullable(i) == ResultSetMetaData.columnNoNulls) || (m2.isNullable(i) == ResultSetMetaData.columnNoNulls)) {
				condition.append("(A.`" + c1 + "` = B.`" + c2 + "`)");
			} else {
				condition.append("(((A.`" + c1 + "` is null) and (B.`"
						+ c2 + "` is null)) or (A.`" + c1 + "` = B.`" + c2 + "`))");
			}
			if (i < colNum) {
				condition.append(" and ");
			}
		}

		// build condition for where clause
		StringBuffer qry1 = new StringBuffer( 
				"SELECT DISTINCT * FROM `" + tblA + "` AS A LEFT JOIN `"
				+ tblB + "` AS B ON "+ condition + " WHERE "); 
		int nonNullColB = -1;
		for (int i = 1; i <= m2.getColumnCount(); i++) { // is there a column that can not be NULL?
			if (m2.isNullable(i) == ResultSetMetaData.columnNoNulls) {
				nonNullColB = i;
			}
		}
		if (nonNullColB == -1) {    // all columns can havs NULLs -> check ALL columns of B for NULL
			for (int i = 1; i <= m2.getColumnCount(); i++) {
				qry1.append("B." + m2.getColumnName(i) + " IS NULL and ");
			}
			qry1.append("not (");
			for (int i = 1; i <= colNum; i++) { // do not count tuple, if also all columns in A are NULL
				qry1.append("A." + m1.getColumnName(i) + " IS NULL");
				if (i != colNum) {
					qry1.append(" and ");
				}
			}			
			qry1.append(")");
		} else {
			qry1.append("B." + m2.getColumnName(nonNullColB) + " IS NULL");
		}
		
		int card = 0;
		// MYSQL: Adapt for MySQL 5 (use COUNT and subquery instaed of reading result set!)
		ResultSet rs1 =
			executeQuery(con.createStatement(), qry1.toString(), "Getting tuples in '" + tblA + "' that are not in '" + tblB + "'");
		while (rs1.next()) {
			card++;
		}
		return card;
	}
	
	/**
	 * Project out selected non-null attributes from A and B, and eliminate tuples. 
	 * Let the results be A' and B', respectively. Then |A'\B'| is computed.
	 * 
	 * @param tblA table A
	 * @param tblB table B
	 * @param keysA selected attributes of A
	 * @param keysB selected attributes of B
	 * @param con connection to the database with the tables 
	 * @return
	 */
	public static int cardSetMinus(String tblA, String tblB, String[] keysA, String[] keysB,
			Connection con) throws SQLException {
		if (keysA.length != keysB.length) {
			throw new RuntimeException("Number of attributes must be the same for both sets.");
		}

		
		StringBuffer predicate = new StringBuffer();
		StringBuffer atbList = new StringBuffer(); 
		for (int i = 0; i < keysA.length; i++) {
			predicate.append("a." + keysA[i] + "=b." + keysB[i]);
			atbList.append("a." + keysA[i]);
			if (i != keysA.length - 1) {
				predicate.append(" AND \n   ");
				atbList.append(",");
			}
		}
		String qry = 
			"SELECT COUNT(*) as count FROM \n(SELECT DISTINCT " 
			+ atbList + 
			" FROM `" + tblA + "` AS a LEFT JOIN `" + tblB + "` AS b\nON "
			+ predicate.toString() 
			+ "\nWHERE b." + keysB[0] + " IS NULL) AS x";
			
		ResultSet rs =
			SQLTools.executeQuery(con.createStatement(), qry,
					"Computing |A\\B| on selected attributes.");
		rs.next();
		return rs.getInt("count");
	}

	/**
	 * Project out selected non-null attributes from A and B, and eliminate tuples. 
	 * Let the results be A' and B', respectively. Then |A' intersect B'| is computed.
	 * 
	 * @param tblA table A
	 * @param tblB table B
	 * @param keysA selected attributes of A
	 * @param keysB selected attributes of B
	 * @param con connection to the database with the tables 
	 * @return
	 */
	public static int cardIntersect(String tblA, String tblB, String[] keysA, String[] keysB,
			Connection con) throws SQLException {
		if (keysA.length != keysB.length) {
			throw new RuntimeException("Number of attributes must be the same for both sets.");
		}
		
		StringBuffer predicate = new StringBuffer();
		StringBuffer atbList = new StringBuffer(); 
		for (int i = 0; i < keysA.length; i++) {
			predicate.append("a." + keysA[i] + "=b." + keysB[i]);
			atbList.append("a." + keysA[i]);
			if (i != keysA.length - 1) {
				predicate.append(" AND \n   ");
				atbList.append(",");
			}
		}
		String qry = 
			"SELECT COUNT(*) as count FROM \n(SELECT DISTINCT " 
			+ atbList + 
			" FROM `" + tblA + "` AS a INNER JOIN `" + tblB + "` AS b\nON "
			+ predicate + ") AS x";
		
		ResultSet rs =
			SQLTools.executeQuery(con.createStatement(), qry,
			"Computing |A intersect B| on selected attributes.");
		rs.next();
		return rs.getInt("count");
	}
	
	/**
	 * Project out selected non-null attributes from A and B, and eliminate tuples. 
	 * Let the results be A' and B', respectively. Then |A'\B'|, |A intersect B| and 
	 * |B'\A'| are computed and return in an array in this order.
	 * 
	 * @param tblA table A
	 * @param tblB table B
	 * @param keysA selected attributes of A
	 * @param keysB selected attributes of B
	 * @param con connection to the database with the tables 
	 * @return [|A'\B'|, |A' intersect B'|, |B'\A'|]
	 */
	public static int[] cardVennDiagram(String tblA, String tblB, String[] keysA, String[] keysB,
			Connection con) throws SQLException {
		int[] venn = new int[3];
		venn[0] = SQLTools.cardSetMinus(tblA, tblB, keysA, keysB, con);
		venn[1] = SQLTools.cardIntersect(tblA, tblB, keysA, keysB, con);
		venn[2] = SQLTools.cardSetMinus(tblB, tblA, keysB, keysA, con);
		return venn;
	}
	
	/**
	 * Creates a (non-unique) index.
	 * 
	 * @param idxName name of new index
	 * @param t table
	 * @param attribs attributes for index
	 */
	public static void createIndex(String idxName, TableWrapper t, 
			String[] attribs)  {
		StringBuffer qry = new StringBuffer(
				"CREATE INDEX `" + idxName + "` ON `" + t.getTblName() + "` (");
		for (int i = 0; i < attribs.length; i++) {
			qry.append("`" + attribs[i] + "`");
			if (i != attribs.length - 1) {
				qry.append(" AND ");
			}
		}
		qry.append(")");
		try {
			execute(t.getStatement(), qry.toString(), 
					"Creating index on table '" + t.getTblName() + "'");
		} catch (java.sql.SQLException e) {}
	}
	
	
	/**
	 * Creates a unique index.
	 * 
	 * @param idxName name of new index
	 * @param t table
	 * @param attribs attributes for index
	 */
	public static void createUniqueIndex(String idxName, TableWrapper t, 
			String[] attribs)  {
		StringBuffer qry = new StringBuffer(
				"CREATE UNIQUE INDEX `" + idxName + "` ON `" + t.getTblName() + "` (");
		for (int i = 0; i < attribs.length; i++) {
			qry.append("`" + attribs[i] + "`");
			if (i != attribs.length - 1) {
				qry.append(",");
			}
		}
		qry.append(")");
		try {
			execute(t.getStatement(), qry.toString(), 
					"Creating unique index on table '" + t.getTblName() + "'");
		} catch (java.sql.SQLException e) {}
	}
		

	/**
	 * Drops the index idxName from table t.
	 * 
	 * @param idxName
	 * @param t
	 */
	public static void dropIndex(String idxName, TableWrapper t) {
		try {
			String qry =
				"DROP INDEX `" + idxName + "` ON `" + t.getTblName() + "`";
			execute(t.getStatement(), qry, "Dropping index from table '"
					+ t.getTblName() + "'");
		} catch (SQLException e) {}
	}
	
	/**
	 * Clone a table with all the data or only the structure of the table.
	 * 
	 * @param tblExisting the table to be cloned
	 * @param tblClone the new table (i.e., the clone)
	 * @param copyData true: exact clone, false: empyt table with same structure
	 * @param con connection to execute queries
	 * @throws SQLException
	 */
	public static void cloneTable(String tblExisting, String tblClone, boolean copyData, Connection con) 
		throws SQLException {
		String qry = "CREATE TABLE `" + tblClone + "` AS " +
				" SELECT * FROM `" + tblExisting + "`";
		if (!copyData) {
			qry += " LIMIT 1";
		}
		con.createStatement().execute(qry);
		if (!copyData) {
			qry = "DELETE FROM `" + tblClone + "`";
			con.createStatement().execute(qry);
		}
	}
		
}
