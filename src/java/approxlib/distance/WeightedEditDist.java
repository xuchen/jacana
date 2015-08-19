package approxlib.distance;

import approxlib.tasmTED.StaticTEDTree;
import approxlib.tasmTED.TEDTree;
import approxlib.tree.LblTree;

public class WeightedEditDist extends TreeDist implements SubtreeDist {
	
	private float[] w1; // w(t1) = weights of the edit ops for the i-th node in preorder of t1
	private float[] w2; // w(t2) = weights of the edit ops for the i-th node in preorder of t2
	private double treedist[][]; // intermediate treedist results
	private double[][] forestdist; // intermediate forest dist results
	WeightingFunction weightingFunction1, weightingFunction2;
	
	public WeightedEditDist(WeightingFunction weightingFunction, boolean normalized) {
		this(weightingFunction, weightingFunction, normalized);
	}
	
	public WeightedEditDist(WeightingFunction weightingFunction1, 
			WeightingFunction weightingFunction2, boolean normalized) {
		super(normalized);
		this.weightingFunction1 = weightingFunction1;
		this.weightingFunction2 = weightingFunction2;
	}
	
	
    @Override
	public double treeDist(LblTree t1, LblTree t2) {
		return treeDist(new StaticTEDTree(t1), new StaticTEDTree(t2));
	}
    
    public double treeDist(TEDTree ted1, TEDTree ted2) {
    	double[][] treedist = this.computeTreeDistMatrix(ted1, ted2);
    	int n1 = treedist.length - 1;
    	int n2 = treedist[0].length - 1;
    	if (this.isNormalized()) {
        	return treedist[n1][n2] / (n1 + n2);
    	} else {
        	return treedist[n1][n2];
    	}    	
    }
    
	private double[][] computeTreeDistMatrix(TEDTree ted1, TEDTree ted2) {
		int[] kr1 = TEDTree.getKeyRoots(ted1);
		int[] kr2 = TEDTree.getKeyRoots(ted2);

		w1 = this.weightingFunction1.getWeights(ted1);
		w2 = this.weightingFunction2.getWeights(ted2);
		
		treedist = new double[ted1.getNodeCount() + 1][ted2.getNodeCount() + 1];
		forestdist = new double[ted1.getNodeCount() + 1][ted2.getNodeCount() + 1];

		for (int i = 1; i < kr1.length; i++) {
			for (int j = 1; j < kr2.length; j++) {
				forestDist(ted1, ted2, kr1[i], kr2[j]);
			}
		}
		return treedist;		
	}
		
	private void forestDist(TEDTree ted1, TEDTree ted2, int i, int j) {		
		forestdist[ted1.lld(i) - 1][ted2.lld(j) - 1] = 0;
		for (int di = ted1.lld(i); di <= i; di++) {
			float costDel =  w1[di];
			forestdist[di][ted2.lld(j) - 1] = forestdist[di - 1][ted2.lld(j) - 1] + costDel;
			for (int dj = ted2.lld(j); dj <= j; dj++) {
				float costIns =  w2[dj];
				forestdist[ted1.lld(i) - 1][dj] = forestdist[ted1.lld(i) - 1][dj - 1] + costIns; 
				
				if ((ted1.lld(di) == ted1.lld(i)) &&
						(ted2.lld(dj) == ted2.lld(j))) {
					float costRen = 0;
					if (!ted1.equalsLbl(di, ted2, dj)) {
						costRen = (w1[di] + w2[dj]) / 2;
					}
					forestdist[di][dj] = 
						Math.min(Math.min(forestdist[di - 1][dj] + costDel,
								forestdist[di][dj - 1] + costIns),
								forestdist[di - 1][dj - 1] + costRen);
					treedist[di][dj] = forestdist[di][dj];
				} else {
					forestdist[di][dj] = 
						Math.min(Math.min(forestdist[di - 1][dj] + costDel,
								forestdist[di][dj - 1] + costIns),
								forestdist[ted1.lld(di) - 1][ted2.lld(dj) -1] + 
								treedist[di][dj]);
				}
			}
		}		
	}
	
	@Override
	public String toString() {
		String s = super.toString();
		if (weightingFunction1 == weightingFunction2) {
			s += "[" + this.weightingFunction1 + "]";
		} else {
			s += "[" + this.weightingFunction1 + "," +	this.weightingFunction2 + "]";			
		}
		return s;
	}

	
	
	/**
	 * @return the weightingFunction1
	 */
	public WeightingFunction getWeightingFunction1() {
		return weightingFunction1;
	}

	/**
	 * @return the weightingFunction2
	 */
	public WeightingFunction getWeightingFunction2() {
		return weightingFunction2;
	}

	/* (non-Javadoc)
	 * @see distance.SubtreeDist#subtreeDist(tree.LblValTree, tree.LblValTree)
	 */
	public double[] subtreeDists(TEDTree t1, TEDTree t2) {
		double[][] treedist = this.computeTreeDistMatrix(t1, t2);
		return treedist[t1.getNodeCount()];
	}

	public double maxDist(int treeSize1, int treeSize2) {
		return
			this.getWeightingFunction1().maxNodeWeightSum(treeSize1) +
			this.getWeightingFunction2().maxNodeWeightSum(treeSize2);
	}

}
