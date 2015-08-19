/*
 * Created on Apr 28, 2008
 */
package approxlib.distance;

import approxlib.tasmTED.StaticTEDTree;
import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;

/**
 * @author naugsten
 */
public class FanoutWeighting extends WeightingFunction {

	private int leafWeight;
	
	public FanoutWeighting(int leafWeight) {
		this.leafWeight = leafWeight;
	}
	
	/**
	 * @see distance.WeightingFunction#getWeights(tree.LblTree)
	 */
	@Override
	public float[] getWeights(LblTree t) {
		return getWeights(new StaticTEDTree(t));
	}

	/* (non-Javadoc)
	 * @see distance.WeightingFunction#getWeights(distance.TEDTree)
	 */
	@Override
	public float[] getWeights(TEDTree t) {
		float[] weights = new float[t.getNodeCount() + 1];
		for (int i = 1; i < weights.length; i++) {
			weights[i] = t.getFanout(i) + this.leafWeight;
		}
		return weights;
	}

	@Override
	public String toString() {
		return leafWeight + "";
	}

	@Override
	public float maxNodeWeightSum(int treeSize) {
		throw new RuntimeException("Method not implemented. Sorry.");
	}
	

}
