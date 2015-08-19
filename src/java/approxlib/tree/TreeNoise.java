/*
 * maybe you want to reuse the code?
 */

package approxlib.tree;

import java.util.Enumeration;

import approxlib.random.RandomVal;


public class TreeNoise {

	/**
	 * This is added to the label of a node when the node is renamed.
	 */
	public static String LABEL_SUFFIX = "_changed";
	/**
	 * This is added to the value of a node when the node is renamed.
	 */
	public static String VALUE_SUFFIX = "_changed";
	
	private TreeNoise() {
	}
	
	/**
	 * <p>
	 * Delete a specified number of nodes from a tree. The root node is never deleted.
	 * If the number of deletions exceeds the number of nodes in the tree, 
	 * all nodes but the root node will be deleted. 
	 * <p>
	 * The random variable is used to choose the positions of the nodes in preorder 
	 * that should be deleted. 
	 * 
	 * @param t tree to be changed 
	 * @param deletions number of deletions
	 * @param random random variable (e.g., new random.RandomVal())
	 */
	public static int randomDeleteNodes(LblValTree t, int deletions, RandomVal random) {
		// copy nodes to an array
		LblValTree[] nodeArr = new LblValTree[t.getNodeCount()];
		int i = 0;
		for (Enumeration<LblValTree> nodeEnum = t.preorderEnumeration(); nodeEnum.hasMoreElements();) {
			nodeArr[i++] = nodeEnum.nextElement();
		}
		// do random deletions
		deletions = Math.min(deletions, nodeArr.length - 1);
		int deletionsDone = 0;
		//System.out.print("\nDeleting nodes: ");
		while (deletionsDone < deletions) {
			int pos = random.getInt(0, nodeArr.length - 1);
			if ((pos >= 0) && (pos < nodeArr.length)) {
				LblValTree n = nodeArr[pos];
				if ((n != null) && (!n.isRoot())) {
					//System.out.print(n.getLabel() + ",");
					t.deleteNode(n);
					nodeArr[pos] = null;
					deletionsDone++;
				}
			}
		}
		return deletionsDone;
	}	

	/**
	 * <p>
	 * Rename a specified number of nodes of a tree. 
	 * If the number of renames exceeds the number of nodes in the tree, 
	 * all nodes are renamed. After a rename, a node <code>n</code> will have the label
	 * <code>n.getLabel()+TreeNoise.LABEL_SUFFIX</code> and the 
	 * value <code>n.getValue()+TreeNoise.VALUE_SUFFIX</code>. Null values 
	 * are not changed.
	 * <p>
	 * The random variable is used to choose the positions of the nodes in preorder 
	 * that should be renamed. 
	 *	
	 * @param t
	 * @param renames
	 * @param random
	 */
	public static int randomRenameNodes(LblValTree t, int renames, RandomVal random) {
		// copy nodes to an array
		LblValTree[] nodes = new LblValTree[t.getNodeCount()];
		int i = 0;
		for (Enumeration<LblValTree> nodeEnum = t.preorderEnumeration(); nodeEnum.hasMoreElements();) {
			nodes[i++] = nodeEnum.nextElement();
		}
		// do random deletions
		renames = Math.min(renames, nodes.length);
		int renamesDone = 0;
		//System.out.print("\nRenaming nodes: ");
		while (renamesDone < renames) {
			int pos = random.getInt(0, nodes.length - 1);
			if ((pos >= 0) && (pos < nodes.length)) {
				LblValTree n = nodes[pos];
				if (n != null) {
					//System.out.print(n.getLabel() + ",");
					n.setLabel(n.getLabel() + TreeNoise.LABEL_SUFFIX);
					if (n.getValue() != null) {
						n.setValue(n.getValue() + TreeNoise.VALUE_SUFFIX);
					}
					nodes[pos] = null;
					renamesDone++;
				}
			}
		}
		return renamesDone;
	}
	
}
