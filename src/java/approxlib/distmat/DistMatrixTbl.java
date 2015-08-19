/*
 * Created on Apr 20, 2007
 */
package approxlib.distmat;

import java.sql.Connection;
import java.sql.SQLException;

import approxlib.sqltools.InsertBuffer;
import approxlib.sqltools.TableWrapper;

public class DistMatrixTbl extends TableWrapper {

	String atbRow = "row";
	String atbCol = "col";
	String atbRowId = "rowId";
	String atbColId = "colId";
	String atbDist = "dist";
	
	public DistMatrixTbl(Connection con, Connection streamCon, InsertBuffer insBuff, String tblName) {
		super(con, streamCon, insBuff, tblName);
	}

	public DistMatrixTbl(Connection con, String tblName) {
		super(con, con, new InsertBuffer(), tblName);
	}
		
	@Override
	public void create() throws SQLException {
		String sqlCreate =
			"CREATE TABLE `" + this.getTblName() + "` (" 
			+ this.getAtbRow() + " INT NOT NULL,"			
			+ this.getAtbCol() + " INT NOT NULL,"
			+ this.getAtbRowId() + " INT NOT NULL,"
			+ this.getAtbColId() + " INT NOT NULL,"
			+ this.getAtbDist() + " DOUBLE PRECISION)"
			;
		this.getStatement().execute(sqlCreate);
	}

	@Override
	public String getAtbList() {
		return 
			this.getAtbRow() + "," +
			this.getAtbCol() + "," +
			this.getAtbRowId() + "," +
			this.getAtbColId() + "," +
			this.getAtbDist();
	}

	/**
	 * @return Returns the atbCol.
	 */
	public String getAtbCol() {
		return atbCol;
	}

	/**
	 * @param atbCol The atbCol to set.
	 */
	public void setAtbCol(String atbCol) {
		this.atbCol = atbCol;
	}

	/**
	 * @return Returns the atbColId.
	 */
	public String getAtbColId() {
		return atbColId;
	}

	/**
	 * @param atbColId The atbColId to set.
	 */
	public void setAtbColId(String atbColId) {
		this.atbColId = atbColId;
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

	/**
	 * @return Returns the atbRow.
	 */
	public String getAtbRow() {
		return atbRow;
	}

	/**
	 * @param atbRow The atbRow to set.
	 */
	public void setAtbRow(String atbRow) {
		this.atbRow = atbRow;
	}

	/**
	 * @return Returns the atbRowId.
	 */
	public String getAtbRowId() {
		return atbRowId;
	}

	/**
	 * @param atbRowId The atbRowId to set.
	 */
	public void setAtbRowId(String atbRowId) {
		this.atbRowId = atbRowId;
	}
	


}
