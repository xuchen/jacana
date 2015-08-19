package approxlib.tasmTED;

import approxlib.util.Heap;
import approxlib.util.Histogram;
import approxlib.util.MemoryWatch;
import approxlib.distance.WeightingFunction;

/**
 * @author Nikolaus Augsten
 *
 */
public class TASMDynamic extends TASM {

	private float[] weightQue; // w(t1) = weights of the edit ops for the i-th node in preorder of t1
	private float[] weightDoc; // w(t2) = weights of the edit ops for the i-th node in preorder of t2
	private double treedist[][]; // intermediate treedist results
	private double[][] forestdist; // intermediate forest dist results
	WeightingFunction weightingFunction1, weightingFunction2;

	
	public TASMDynamic(WeightingFunction wQuery, WeightingFunction wDoc) {
		this.weightingFunction1 = wQuery;
		this.weightingFunction2 = wDoc;
	}

	protected static int prefixDist(TEDTree que, TEDTree doc, int q, int d, Heap topK,
			double[][] forestdist, double[][] treedist, float[] weightQue, float[] weightDoc) {		
		forestdist[que.lld(q) - 1][doc.lld(d) - 1] = 0;
		int docPrefixSize = -1;
		for (int dd = doc.lld(d); dd <= d; dd++) {    // traverse subtree rooted in node d of the document
 		   	float costIns =  weightDoc[dd];
			forestdist[que.lld(q) - 1][dd] = forestdist[que.lld(q) - 1][dd - 1] + costIns; 
			
			docPrefixSize = dd - doc.lld(d) + 1;
			double lowerBound = docPrefixSize - que.getNodeCount(); // lower bound for distance (que, subtree rooted in dd)
			// try to prune columns >= dd in the forest dist matrix			
			/*
			 * Note: due to lowerBound >= (as opposed to >) we might prune nodes with smaller postorder number
			 * that are computed later. Thus the results can differ from the naive algorithm which computes all nodes
			 * in strict postorder. This could be fixed by not pruning if lowerbound = ..., but dd < topK.peek.postorder 
			 */
			if ((topK.getSize() == topK.getMaxSize())    // we already have k subtrees 
					&& (lowerBound >= ((NodeDistPair)topK.peek()).getDist())) { // lower bound
				return docPrefixSize;
			}
			
			for (int dq = que.lld(q); dq <= q; dq++) { // traverse subtree rooted in node q of the query
				float costDel =  weightQue[dq];
				forestdist[dq][doc.lld(d) - 1] = forestdist[dq - 1][doc.lld(d) - 1] + costDel;
				
				if ((que.lld(dq) == que.lld(q)) &&
						(doc.lld(dd) == doc.lld(d))) {
					float costRen = 0;
					if (!que.equalsLbl(dq, doc, dd)) {
						costRen = (weightQue[dq] + weightDoc[dd]) / 2;
					}
					forestdist[dq][dd] = 
						Math.min(Math.min(forestdist[dq - 1][dd] + costDel,
								forestdist[dq][dd - 1] + costIns),
								forestdist[dq - 1][dd - 1] + costRen);
					treedist[dq][dd] = forestdist[dq][dd];

					// update the ranking
					NodeDistPair candidate = new NodeDistPair(doc.getGlobalID(dd), treedist[dq][dd]);
					if (dq == que.root() && // distance between whole query and subtree of the document
							(topK.getSize() < topK.getMaxSize() ||  // heap is not full or  
									candidate.compareTo(topK.peek()) < 0)) { // candidate is better
						if (!topK.insert(candidate)) {
							topK.substitute(candidate);	// otherwise substitute
						}

					}
				} else {
					forestdist[dq][dd] = 
						Math.min(Math.min(forestdist[dq - 1][dd] + costDel,
								forestdist[dq][dd - 1] + costIns),
								forestdist[que.lld(dq) - 1][doc.lld(dd) -1] + 
								treedist[dq][dd]);
				}
			}
		}
		return docPrefixSize;
	}

	/* (non-Javadoc)
	 * @see tasmTED.TASM#tasm(distance.TEDTree, distance.TEDTree, int)
	 */
	@Override
	public Heap tasm(TEDTree que, TEDTree doc, int k) {

		Heap topK = new Heap(k);

		if (k == 0) {
			return topK;
		}
		
		int[] krQue = TEDTree.getKeyRoots(que);
		int[] krDoc = TEDTree.getKeyRoots(doc);

		weightQue = this.weightingFunction1.getWeights(que);
		weightDoc = this.weightingFunction2.getWeights(doc);
		
		treedist = new double[que.getNodeCount() + 1][doc.getNodeCount() + 1];
		forestdist = new double[que.getNodeCount() + 1][doc.getNodeCount() + 1];

		MemoryWatch.measure();

		for (int d = 1; d < krDoc.length; d++) {
			int computedPrefixSize = Integer.MIN_VALUE;					
			for (int q = 1; q < krQue.length; q++) {
				computedPrefixSize = Math.max(computedPrefixSize, 
						prefixDist(que, doc, krQue[q], krDoc[d], topK, 
						forestdist, treedist, weightQue, weightDoc));
			}
			Histogram.put(computedPrefixSize);
		}
		return topK;
	}
	

	
}
