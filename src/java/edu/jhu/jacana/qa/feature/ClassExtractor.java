/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.List;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;

/**
 * @author Xuchen Yao
 *
 */
public class ClassExtractor extends AbstractFeatureExtractor {
	

	public ClassExtractor() {
		super("class");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extract(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree, DependencyTree aTree) {
		
		List<String> answers = aTree.getAnswers();
		String[] classes = answers.toArray(new String[answers.size()]);
		return classes;
	}
	
	@Override
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template) {
		return extractSingleFeature(dist, qTree, aTree);
	}

}
