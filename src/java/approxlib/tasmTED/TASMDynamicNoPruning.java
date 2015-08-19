/**
 * 
 */
package approxlib.tasmTED;

import approxlib.util.Heap;
import approxlib.distance.WeightedEditDist;
import approxlib.distance.WeightingFunction;

/**
 * @author Nikolaus Augsten
 *
 */
public class TASMDynamicNoPruning extends TASM {

	private WeightedEditDist dist;
	
	public TASMDynamicNoPruning(WeightingFunction wQuery, WeightingFunction wDoc) {
		dist = new WeightedEditDist(wQuery, wDoc, false);
	}

	
	/* (non-Javadoc)
	 * @see tasmTED.TASM#tasm(distance.TEDTree, distance.TEDTree, int)
	 */
	@Override
	public Heap tasm(TEDTree t1, TEDTree t2, int k) {
		Heap topK = new Heap(k);
		if (k == 0) {
			return topK;
		}
		double dists[] = dist.subtreeDists(t1, t2);
		for (int i = 1; i <= t2.getNodeCount(); i++) {
			if (topK.getSize() < k || dists[i] < ((NodeDistPair)topK.peek()).getDist()) {
				NodeDistPair result = new NodeDistPair(t2.getGlobalID(i), dists[i]);
				if (!topK.insert(result)) {
					topK.substitute(result);
				}				
			}
		}
		return topK;
	}
	

}
