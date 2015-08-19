/*
 * Created on Jan 18, 2005
 */
package approxlib.sqltools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Wrapper to a table in a database.
 * 
 * @author augsten
 */
abstract public class TableWrapper {
	
	/**
	 * Size of the <code>stopByte</code> in bytes.
	 */
	public static final int stopSize = 1;
	/**
	 * Value that can be added to <code>CHAR</code> attributes in SQL to prevent it from removing trailing spaces.
	 */
	public static final byte stopByte = (byte)'|';

	private String tblName; // table name
	
	// the database con
	private Connection streamCon; // reading input tables (the stream needs its own con...)
	private Connection con; // con for all other purposes, but reading the input stream
	private InsertBuffer insBuff; // buffer for fast insert
	private Statement streamStatement; // stream statement
	
	// MYSQL MySQL JDBC driver specific constant Integer.MIN_VALUE means "use a stream statement" 
	private int defaultFetchSize = Integer.MIN_VALUE; 

	/**
	 * Use this constructor to give a different connection for 
	 * streaming resultsets and to use you own instance of an insert buffer.
	 * 
	 * @param con general purpose connection
	 * @param streamCon connection used exclusively for streaming result sets
	 * @param insBuff buffer for fast inserts
	 * @param tblName
	 */
	public TableWrapper(Connection con, Connection streamCon, InsertBuffer insBuff, String tblName) {
		this.con = con;
		this.streamCon = streamCon;
		this.insBuff = insBuff;
		this.tblName = tblName;
	}
	
	/**
	 * Using this constructor the stream statement will use the same connection
	 * as all other statements. The insert buffer will be of default size.
	 * 
	 * @param con
	 * @param tblName
	 */
	public TableWrapper(Connection con, String tblName) {
		this(con, con, new InsertBuffer(), tblName);
	}
	
	/**
	 * Returns a new stream statement that uses the stream connection. 
	 * If there is already an active stream statement with the connection,
	 * that stream is closed before the new one is activated.
	 * 
	 * This can be overridden in order to support driver
	 * specific features. In this version it gives a resultset that can be
	 * read only forward and can not be updated. The con used her is 
	 * not used elsewhere.
	 * 
	 * Note: Some drivers ignore the fetch size. MySQL returns a stream statement
	 * only, if fetchSize is Integer.MIN_VALUE!
	 * 
	 * @param size the fetch size
	 * @return a statement to read from (only forward and not updatable)
	 * @throws SQLException
	 */
	public Statement getStreamStatement(int size) throws SQLException {
		this.closeStreamStatement();
		Statement stream = streamCon.createStatement(
				java.sql.ResultSet.TYPE_FORWARD_ONLY,
				java.sql.ResultSet.CONCUR_READ_ONLY);
		stream.setFetchSize(size);
		this.streamStatement = stream;
		return stream;
	}
	
	/**
	 * Like getStreamStatement(int), but tells the driver to use a default 
	 * value for the fetch size.
	 *
	 * The default fetch size is stored in TableWrapper.
	 * 
	 * @return streaming statement (only forward and not updatable) with
	 *         default fetch size
	 * @throws SQLException
	 */
	public Statement getStreamStatement() throws SQLException {
		return getStreamStatement(this.getDefaultFetchSize());
	}
	
	/**
	 * Close the current stream if one is active.
	 * 
	 * @return true if there was a stream to be closed.
	 * @throws SQLException
	 */
	public boolean closeStreamStatement() throws SQLException {
		if (this.streamStatement != null) {
			this.streamStatement.close();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @return statement for the standard connection
	 * @throws SQLException
	 */
	public Statement getStatement() throws SQLException {
		return this.getCon().createStatement();
	}
	
	/**
	 * 
	 * Is used, for example, by InsertBuffer to determine the query head...
	 * 
	 * @return comma separated list of all known attributes of the table.
	 */
	abstract public String getAtbList();
		
	/**
	 * Drop the table.
	 * 
	 * @throws SQLException
	 */
	public void drop() throws SQLException {
		this.getStatement().execute("DROP TABLE `" + tblName + "`");
	}
	
	/**
	 * Create the table. You can assume there is no table of this name there.
	 */
	abstract public void create() throws SQLException;
	
	public void reset() throws SQLException {
		try {
			drop();
		} catch (SQLException e) {
		}
		create();
	}
	
	/**
	 * Will rename the table wrapped by this object in the underlaying database. Assumes that the table exists.  
	 * 
	 * @param tblName new table name
	 * @throws SQLException
	 */
	public void rename(String tblName) throws SQLException {
		String qry = "ALTER TABLE `" + this.getTblName() + "` RENAME `" + tblName + "`"; 
		this.getStatement().execute(qry);
		this.setTblName(tblName);
	}
	
	/** 
	 * Will create the table, if it is not already there.
	 *
	 */
	public void open() {
		try {
			create();
		} catch (SQLException e) {
		}		
	}
	
	/**
	 * Flush all buffers.
	 * 
	 * @throws SQLException
	 */
	public void flush() throws SQLException {
		this.getInsBuff().flush();
	}
	
	/**
	 * Release buffer memory.
	 */
	public void close() throws Exception {
		this.insBuff.close();
	}
	
	/**
	 * @return Returns the con.
	 */
	public Connection getCon() {
		return con;
	}
	/**
	 * @param con The con to set.
	 */
	public void setCon(Connection con) {
		this.con = con;
	}
	/**
	 * @return Returns the streamCon.
	 */
	public Connection getStreamCon() {
		return streamCon;
	}
	/**
	 * @param streamCon The streamCon to set.
	 */
	public void setStreamCon(Connection streamCon) {
		this.streamCon = streamCon;
	}
	/**
	 * @return Returns the tblName.
	 */
	public String getTblName() {
		return tblName;
	}
	/**
	 * @param tblName The tblName to set.
	 */
	public void setTblName(String tblName) {
		this.tblName = tblName;
	}
	/**
	 * @return open InsertBuffer (insBuff) with default size.
	 */
	public InsertBuffer getInsBuff() throws SQLException {
		if (!insBuff.open) {
			insBuff.open(this);
		}
		return insBuff;
	}
	
	/**
	 * Compares two tables with with LEFT OUTER JOINS. NULL value is checked on an 
	 * attribute that can not be NULL. 
	 * @param tw
	 * @param nonNullAtbName attribute in both tables that can not be NULL (e.g. a key).
	 * @return
	 * @throws SQLException
	 */
	public boolean equals(TableWrapper tw, String nonNullAtbName) throws SQLException {
		String A = "`" + this.getTblName() + "`";
		String B = "`" + tw.getTblName() + "`";
		String qry =
			"SELECT * "
			+ "FROM " + A + " AS A NATURAL LEFT JOIN " + B + " AS B "
			+ "WHERE B." + nonNullAtbName + " IS NULL";
		ResultSet rs1 = SQLTools.executeQuery(this.getStatement(), qry,
				"Comparing " + A + " and " + B + " for equality.");
		qry =
			"SELECT * "
			+ "FROM " + B + " AS B NATURAL LEFT JOIN " + A + " AS A "
			+ "WHERE A." + nonNullAtbName + " IS NULL";
		ResultSet rs2 = SQLTools.executeQuery(this.getStatement(), qry,
				"Comparing " + B + " and " + A + " for equality.");
		
		return !(rs1.next() || rs2.next()) && this.getAtbList().equals(tw.getAtbList());

	}
	
	/**
	 * Issues an SQL query that counts the number of tuples.
	 * 
	 * @return number of tuples in table
	 * @throws SQLException
	 */
	public long getSize() throws SQLException {
		String qry =
			"SELECT COUNT(*) as cnt FROM `" + this.getTblName() + "`";
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getLong("cnt");
	}
	
	/**
	 * How many distinct values does the table contain for the given attribute?
	 * 
	 * @param atbName
	 * @return cardinality of the set of values of the attribute atbName 
	 * @throws SQLException
	 */
	public long getAtbCard(String atbName) throws SQLException {
		String rowNumQry = 
			"SELECT COUNT(*) AS card FROM "
			+ "(SELECT DISTINCT " + atbName + " FROM `" + this.getTblName() + "`) AS A";
		ResultSet rs = this.getStatement().executeQuery(rowNumQry);
		rs.next();
		return rs.getLong("card");  
	}
	
	public boolean exists() {
		try {
			String qry = "SELECT * FROM `" + this.getTblName() + "`";
			this.getStatement().execute(qry);
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * @return default fetch size for streaming ResultSets for this table
	 */
	public int getDefaultFetchSize() {
		return defaultFetchSize;
	}

	/**
	 * @param defaultFetchSize default fetch size for streaming ResultSets for this table
	 */
	public void setDefaultFetchSize(int defaultFetchSize) {
		this.defaultFetchSize = defaultFetchSize;
	}

}
