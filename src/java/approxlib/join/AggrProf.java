/*
 * Created on 8-Apr-06
 */
package approxlib.join;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import approxlib.sqltools.SQLTools;
import approxlib.sqltools.TableWrapper;

/**
 * @author augsten
 */
public class AggrProf extends TableWrapper {
	
	/**
	 * default name of the tree index attribute  
	 */
	public final String ATB_TREE_ID = "treeID";
	
	/**
	 * default name of the hashed pq-gram   
	 */
	public final String ATB_PQ_GRAM = "pqGram";
	/**
	 * default name of the counter   
	 */
	public final String ATB_CNT = "cnt";
	
	
	
	private String atbTreeID = ATB_TREE_ID;  
	private String atbPQGram = ATB_PQ_GRAM; 
	private String atbCnt = ATB_CNT;
		
	private PQGramTbl pqgTbl;		
	
	/**
	 * @return Returns the pqgTbl.
	 */
	public PQGramTbl getPqgTbl() {
		return pqgTbl;
	}
	/**
	 * @param pqgTbl The pqgTbl to set.
	 */
	public void setPqgTbl(PQGramTbl pqgTbl) {
		this.pqgTbl = pqgTbl;
	}
	public AggrProf(Connection con, String tblName, PQGramTbl pqgTbl) {
		super(con, tblName);
		this.pqgTbl = pqgTbl;
	}

	public void loadPQGrams() throws SQLException {
		this.reset();
		String qry =
			"INSERT INTO " + this.getTblName() + 
			" SELECT *, COUNT(*) AS " + this.atbCnt +
			" FROM " + pqgTbl.getTblName() + 
			" GROUP BY " + pqgTbl.getAtbTreeID() + "," + pqgTbl.getAtbPQGram();
		SQLTools.execute(this.getStatement(), qry, 
				"Aggregating pq-grams from '" + pqgTbl.getTblName() + 
				"' into '" + this.getTblName() + "'.");
		this.addIdx_PQGrams();
	}
	
	public void loadPQGrams(int firstTreeID, int lastTreeID) throws SQLException {
		String qry =
			"INSERT INTO " + this.getTblName() + 
			" SELECT *, COUNT(*) AS " + this.atbCnt +
			" FROM " + pqgTbl.getTblName() + 
			" WHERE " + pqgTbl.getAtbTreeID() + ">=" + firstTreeID +
			" AND " + pqgTbl.getAtbTreeID() + "<=" + lastTreeID +
			" GROUP BY " + pqgTbl.getAtbTreeID() + "," + pqgTbl.getAtbPQGram();
		SQLTools.execute(this.getStatement(), qry, 
				"Aggregating pq-grams of trees " + firstTreeID + " to " + lastTreeID + 
				"('" + pqgTbl.getTblName() + 	"' --> '" + this.getTblName() + "').");
	}
	
	public void addIdx_PQGrams() throws SQLException {
		String qry =
			"ALTER TABLE " + this.getTblName() + " ADD INDEX (" + this.atbPQGram + ") ";
		SQLTools.execute(this.getStatement(), qry, 
				"Adding index to aggregated pq-gram table '" + this.getTblName() + "'.");	
	}
		
	public void dropIdx_PQGrams() throws SQLException {
		String qry =
			"ALTER TABLE " + this.getTblName() + " DROP INDEX " + this.getAtbPQGram();
		SQLTools.execute(this.getStatement(), qry, 
				"Dropping index from aggregated pq-gram table '" + this.getTblName() + "'.");	
	}

	public void addIdx_treeID_PQGrams() throws SQLException {
		String qry =
			"ALTER TABLE " + this.getTblName() + 
			" ADD UNIQUE INDEX ns_join (" + this.atbTreeID + "," + this.atbPQGram + ") ";
		SQLTools.execute(this.getStatement(), qry, 
				"Adding index 'ns-join' to aggregated pq-gram table '" + this.getTblName() + "'.");	
	}
		
	public void dropIdx_treeID_PQGrams() throws SQLException {
		String qry =
			"ALTER TABLE " + this.getTblName() + " DROP INDEX ns_join";
		SQLTools.execute(this.getStatement(), qry, 
				"Dropping index 'ns_join' from aggregated pq-gram table '" + this.getTblName() + "'.");	
	}
	
	
	/**
	 * Returns a comma-separated list of the attribute names in the following order: 
	 * treeID, pqGram, cnt
	 * 
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {		
		return this.atbTreeID + "," + this.atbPQGram + "," + this.atbCnt;
	}

	/**
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#create()
	 */
	@Override
	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `" + getTblName() + "` (");
		qryCreate.append(atbTreeID + " INT NOT NULL,");
		qryCreate.append(atbPQGram + " CHAR(" + (this.pqgTbl.getHf().getLength() * (this.pqgTbl.getP() + this.pqgTbl.getQ())) + "),");
		qryCreate.append(atbCnt + " INT NOT NULL");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());
	}
	
	/**
	 * 
	 * @param treeID
	 * @return profile size of tree treeID
	 * @throws SQLException
	 */
			
	public long getIdxSize(int treeID) throws SQLException {
		String qry =
			"SELECT " + this.atbTreeID + ", COUNT(*) as cnt FROM " + this.getTblName()
			+ " WHERE " + this.atbTreeID + "=" + treeID
			+ " GROUP BY " + this.atbTreeID; 
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getLong("cnt");
	}
			
	/**
	 * @return Returns the atbCnt.
	 */
	public String getAtbCnt() {
		return atbCnt;
	}

	/**
	 * @return Returns the atbPQGram.
	 */
	public String getAtbPQGram() {
		return atbPQGram;
	}

	/**
	 * @return Returns the atbTreeID.
	 */
	public String getAtbTreeID() {
		return atbTreeID;
	}

}
