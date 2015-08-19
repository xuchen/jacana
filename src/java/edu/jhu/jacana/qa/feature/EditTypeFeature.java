/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;

import approxlib.distance.Edit;
import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;

/**
 * @author Xuchen Yao
 *
 */
public class EditTypeFeature extends AbstractFeatureExtractor {

	/**
	 * 
	 */
	public EditTypeFeature() {
		super("edit");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extract()
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree, DependencyTree aTree) {
		ArrayList<Edit> editList = dist.getEditList();
		String[] features = new String[editList.size()];
		for (int i=0; i<editList.size(); i++) {
			Edit e = editList.get(i);
			features[i] = e.getType().toString();
		}

		return features;
	}

}
