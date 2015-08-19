/*
 * Created on 6/02/2007
 */
package approxlib.sqltools;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Clones an existing table and creates a TableWrapper object for the clone. 
 * The create method can only be used as long as the cloned table exists.
 *   
 * @author naugsten
 */
public class CloneTbl extends TableWrapper {
	
	private String atbList;
	private String tblNameCloned;

	/**
	 * @param con
	 * @param tblName
	 */
	public CloneTbl(Connection con, String tblNameThis, String tblNameCloned) throws SQLException {
		super(con, tblNameThis);
		this.tblNameCloned = tblNameCloned;
		ResultSetMetaData meta = SQLTools.getMetaData(tblNameCloned, con);
		int colNum = meta.getColumnCount();
		StringBuffer sb = new StringBuffer();
		for (int col = 1; col <= colNum; col++) {
			sb.append("`" + meta.getColumnName(col) + "`");
			if (col != colNum) {
				sb.append(",");
			}
		}
		atbList = sb.toString();
	}

	/**
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {
		return atbList;
	}

	/**
	 * @see sqltools.TableWrapper#create()
	 */
	@Override
	public void create() throws SQLException {
		SQLTools.cloneTable(tblNameCloned, this.getTblName(), false, this.getCon());
	}

}
