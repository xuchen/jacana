/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;

/**
 * @author Xuchen Yao
 *
 */
public class PosFeature extends AbstractFeatureExtractor {
	
	public PosFeature() {
		super("pos");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extract(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {

		String[] features = new String[aTree.getLabels().size()];
		for (int i=0; i<features.length; i++) {
			features[i] = aTree.getLabels().get(i).tag();
		}
		return features;
	}

}
