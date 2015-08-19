/*
 * Created on Nov 3, 2006
 */
package approxlib.tree;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * @author augsten
 */
public interface Forest {

	/**
	 * Store a new tree into this forest. 
	 * The tree parameter is passed by value, i.e., a copy of the tree is stored. 
	 *    
	 * 
	 * @param t tree to store
	 */
	public void storeTree(LblValTree t) throws SQLException;

	/**
	 * Store all trees of a forest into this forest. 
	 * The forest argument is passed by value, i.e., a copy of the forest is stored.
	 * 
	 * @param f forest to store
	 */
	public void storeForest(MMForest f) throws SQLException;
	
	/**
	 * Load a single tree from a forest. If no tree with the given 
	 * ID can be found, null is returned. 
	 * A copy of the tree is returned; changing the returned tree has
	 * no effect on this forest.  
	 * 
	 * @param treeID ID of the tree to load
	 * @return copy of the tree with ID <code>treeID</code> (null if tree does not exist)
	 * @throws SQLException
	 */
	public LblValTree loadTree(int treeID) throws SQLException;
	
	/**
	 * Load all trees of this forest into main memory. 
	 * A copy of the trees is returned; changing the returned trees has
	 * no effect on this forest.  
	 * 
	 * @return forest
	 * @throws SQLException
	 */
	public MMForest loadForest() throws SQLException;
	
	/**
	 * Get the ID-values of all trees in this forest.
	 * 
	 * @return array of tree IDs
	 * @throws SQLException
	 */
	public int[] getTreeIDs() throws SQLException;
	
	/**
	 * Iterate through all the trees of this forest in arbitrary order.
	 *  
	 * @return iterator over all the trees in the forest
	 * @throws SQLException
	 */
	public Iterator<LblValTree> forestIterator() throws SQLException;
	
	/**
	 * Get the number of trees in the forest.
	 * 
	 * @return number of trees in the forest.
	 * @throws SQLException
	 */
	public long getForestSize() throws SQLException;		
		
}
