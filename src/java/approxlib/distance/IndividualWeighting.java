/**
 * 
 */
package approxlib.distance;

import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;

/**
 * @author naugsten
 *
 */
public class IndividualWeighting extends WeightingFunction {

	private float[] weights;

	/**
	 * @param weights the array  that {@link #getWeights(LblTree)} returns.
	 */
	public IndividualWeighting(float[] weights) {
		this.weights = weights;
	}

	/* (non-Javadoc)
	 * @see distance.WeightingFunction#getWeights(tree.LblTree)
	 */
	@Override
	public float[] getWeights(LblTree t) {		
		return this.weights;
	}

	@Override
	public float maxNodeWeightSum(int treeSize) {
		float maxNodeWeightSum = 0;
		for (int i = 0; i < treeSize; i++) {
			maxNodeWeightSum += weights[i];
		}
		return maxNodeWeightSum;
	}
	
	/* (non-Javadoc)
	 * @see distance.WeightingFunction#getWeights(distance.TEDTree)
	 */
	@Override
	public float[] getWeights(TEDTree t) {
		return this.weights;
	}
	

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(this.getClass().getSimpleName() + "[");
		for (int i = 0; i < weights.length; i++) {
			sb.append(weights[i] + (i == weights.length - 1 ? "]" : ","));
		}
		return sb.toString();
	}

	
}
