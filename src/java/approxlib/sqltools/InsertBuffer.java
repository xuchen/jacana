/*
 * Created on Jan 18, 2005
 */
package approxlib.sqltools;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * In order to use an insert buffer, you must open it. To open it,
 * have to give a table wrapper.
 * 
 * The insert buffer does an INSERT IGNORE by default, i.e. duplicates 
 * in presence of unique keys give no error, but are ignored.  
 * 
 * @author augsten
 */
public class InsertBuffer {

	protected TableWrapper tw;    // table this buffer belongs to
	protected Statement out;      // for executing queries
	protected StringBuffer qry;   // buffered query

	protected boolean empty; // the InsertBuffer is empty, i.e. needs not to be flushed (and also: qry is not a valid SQL)
	protected boolean open; // the InsertBuffer is open, i.e., ready to use
	private boolean insertIgnore = true; // do an INSERT IGNORE 
	
	/**
	 *  Creates an InsertBuffer, that has still to be open.
	 */
	public InsertBuffer() {
		open = false;
	}
	
	/**
	 * Initializes the InsertBuffer for a table.
	 * 
	 * @param tw table
	 * @param size size of the insert buffer
	 * @throws SQLException
	 */
	public void open(TableWrapper tw, int size) throws SQLException {
		this.tw = tw;
		
		// init query buffer
		qry = new StringBuffer(size);
		qry.append(getQueryHead());
		empty = true;

		// init connection
		this.out = tw.getStatement();

		open = true;
		// System.out.println("Opended insert buffer of size " + (size/1000) + "K,");
	}

	/**
	 * Initializes the InsertBuffer for a table with default size.
	 * 
	 * @param tw table
	 * @throws SQLException
	 */
	public void open(TableWrapper tw) throws SQLException {
		open(tw, getDefaultSize(tw.getCon()));
	}
	
	
	/**
	 * Override this method for database specific drivers.
	 * 
	 * @return default size for buffer (=1000)
	 */
	public int getDefaultSize(Connection con) throws SQLException {
		return 1000;
	}
	
	/**
	 * @return query head 'INSERT IGNORE INTO tableName (attrib-list) VALUES'
	 */
	public String getQueryHead() {
		return "INSERT " + (insertIgnore ? "IGNORE" : "") + " INTO `" + tw.getTblName() + "` (" + 
				tw.getAtbList() + ") VALUES";
	}
	
	/**
	 * Override this method for database specific drivers. In this default version it 
	 * will flush the insert command each time it is called.
	 * 
	 * @param line a comma separated list of values enclosed by brackets
	 * (e.g. "(val1,val2,val3)")
	 * @throws SQLException
	 */
	public void insert(String line) throws SQLException {
		empty = false;
		qry.append(line);		
		flush();
	}
	
	/**
	 * Writes all data in the buffer to the table.
	 * 
	 * @throws SQLException
	 */
	public void flush() throws SQLException {
		if ((open) && (!empty)) {
			// System.out.println("length: " + qry.length());
			// System.out.println("capacity: " + qry.capacity());
			//System.out.println(qry.toString());
			out.executeUpdate(qry.toString());
			qry = new StringBuffer(qry.capacity());
			qry.append(getQueryHead());
			empty = true;
		}
	}
	
	/**
	 * Closes this buffer. In order to reuse it, you have to open it again.
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		if (open) {
			flush();
			out.close();
			qry = null;
			open = false;
		}
	}

	public boolean isInsertIgnore() {
		return insertIgnore;
	}

	public void setInsertIgnore(boolean insertIgnore) {
		this.insertIgnore = insertIgnore;
	}

	/**
	 * @return Returns the empty.
	 */
	public boolean isEmpty() {
		return empty;
	}
}
