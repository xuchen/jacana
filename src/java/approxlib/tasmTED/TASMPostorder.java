/**
 * 
 */
package approxlib.tasmTED;

import approxlib.distance.WeightingFunction;
import approxlib.util.Heap;
import approxlib.util.Histogram;
import approxlib.util.MemoryWatch;

/**
 * @author Nikolaus Augsten
 *
 */
public class TASMPostorder extends TASM implements PostorderQueue {

	public static boolean DEBUG = false;
	
	// initilization
	private WeightingFunction weightingFunction1, weightingFunction2;
	private PostorderSource postorderSource;
	
	// input parameters to TASM
	private TEDTree que;
	private RingBuffTEDTreeKR streamDoc;
	private int k;
	
	// variables used by TASM computation
	private Heap topK;
	private int computeNext = 0;
	
	private int[] krQue;
	
	double[][] forestdist;
	double[][] treedist;

	float[] weightQue;

	
	/**
	 * @param wQuery
	 * @param wDoc
	 * @param postorderSource
	 */
	public TASMPostorder(WeightingFunction wQuery, WeightingFunction wDoc, 
			PostorderSource postorderSource) {
		this.weightingFunction1 = wQuery;
		this.weightingFunction2 = wDoc;
		this.postorderSource = postorderSource;
	}

	/* (non-Javadoc)
	 * @see tasmTED.TASM#tasm(distance.TEDTree, distance.TEDTree, int)
	 */
	@Override
	public Heap tasm(TEDTree que, TEDTree doc, int k) {
		if (k == 0) return new Heap(0);
		
		// copy parameters
		int maxSubtreeSize = (int)Math.round(Math.ceil(que.getNodeCount()  
				+ weightingFunction1.maxNodeWeightSum(que.getNodeCount()) 
				+ weightingFunction2.maxNodeWeightSum(k)));
		this.que = que;
		this.k = k;
		
		/*
		 * Do not increase the label dictionary for the document.
		 * We can do this as we only need to check equality to the labels
		 * of the query. The label IDs of the document nodes that do 
		 * not appear in the query all have the same label ID
		 * LabelDictionary.KEY_DUMMY_LABEL.
		 */
		que.getLabelDictionary().setNewLabelsAllowed(false);

		this.streamDoc = new RingBuffTEDTreeKR(maxSubtreeSize, que.getLabelDictionary());

		// initialize
		this.topK = new Heap(k);
		this.computeNext = 1;
		this.krQue = TEDTree.getKeyRoots(que);
		this.weightQue = this.weightingFunction1.getWeights(que);
				
		forestdist = new double[que.getNodeCount() + 1][streamDoc.getBufferSize() + 1];
		treedist = new double[que.getNodeCount() + 1][streamDoc.getBufferSize() + 1];
		
		// start computation
		postorderSource.appendTo(this);
		
		// compute topK for root node and merge with previous topK list
		this.topK.merge( 
			subtreeTASM(this.que, this.streamDoc, this.k, 
					this.computeNext, this.streamDoc.root(),
					new TASMDynamic(this.weightingFunction1, 
							this.weightingFunction2)));

		
		return topK;
	}


	private Heap subtreeTASM(TEDTree que, RingBuffTEDTreeKR doc, 
			int k, int computeNext, int subtreeRoot, TASMDynamic tasmDynamic) {
		
		Heap topK = new Heap(k);
		int U = doc.getBufferSize() - que.getNodeCount();
		while (subtreeRoot >= computeNext) {
			int subtreeSize = subtreeRoot - doc.lld(subtreeRoot) + 1;	
			int L = Math.abs(subtreeSize - que.getNodeCount());
			if (L <= U && (topK.getSize() < k || 
					L < ((NodeDistPair)topK.peek()).getDist())) {
				// get subtree
				RingBuffTEDTreeKR subtree = 
					doc.getSubtree(subtreeRoot);
				if (DEBUG) System.out.println("subtree starting at " + 
						computeNext + ": " + subtree);				

				// compute TASM for 'subtree'				
				int[] krSubtree = TEDTree.getKeyRoots(subtree);				
				float[] weightDoc = this.weightingFunction2.getWeights(subtree);

				MemoryWatch.measure();
				
				for (int d = 1; d < krSubtree.length; d++) {
					int computedPrefixSize = Integer.MIN_VALUE;					
					for (int q = 1; q < krQue.length; q++) {
						computedPrefixSize = Math.max(computedPrefixSize, 
							TASMDynamic.prefixDist(que, subtree, krQue[q], krSubtree[d], topK,
								forestdist, treedist, weightQue, weightDoc));
					}
					Histogram.put(computedPrefixSize);
				}
				if (DEBUG) System.out.println("top-k after computing subtree: " + topK);				
				
				// move to next subtree that must be computed
				subtreeRoot -= subtree.getNodeCount();
			} else {
				// move to next subtree that must be computed
				subtreeRoot--;
			}
			
		}
		if (DEBUG) System.out.println("topK of subtreeTASM call: " + topK);
		return topK;
	}
	
	/* (non-Javadoc)
	 * @see tasmTED.PostorderQueue#append(java.lang.String, int)
	 */
	public void append(String label, int subtreeSize) {
		if (DEBUG) System.out.println((streamDoc.getNodeCount() + 1) + 
				": (" + label + "," + subtreeSize + ")");
		if (streamDoc.getNodeCount() >= streamDoc.getBufferSize() && // ringbuffer is already filled
				computeNext == streamDoc.smallestValidID()) { // next node would shift out uncomputed node

			// next uncomputed node is a leaf
			if (streamDoc.lld(computeNext) == computeNext) { 

				// get root of leafmost subtree
				int rootNext = streamDoc.getLeafKeyRoot(computeNext);

				// compute TASM for leafmost subtree
				Heap subtreeTopK = subtreeTASM(this.que, this.streamDoc, 
						this.k, this.computeNext, rootNext,
						new TASMDynamic(this.weightingFunction1, 
								this.weightingFunction2));

				// merge topK of subtrees with current topK
				topK.merge(subtreeTopK);
				if (DEBUG) System.out.println("global topK after subtreeTASM call: " + this.topK);

				// set "this.computeNext" to node after leftmost subtree root
				computeNext += rootNext - computeNext + 1;	
			} else { // next uncomputed node is the root of a subtree that can be pruned
				if (DEBUG) System.out.println("skipping computeNext (non-leaf): " + computeNext +
						" " + streamDoc.getLabel(computeNext));
				computeNext++; 
			}
			
		}
		// append this node
		streamDoc.append(label, subtreeSize);
		
	}

}
