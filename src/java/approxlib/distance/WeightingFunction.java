/*
 * Created on Apr 28, 2008
 */
package approxlib.distance;

import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;

public abstract class WeightingFunction {
	
	/**
	 * Compute a weight for each node in the tree <code>t</code>. 
	 * The node weights in the returned array are sorted by the 
	 * postorder positions of the nodes (starting with 1), 
	 * i.e. the weight of the left-most leaf is stored at position 1, 
	 * the weight of the root in position <code>|t|</code>.  
	 * 
	 * @param t rooted, ordered, labeled tree
	 * @return an array with a weight for each node in <b>t</b>
	 */
	abstract public float[] getWeights(LblTree t);

	abstract public float[] getWeights(TEDTree t);

	abstract public float maxNodeWeightSum(int treeSize);
	
}
