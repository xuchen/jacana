/**
 * 
 */
package approxlib.distance;

import java.util.Arrays;


import approxlib.tasmTED.StaticTEDTree;
import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;

/**
 * @author naugsten
 *
 */
public class UnitWeighting extends WeightingFunction {

	/* (non-Javadoc)
	 * @see distance.WeightingFunction#getWeights(tree.LblTree)
	 */
	@Override
	public float[] getWeights(LblTree t) {
		return getWeights(new StaticTEDTree(t));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {		
		return this.getClass().getSimpleName();
	}

	@Override
	public float maxNodeWeightSum(int treeSize) {
		return treeSize;
	}
	
	/* (non-Javadoc)
	 * @see distance.WeightingFunction#getWeights(distance.TEDTree)
	 */
	@Override
	public float[] getWeights(TEDTree t) {
		float[] weights = new float[t.getNodeCount() + 1];
		Arrays.fill(weights, 1);
		weights[0] = 0;
		return weights;
	}

	
}
