/*
 * Created on Nov 3, 2006
 */
package approxlib.tree;

import java.sql.SQLException;

/**
 * @author augsten
 */
public class ForestConversion {

	private ForestConversion() {}
	
	public static void convert(Forest a1, Forest a2) throws SQLException {
		int[] ids = a1.getTreeIDs();
		for (int i = 0; i < ids.length; i++) {			
			a2.storeTree(a1.loadTree(ids[i]));
		}
	}
	
	public static void convertCleanNulls(Forest a1, Forest a2) throws SQLException {
		int[] ids = a1.getTreeIDs();
		for (int i = 0; i < ids.length; i++) {
			LblValTree t = a1.loadTree(ids[i]);
			t.cleanNullValues();
			a2.storeTree(t);
		}
	}
	
}
