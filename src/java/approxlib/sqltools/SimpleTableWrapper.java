/*
 * Created on Mar 14, 2005
 */
package approxlib.sqltools;

import java.sql.SQLException;
import java.sql.Connection;

/**
 * A simple table. You can use it, if you want to access a table, but you don't want
 * to make a subclass of tableWrapper for this purpose.
 * 
 * @author augsten
 */
final public class SimpleTableWrapper extends TableWrapper {
	
	String atbList;
	String sqlCreate;
	
	/**
	 * Constructor.
	 *  
	 * @param atbList return value for getAtbList
	 * @param sqlCreate sql statement to issue for a call of {@link #create()}
	 */
	public SimpleTableWrapper(Connection con, Connection streamCon, 
			InsertBuffer insBuff, String tblName,
			String atbList, String sqlCreate) {
		super(con, streamCon, insBuff, tblName);
		this.atbList = atbList;
		this.sqlCreate = sqlCreate;
	}

	/* (non-Javadoc)
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {
		return atbList;
	}

	/**
	 * Does nothing. You cannot create this table, only use it...
	 * 
	 * @throws SQLException
	 */
	@Override
	public void create() throws SQLException {
		this.getCon().createStatement().execute(sqlCreate);
	}

}
