/**
 * 
 */
package approxlib.distance;

import approxlib.tasmTED.StaticTEDTree;
import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;
import approxlib.tree.LblValTree;

/**
 * As {@link distance.SubtreeWeighting}, but the leaf nodes can have an other weight 
 * which is the same for all leaf nodes. 
 * The leaf weight does not affect the weiht of the other nodes.
 * 
 * @author naugsten
 *
 */
public class SubtreeLeafWeighting extends WeightingFunction {

	private double exponent;
	private float leafWeight;
	private LblValTree query;

	/**
	 * 
	 * @param query tree for which the weight is to be computed.
	 * @param exponent
	 * @param leafWeight
	 * @throws RuntimeException
	 */
	public SubtreeLeafWeighting(LblValTree query, double exponent, float leafWeight) throws RuntimeException {
		this.exponent = exponent;
		this.leafWeight = leafWeight;
		this.query = query;
	}

	/* (non-Javadoc)
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
	public float[] getWeights(TEDTree ted) {
		float[] weights = new float[ted.getNodeCount() + 1];
		for (int i = 1; i < weights.length; i++) {
			// leaf
			if (ted.lld(i) == i) { 
				weights[i] = this.leafWeight;
			} else {
				weights[i] = (i - ted.lld(i) + 1);				
				weights[i] = (float)Math.pow(weights[i] + 1, this.getExponent());
			}
		}
		return weights;
	}
	
	/**
	 * @return the exponent
	 */
	public double getExponent() {
		return exponent;
	}

	@Override
	public float maxNodeWeightSum(int treeSize) {
		float[] weights = this.getWeights(query);
		float sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += weights[i];
		}
		return sum;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[exponent=" + this.exponent + ",leafWeight=" + this.leafWeight +"]";
	}


}
