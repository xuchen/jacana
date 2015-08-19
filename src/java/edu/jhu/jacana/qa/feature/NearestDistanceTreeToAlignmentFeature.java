/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;

/**
 * Adding this feature decreases the score from 47 to 35: I guess that dep parses don't
 * quite show the answer pattern...
 * @author Xuchen Yao
 *
 */
@Deprecated
public class NearestDistanceTreeToAlignmentFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public NearestDistanceTreeToAlignmentFeature() {
		super("nearest_alignment_tree");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		// do set it to false
		// when not using: 36/222, when true: 40/222, when false:38/222
		boolean normalize = false;
		int size = aTree.getSize();
		String[] features = new String[size];
		ArrayList<Integer> alignIndex = new ArrayList<Integer>();
		ArrayList<String> featList = new ArrayList<String>(); 
		HashMap<Integer, Integer> alignIndex1to2 = new HashMap<Integer, Integer>(); 
		
		// get all alignment index for non stopwords
		for (Integer id:dist.getAlign1to2().keySet()) {
			if (!EditDist.stopWordPosTags.contains(dist.getPos1()[id])) {
				alignIndex.add(dist.id2idxInWordOrder1(id));
				alignIndex1to2.put(dist.id2idxInWordOrder1(id), dist.id2idxInWordOrder2(dist.getAlign1to2().get(id)));
			}
		}
		
		// isn't necessary
		Integer[] sortedAlignIndex = alignIndex.toArray(new Integer[alignIndex.size()]);
		Arrays.sort(sortedAlignIndex);
		
		double[] minDist = new double[size];
		int[] nearestIdx = new int[size];
		
		if (sortedAlignIndex.length == 0) {
			Arrays.fill(minDist, size);
			for (int i=0; i<size; i++) {
				features[i] = this.featureName+":"+minDist[i];
			}
			return features;
		} else {
			for (int i=0; i<size; i++) {
				int min = size;
				TreeGraphNode fromNode = aTree.getTree().get(i);
				for (int j=0; j<sortedAlignIndex.length; j++) {
					// Given nodes t1 and t2 which are dominated by this node, returns a list of all the nodes on the path from t1 to t2, inclusive, or null if none found.
					List<Tree> paths = aTree.getRoot().pathNodeToNode(fromNode, aTree.getTree().get(sortedAlignIndex[j]));
					int dist1 = paths.size() - 1;
					
					if (dist1 < min) {
						min = dist1;
						nearestIdx[i] = sortedAlignIndex[j];
					}
				}
				minDist[i] = min;
			}
		}
		
		if (normalize) {
			for (int i=0; i<size; i++) {
				minDist[i] = minDist[i]*1.0/size;
			}
		}
			
		for (int i=0; i<size; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.featureName+":"+minDist[i]);
			// these two features show the pos and dep of the word aligned to in the answer tree.
			sb.append(String.format("\t%s_from_pos=%s", this.featureName, aTree.getLabels().get(nearestIdx[i]).tag()));
			sb.append(String.format("\t%s_from_dep=%s", this.featureName, aTree.getDependencies().get(nearestIdx[i]).reln()));
			// the following two features brought down the score from 40/222 to 31/222
			//sb.append(String.format("\t%s_from_pos=%s:%f", this.featureName, aTree.getLabels().get(nearestIdx[i]).tag(), minDist[i]));
			//sb.append(String.format("\t%s_from_dep=%s:%f", this.featureName, aTree.getDependencies().get(nearestIdx[i]).reln(), minDist[i]));
			
			// the following features don't affect the score 40/222 to 40/222
			int nearestIdx2 = alignIndex1to2.get(nearestIdx[i]);
			sb.append(String.format("\t%s_to_pos=%s", this.featureName, qTree.getLabels().get(nearestIdx2).tag()));
			sb.append(String.format("\t%s_to_dep=%s", this.featureName, qTree.getDependencies().get(nearestIdx2).reln()));
			features[i] = sb.toString();			
		}
		
		return features;
	}

}
