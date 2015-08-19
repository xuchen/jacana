/*
 * Created on 8-Apr-06
 */
package approxlib.join;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import approxlib.sqltools.SQLTools;
import approxlib.sqltools.TableWrapper;

public class Intersect extends TableWrapper {

	public final String ATB_TREE_ID1 = "treeID1";
	public final String ATB_TREE_ID2 = "treeID2";
	public final String ATB_CNT = "cnt";
	public final String ATB_DIST = "dist";
	
	
	private String atbTreeID1 = ATB_TREE_ID1;
	private String atbTreeID2 = ATB_TREE_ID2;
	private String atbCnt = ATB_CNT;
	private String atbDist = ATB_DIST;
	
	private AggrProf idx1, idx2; 
	
	public Intersect(Connection con, String tblName) {
		super(con, tblName);
	}
	
	public void setIdxs(AggrProf idx1, AggrProf idx2) {
		this.idx1 = idx1;
		this.idx2 = idx2;
	}
	
	public void intersect(AggrProf t1, AggrProf t2) throws SQLException {
		this.idx1 = t1;
		this.idx2 = t2;
		String qry = "INSERT INTO " + this.getTblName()  
		+ " SELECT " + "A." + t1.getAtbTreeID() + "," 
		+ "B." + t2.getAtbTreeID() + "," 
		+ " SUM(LEAST(" + "A." + t1.getAtbCnt() + ",B." + t2.getAtbCnt() + "))" 
		+ ", NULL "
		+ " FROM " + t1.getTblName() + " AS A," 
		+ t2.getTblName() + " AS B WHERE " 
		+ "A." + t1.getAtbPQGram() + " = B." + t2.getAtbPQGram() 
		+ " GROUP BY A." + t1.getAtbTreeID() + ",B." + t2.getAtbTreeID();
		SQLTools.execute(this.getStatement(), qry, "Intersecting aggregated pq-gram tables: '" 
				+ t1.getTblName() + "' and '" + t2.getTblName() + "'");
		t1.getPqgTbl().getPsTbl().buildIndex();
		t2.getPqgTbl().getPsTbl().buildIndex();		
	}
	
	public void intersect(AggrProf t1, int treeID1, AggrProf t2, int treeID2) throws SQLException {
		this.idx1 = t1;
		this.idx2 = t2;
		String qry = "INSERT INTO " + this.getTblName()  
			+ " SELECT " + "A." + t1.getAtbTreeID() + "," 
			+ "B." + t2.getAtbTreeID() + "," 
			+ " SUM(LEAST(" + "A." + t1.getAtbCnt() + ",B." + t2.getAtbCnt() + "))" 
			+ ", NULL "
			+ " FROM " + t1.getTblName() + " AS A," 
			+ t2.getTblName() + " AS B WHERE " 
			+ "A." + t1.getAtbTreeID() + "=" + treeID1 + " AND " 
			+ "B." + t2.getAtbTreeID() + "=" + treeID2 + " AND " 
			+ "A." + t1.getAtbPQGram() + " = B." + t2.getAtbPQGram() 
			+ " GROUP BY A." + t1.getAtbTreeID() + ",B." + t2.getAtbTreeID();
		SQLTools.execute(this.getStatement(), qry, "Intersecting aggregated pq-gram tables: '" 
				+ t1.getTblName() + "' and '" + t2.getTblName() + "'");
	}
	
	/**
	 * Compute distance - needs to to intersection first!
	 * 
	 * @param ps1
	 * @param ps2
	 * @throws SQLException
	 */
	public void computeDist(ProfileSizeTbl ps1, int treeID1, ProfileSizeTbl ps2, int treeID2) throws SQLException {
		String isNm = this.getTblName();

		String qry =
			"UPDATE " + isNm + "," 
			+ ps1.getTblName() + " AS A,"   
			+ ps2.getTblName() + " AS B"   
			+ " SET " + isNm + "." + atbDist + "="
			+ "(A." + ps1.getAtbProfSize() + "+" 
			+ "B." + ps2.getAtbProfSize() + "-"
			+ "2*" + isNm + "." + atbCnt + ")/(" 
			+ "A." + ps1.getAtbProfSize() + "+" 
			+ "B." + ps2.getAtbProfSize() + "-"
			+ isNm + "." + atbCnt + ")"
			+ " WHERE "
			+ "A." + ps1.getAtbTreeID() + "=" + treeID1 + " AND " 
			+ "B." + ps2.getAtbTreeID() + "=" + treeID2 + " AND "
			+ isNm + "." + atbTreeID1 + "=" + "A." + ps1.getAtbTreeID() + " AND "
			+ isNm + "." + atbTreeID2 + "=" + "B." + ps2.getAtbTreeID();
		SQLTools.executeUpdate(this.getStatement(), qry,
				"Computing distance from intersection in '" + isNm + "'");
	}
	
	/**
	 * Compute distance - needs to to intersection first!
	 * 
	 * @param ps1
	 * @param ps2
	 * @throws SQLException
	 */
	public void computeDist(ProfileSizeTbl ps1, ProfileSizeTbl ps2) throws SQLException {
		String isNm = this.getTblName();

		// pseudo distance bag normalization
		String qry =
			"UPDATE " + isNm + "," 
			+ ps1.getTblName() + " AS A,"   
			+ ps2.getTblName() + " AS B"   
			+ " SET " + isNm + "." + atbDist + "="
			+ "(A." + ps1.getAtbProfSize() + "+" 
			+ "B." + ps2.getAtbProfSize() + "-"
			+ "2*" + isNm + "." + atbCnt + ")/(" 
			+ "A." + ps1.getAtbProfSize() + "+" 
			+ "B." + ps2.getAtbProfSize() + "-"
			+ isNm + "." + atbCnt + ")"
			+ " WHERE "
			+ isNm + "." + atbTreeID1 + "=" + "A." + ps1.getAtbTreeID() + " AND "
			+ isNm + "." + atbTreeID2 + "=" + "B." + ps2.getAtbTreeID();
		// no normalization
//		String qry =
//			"UPDATE " + isNm + "," 
//			+ ps1.getTblName() + " AS A,"   
//			+ ps2.getTblName() + " AS B"   
//			+ " SET " + isNm + "." + atbDist + "="
//			+ "(A." + ps1.getAtbProfSize() + "+" 
//			+ "B." + ps2.getAtbProfSize() + "-"
//			+ "2*" + isNm + "." + atbCnt + ")" 
//			+ " WHERE "
//			+ isNm + "." + atbTreeID1 + "=" + "A." + ps1.getAtbTreeID() + " AND "
//			+ isNm + "." + atbTreeID2 + "=" + "B." + ps2.getAtbTreeID();
//		System.out.println(qry);
		SQLTools.executeUpdate(this.getStatement(), qry,
				"Computing distance from intersection in '" + isNm + "'");
	}


	/**
	 * Get distance between treeID1 and treeID2. Requires to run intersec and computeDist first.
	 * @param treeID1
	 * @param treeID2
	 * @return dist between treeID1 and treeID2 if known, -1 otherwise
	 * @throws SQLException
	 */
	public double getDist(int treeID1, int treeID2) throws SQLException {
		String qry =
			"SELECT " + atbDist + " FROM " + this.getTblName() +
			" WHERE " + atbTreeID1 + "=" + treeID1 + 
			" AND " + atbTreeID2 + "=" + treeID2;
		ResultSet rs = this.getStatement().executeQuery(qry);
		if (rs.next()) {
			return rs.getDouble(this.atbDist);
		} else {
			return 1;
		}
	}
	
	/**
	 * Returns a comma-separated list of the attribute names in the following order: 
	 * treeID1, treeID2, cnt
	 * 
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	public String getAtbList() {
		return this.atbTreeID1 + "," + this.atbTreeID2 + "," 
		            + this.atbCnt + "," + this.atbDist;
	}

	public void create() throws SQLException {
		StringBuffer qryCreate = new StringBuffer("CREATE TABLE `" + getTblName() + "` (");
		qryCreate.append(atbTreeID1 + " INT NOT NULL,");
		qryCreate.append(atbTreeID2 + " INT NOT NULL,");
		qryCreate.append(atbCnt + " INT NOT NULL,");
		qryCreate.append(atbDist + " FLOAT");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());
	}

	/**
	 * @return Returns the atbTreeID1.
	 */
	public String getAtbTreeID1() {
		return atbTreeID1;
	}

	/**
	 * @return Returns the atbTreeID2.
	 */
	public String getAtbTreeID2() {
		return atbTreeID2;
	}
	
	

	/**
	 * @return Returns the atbDist.
	 */
	public String getAtbDist() {
		return atbDist;
	}

	/**
	 * @return Returns the atbCnt.
	 */
	public String getAtbCnt() {
		return atbCnt;
	}
	
	
	
	/**
	 * @return Returns the idx1.
	 */
	public AggrProf getIdx1() {
		return idx1;
	}
	/**
	 * @return Returns the idx2.
	 */
	public AggrProf getIdx2() {
		return idx2;
	}
	public void addIdx_treeIDs() throws SQLException {
		String qry = "ALTER TABLE " + this.getTblName() + " ADD UNIQUE treeIDs(" +
		             this.atbTreeID1 + "," + this.atbTreeID2 + ")";
		SQLTools.execute(this.getStatement(), qry, 
				"Creating index 'treeIDs' on '" + this.getTblName() + "'.");
	}

	public void dropIdx_treeIDs() throws SQLException {
		String qry = "ALTER TABLE " + this.getTblName() + " DROP INDEX treeIDs";
		SQLTools.execute(this.getStatement(), qry, 
				"Dropping index 'treeIDs' from '" + this.getTblName() + "'.");
	}	

	public int equalTreeIDs() throws SQLException {
		String qry = "SELECT COUNT(*) AS cnt FROM " + this.getTblName() +
		" WHERE " + this.atbTreeID1 + "=" + this.atbTreeID2;
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getInt("cnt");		
	}

	public int nonequalTreeIDs() throws SQLException {
		String qry = "SELECT COUNT(*) AS cnt FROM " + this.getTblName() +
		" WHERE " + this.atbTreeID1 + "!=" + this.atbTreeID2;
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		return rs.getInt("cnt");		
	}

	/**
	 * Get sum of distances between trees with the same treeID. 
	 * 
	 * @return
	 */
	public double getSumSelfDist() throws SQLException {
		String qry = "SELECT SUM(" + this.atbDist + ") AS sum FROM " + 
		this.getTblName() + " WHERE " + this.atbTreeID1 + "=" + this.atbTreeID2;
		ResultSet rs = this.getStatement().executeQuery(qry);
		rs.next();
		
		return rs.getDouble("sum");
	}
	
	/**
	 * Get sum of distances between trees with the same treeID. 
	 * 
	 * @return
	 */
	public double getMinSumNonselfDist(int treeNum1) throws SQLException {
		String qry = "SELECT " + this.atbTreeID1 + 
			",MIN(" + this.atbDist + ") as min FROM " + 
			this.getTblName() + " WHERE " + this.atbTreeID1 + "!=" + this.atbTreeID2 +
			" GROUP BY " + this.atbTreeID1;
		ResultSet rs = this.getStatement().executeQuery(qry);
		double sum = 0;
		int i = 0;
		while (rs.next()) {
			sum += rs.getDouble("min");
			i++;
		}
		sum += (treeNum1 - i);
		return sum;
	}
	
}
