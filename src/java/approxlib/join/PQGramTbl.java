/*
 * Created on Aug 2, 2005
 */
package approxlib.join;

import java.sql.Connection;
import java.sql.SQLException;

import approxlib.sqltools.InsertBuffer;
import approxlib.sqltools.TableWrapper;
import approxlib.sqltools.SQLTools;

import java.util.LinkedList;

import approxlib.hash.FixedLengthHash;

/**
 * Stores pq-gram in a table with schema (treeID, pqgram). 
 * The pq-grams are hashed, the hash values are concatenated and stored.
 * 
 * Note: Some DMBS remove trailing spaces for CHAR attributes. As all pqgrams have the same length, when loading them,
 * it is easy to add removed spaces again.
 * 
 * @author augsten
 */
public class PQGramTbl extends TableWrapper {
	
	/**
	 * default name of the tree index attribute  
	 */
	public final String ATB_TREE_ID = "treeID";
	
	/**
	 * default name of the hashed pq-gram   
	 */
	public final String ATB_PQ_GRAM = "pqGram";
	
	private String atbTreeID = ATB_TREE_ID;
	private String atbPQGram = ATB_PQ_GRAM;
	
	private FixedLengthHash hf;

	/**
	 * p-parameter of the pq-grams stored in the profile.
	 */
	private int p;

	/**
	 * q-parameter of the pq-grams stored in the profile.
	 */
	private int q;
	
	/**
	 * The number of inserted pq-grams since the last counter reset.
	 */
	private int insertCnt;
	
	/**
	 * Related profile size table. Should be 
	 * set by the method that computes the pq-grams.
	 */
	private ProfileSizeTbl psTbl;
	
	/**
	 * @param con
	 * @param insBuff
	 * @param tblName
	 * @param hf
	 * @param p
	 * @param q
	 */
	public PQGramTbl(Connection con, InsertBuffer insBuff,
			String tblName, FixedLengthHash hf, int p, int q) {
		super(con, con, insBuff, tblName);
		this.hf = hf;
		this.q = q;
		this.p = p;
		this.insertCnt = 0;
	}

	/**
	 * Returns a comma-separated list of the attribute names in the following order: 
	 * treeID, pqGram
	 * 
	 * Overridden method.
	 *
	 * @see sqltools.TableWrapper#getAtbList()
	 */
	@Override
	public String getAtbList() {		
		return this.atbTreeID + "," + this.atbPQGram;
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
		qryCreate.append(atbPQGram + " CHAR(" + (this.hf.getLength() * (this.q + this.p)) + ")");
		qryCreate.append(")");
		this.getStatement().execute(qryCreate.toString());
	}
	
	
	/**
	 * Add a single pq-gram.
	 * 
	 * @param treeID
	 * @param ppart p-part of the pq-gram
	 * @param qpart q-part of the pq-gram
	 * 
	 * @throws SQLException 
	 * @throws RuntimeException
	 */
	public void addPQGram(int treeID, LinkedList ppart, LinkedList qpart)  
	throws SQLException,RuntimeException {
		// if pq-gram is compatible...
		if ((ppart.size() == this.p) && (qpart.size() == this.q)) {
			String pqgStr = SQLTools.escapeSingleQuote(
					hf.concatLst(ppart) + hf.concatLst(qpart));
			this.getInsBuff().insert("(" + treeID + ",'" + pqgStr + "')");
			insertCnt++;
		} else {
			throw new RuntimeException("\n\n  Dimensions of p- and/or q-part " + 
					"(p=" + ppart.size() + ", q=" + qpart.size() + ") not compatible\n" +
					"  with the pq-grams already stored in the table '" + 
					this.getTblName() + "' (p=" + this.p + ", q=" + this.q + ").\n");
		}
	}

	
	/**
	 * @return Returns the q.
	 */
	public int getQ() {
		return q;
	}
	/**
	 * @return Returns the q.
	 */
	public int getP() {
		return p;
	}
	/**
	 * @return hash function used to store the p-parts
	 */
	public FixedLengthHash getHf() {
		return hf;
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
	
	
	/**
	 * @return Returns the number of inserted tuple since the last reset of the counter.
	 */
	public int getInsertCnt() {
		return insertCnt;
	}
	
	/**
	 * Resets the number of inserted tuples.
	 * @see getInsertCnt()
	 */
	public void resetInsertCnt() {
		insertCnt = 0;
	}

	/**
	 * @return Returns the psTbl.
	 */
	public ProfileSizeTbl getPsTbl() {
		return psTbl;
	}
	/**
	 * @param psTbl The psTbl to set.
	 */
	public void setPsTbl(ProfileSizeTbl psTbl) {
		this.psTbl = psTbl;
	}

}
