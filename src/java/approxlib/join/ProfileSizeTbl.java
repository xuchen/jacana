/*
 * Created on 10-Apr-06
 */
package approxlib.join;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.ListIterator;

import approxlib.sqltools.InsertBuffer;
import approxlib.sqltools.SQLTools;
import approxlib.sqltools.TableWrapper;

/**
 * @author augsten
 */
public class ProfileSizeTbl extends TableWrapper {

	/**
	 * default name of the tree index attribute  
	 */
	public final String ATB_TREE_ID = "treeID";
	
	/**
	 * default name of the hashed pq-gram   
	 */
	public final String ATB_PROF_SIZE = "profSize";
	
	private String atbTreeID = ATB_TREE_ID;
	private String atbProfSize = ATB_PROF_SIZE;

	
	/**
	 * @param con
	 * @param tblName
	 */
	public ProfileSizeTbl(Connection con, String tblName) {
		super(con, tblName);
	}

	public ProfileSizeTbl(Connection con, Connection streamCon, InsertBuffer insBuff, String tblName) {
		super(con, streamCon, insBuff, tblName);
	}

	public void load(AggrProf idx) throws SQLException {
		reset();
		String qry =
			"INSERT INTO " + this.getTblName()  
			+ " SELECT " + idx.getAtbTreeID() + ", SUM(" + idx.getAtbCnt() + ")" 
			+ " FROM " + idx.getTblName() 
			+ " GROUP BY " + idx.getAtbTreeID();
		SQLTools.execute(this.getStatement(), qry, 
				"Creating profile size table '" + this.getTblName() 
				+ "' from pq-gram index '" + idx.getTblName() + "'.");
	}
	
	/**
	 * Returns a comma-separated list of the attribute names in the following order: 
	 * treeID, treeSize
	 * 
	 * Overridden method.
	 * 
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {
		return this.atbTreeID + "," + this.atbProfSize;
	}

	/* (non-Javadoc)
	 * @see sqltools.TableWrapper#create()
	 */
	@Override
	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `" + getTblName() + "` (");
		qryCreate.append(atbTreeID + " INT NOT NULL,");
		qryCreate.append(atbProfSize + " INT NOT NULL");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());
	}
	
	/**
	 * Unique index on treeIDs.
	 * @throws SQLException
	 */
	public void buildIndex() {
		try {
		String qry = 
			"ALTER TABLE " + this.getTblName() + " ADD UNIQUE INDEX(" + this.atbTreeID + ")";
		SQLTools.execute(this.getStatement(), qry, 
				"Indexing tree IDs of table '" + this.getTblName() + "'");
		} catch (SQLException e) {			
		}
	}
	
	public void insertTree(int treeID, int treeSize) throws SQLException {
		this.getInsBuff().insert("(" + treeID + "," + treeSize + ")");
	}
	
	/**
	 * @return Returns the atbProfSize.
	 */
	public String getAtbProfSize() {
		return atbProfSize;
	}

	/**
	 * @return Returns the atbTreeID.
	 */
	public String getAtbTreeID() {
		return atbTreeID;
	}

	/**
	 * 
	 * @param treeID
	 * @return profile size of tree treeID
	 * @throws SQLException
	 */
			
	public long getProfSize(int treeID) throws SQLException {
		String qry =
			"SELECT " + this.atbProfSize + " FROM " + this.getTblName() 
			+ " WHERE " + this.atbTreeID + "=" + treeID;
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getLong(1);
	}

	public int[] getTreeIDs() throws SQLException {
		String qry =
			"SELECT " + this.atbTreeID + " FROM " 
			+ this.getTblName() + " ORDER BY " + this.atbTreeID;
		ResultSet rs = this.getStatement().executeQuery(qry);
		LinkedList ll = new LinkedList();
		while (rs.next()) {
			ll.add(new Integer(rs.getInt(this.atbTreeID)));
		}
		int[] tids = new int[ll.size()];
		int i = 0;
		for (ListIterator it = ll.listIterator(); it.hasNext();) {
			tids[i] = ((Integer)it.next()).intValue();
			i++;
		}
		return tids; 		
	}
	
	
}
