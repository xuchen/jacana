/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;

/**
 * @author Xuchen Yao
 *
 */
@Deprecated
public class QuestionMatchDepFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public QuestionMatchDepFeature() {
		super("qword_dep");
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		DepFeature depEx = new DepFeature();
		String[] features = depEx.extractSingleFeature(dist, qTree, aTree);
		String q = QuestionWordExtractor.getQuestionWords(qTree);
		for (int i=0; i<features.length; i++) {
			features[i] = String.format("%s|%s|%s", featureName, features[i], q);
		}
		return features;
	}
	
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template) {
		return extractSingleFeature(dist, qTree, aTree);
	}

}
