/**
 * 
 */
package approxlib.distance;

import approxlib.tasmTED.TEDTree;

/**
 * @author naugsten
 *
 */
public interface SubtreeDist {

	/**
	 * Compute the distance between the query tree and all subtrees of the document tree.
	 * The returned double array contains at position i the distance between
	 * t1 and the tree rooted in the i-th node of t2 in postorder.
	 * The postorder count start with 1, the element at position 0 is not defined.
	 * 
	 * @param query
	 * @param document
	 * @return
	 */
	public double[] subtreeDists(TEDTree t1, TEDTree t2);

}
