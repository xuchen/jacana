/**
 * 
 */
package approxlib.distance;

import approxlib.tasmTED.StaticTEDTree;
import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;

/**
 * @author naugsten
 *
 */
public class SubtreeWeighting extends WeightingFunction {

	private double exponent;
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	public SubtreeWeighting(double exponent) throws RuntimeException {
		this.exponent = exponent;
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
		if (this.exponent == 0) {
			return new UnitWeighting().getWeights(ted);
		}		
		float[] weights = new float[ted.getNodeCount() + 1];
		for (int i = 1; i < weights.length; i++) {
			weights[i] = (i - ted.lld(i) + 1);
			if (this.exponent == 1.0) {
				weights[i] = weights[i] + 1;
			} else if (this.exponent == 2.0) {
				weights[i] = (weights[i] + 1) * (weights[i] + 1);
			} else {
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
		double x = this.getExponent();
		float sum = 0;
		if (x == 0) {
			sum += treeSize;
		} else if (x > 0){
			for (int i = 1; i <= treeSize; i++) {	
				sum += Math.pow(i + 1, x);
			}
		} else {
			throw new RuntimeException("Maximum node weight not implemented for x < 0");
		}
		return sum;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[exponent=" + this.exponent + "]";
	}


}
