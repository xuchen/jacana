/**
 * 
 */
package approxlib.distmat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import approxlib.sqltools.InsertBuffer;
import approxlib.sqltools.SQLTools;
import approxlib.sqltools.TableWrapper;

/**
 * @author naugsten
 *
 */
public class MapTbl extends TableWrapper {
	
	String atbID1 = "idA";
	String atbID2 = "idB";
	String atbName1 = "nameA";
	String atbName2 = "nameB";
	String atbDist = "dist";

	/**
	 * @param con
	 * @param tblName
	 */
	public MapTbl(Connection con, String tblName) {
		super(con, tblName);
	}
		
	public MapTbl(Connection con, Connection streamCon, InsertBuffer insBuff, String tblName) {
		super(con, streamCon, insBuff, tblName);
	}



	/* (non-Javadoc)
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {
		return 
			this.getAtbID1() + "," + this.getAtbID2() + "," + 
			this.getAtbName1() + "," + this.getAtbName2() + "," +
			this.getAtbDist();
	}

	/**
	 * Creates the table with a unique index on the pairs.
	 * 
	 * @see sqltools.TableWrapper#create()
	 */
	@Override
	public void create() throws SQLException {
		String qry =
			"CREATE TABLE `" + this.getTblName() + "` ("
			+ this.getAtbID1() + " INT NOT NULL,"
			+ this.getAtbID2() + " INT NOT NULL,"
			+ this.getAtbName1() + " VARCHAR(255),"
			+ this.getAtbName2() + " VARCHAR(255),"
			+ this.getAtbDist() + " DOUBLE)";
		SQLTools.execute(this.getStatement(), qry, "Creating table '" + this.getTblName() + "'");
		this.createIndex();
	}

	
	
	/**
	 * @return Returns the atbID1.
	 */
	public String getAtbID1() {
		return atbID1;
	}

	/**
	 * @param atbID1 The atbID1 to set.
	 */
	public void setAtbID1(String atbID1) {
		this.atbID1 = atbID1;
	}

	/**
	 * @return Returns the atbID2.
	 */
	public String getAtbID2() {
		return atbID2;
	}

	/**
	 * @param atbID2 The atbID2 to set.
	 */
	public void setAtbID2(String atbID2) {
		this.atbID2 = atbID2;
	}

	/**
	 * @return Returns the atbName1.
	 */
	public String getAtbName1() {
		return atbName1;
	}

	/**
	 * @param atbName1 The atbName1 to set.
	 */
	public void setAtbName1(String atbName1) {
		this.atbName1 = atbName1;
	}

	/**
	 * @return Returns the atbName2.
	 */
	public String getAtbName2() {
		return atbName2;
	}

	/**
	 * @param atbName2 The atbName2 to set.
	 */
	public void setAtbName2(String atbName2) {
		this.atbName2 = atbName2;
	}

	public void insertPair(int id1, int id2) throws SQLException {
		this.getInsBuff().insert("(" + id1 + "," + id2 + ",null,null,null)");
	}

	public void insertPair(int id1, int id2, String name1, String name2, double dist) throws SQLException {
		String s = "(" + id1 + "," + id2 
			+ ",'" + SQLTools.escapeSingleQuote(name1) + "'"
			+ ",'" + SQLTools.escapeSingleQuote(name2) + "'" 
			+ "," + dist +")";
		this.getInsBuff().setInsertIgnore(false);
		this.getInsBuff().insert(s);
	}
	
	public void createIndex() {
		SQLTools.createIndex("ID1", this, 
				new String[] {this.getAtbID1()});
		SQLTools.createIndex("ID2", this, 
				new String[] {this.getAtbID2()});
	}
	
	public void dropIndex() {
		SQLTools.dropIndex("ID1", this);
		SQLTools.dropIndex("ID2", this);
	}
	
	public int intersect(MapTbl m) throws SQLException {
		String qry =
			"SELECT COUNT(*) AS cardinality FROM `" + this.getTblName() + "` AS A INNER JOIN "
			+ "`" + m.getTblName() + "` AS B ON "
			+ "A." + this.getAtbID1() + "=B." + m.getAtbID1() + " AND "
			+ "A." + this.getAtbID2() + "=B." + m.getAtbID2();
		ResultSet rs = 
			SQLTools.executeQuery(this.getStatement(), qry, 
				"Computing cardinality of '" + this.getTblName() + " INTERSECT " + 
				m.getTblName() + "'");
		rs.next();
		return rs.getInt("cardinality");
	}
	
	public double recall(MapTbl correctMapping) throws SQLException {
		return ((double)this.intersect(correctMapping) / (double)correctMapping.getSize());
	}

	public double precision(MapTbl correctMapping) throws SQLException {
		return ((double)this.intersect(correctMapping) / (double)this.getSize());
	}
	
	/**
	 * @return Returns the atbDist.
	 */
	public String getAtbDist() {
		return atbDist;
	}

	/**
	 * @param atbDist The atbDist to set.
	 */
	public void setAtbDist(String atbDist) {
		this.atbDist = atbDist;
	}

}
