package approxlib.tasmTED;

import approxlib.util.Heap;
import approxlib.distance.WeightedEditDist;
import approxlib.distance.WeightingFunction;

public class TASMNaive extends TASM {

	private WeightedEditDist dist;
	
	public TASMNaive(WeightingFunction wQuery, WeightingFunction wDoc) {
		dist = new WeightedEditDist(wQuery, wDoc, false);
	}
	
	/* (non-Javadoc)
	 * @see tasmTED.TASM#tasm(distance.TEDTree, distance.TEDTree, int)
	 */
	@Override
	public Heap tasm(TEDTree ted1, TEDTree ted2, int k) {
		Heap topK = new Heap(k);
		if (k == 0) {
			return topK;
		}
		for (int i = 1; i <= ted2.getNodeCount(); i++) {
			
			// compute distance to subtree
			double d = dist.treeDist(ted1, ted2.getSubtree(i));
			if (topK.getSize() < k || d < ((NodeDistPair)topK.peek()).getDist()) {
				NodeDistPair result = new NodeDistPair(ted2.getGlobalID(i), d);
				if (!topK.insert(result)) {
					topK.substitute(result);
				}
			}			
		}
		return topK;
	}
	
}
