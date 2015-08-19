/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;

/**
 * @author Xuchen Yao
 *
 */
public class NearestDistanceToAlignmentFeature extends AbstractFeatureExtractor {

	public NearestDistanceToAlignmentFeature() {
		super("nearest_alignment");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		// do set it to false
		// when not using: 36/222, when true: 38/222, when false:43/222
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
				for (int j=0; j<sortedAlignIndex.length; j++) {
					int dist1 = Math.abs(sortedAlignIndex[j]-i);
					if (dist1 < min) {
						min = dist1;
						nearestIdx[i] = sortedAlignIndex[j];
					} else
						break;
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
			// these two features show the pos and dep of the word aligned to in the answer tree. 43/222 -> 47/222
			sb.append(String.format("\t%s_from_pos=%s", this.featureName, aTree.getLabels().get(nearestIdx[i]).tag()));
			sb.append(String.format("\t%s_from_dep=%s", this.featureName, aTree.getDependencies().get(nearestIdx[i]).reln()));
			
			// this feature doesn't help on DEV (50/222 -> 43/222, see log at revision 103)
			sb.append(String.format("\t%s_from_ner=%s", this.featureName, aTree.getEntities().get(nearestIdx[i])));
			
			// the following two features brought down the score from 47/222 to 39/222
			//sb.append(String.format("\t%s_from_pos=%s:%f", this.featureName, aTree.getLabels().get(nearestIdx[i]).tag(), minDist[i]));
			//sb.append(String.format("\t%s_from_dep=%s:%f", this.featureName, aTree.getDependencies().get(nearestIdx[i]).reln(), minDist[i]));
			
			// the following features brought down the score from 47/222 to 43/222
			// int nearestIdx2 = alignIndex1to2.get(nearestIdx[i]);
			//sb.append(String.format("\t%s_to_pos=%s", this.featureName, qTree.getLabels().get(nearestIdx2).tag()));
			//sb.append(String.format("\t%s_to_dep=%s", this.featureName, qTree.getDependencies().get(nearestIdx2).reln()));
			features[i] = sb.toString();			
		}
		
		return features;
	}

}
