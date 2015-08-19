/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.Arrays;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;

/**
 * Some movie/book names are inside quotes `` '', but they can't be recognized by NER
 * so we add a simple feature.
 * @author Xuchen Yao
 *
 */
public class QuoteFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public QuoteFeature() {
		super("inside_quote");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		String[] features = new String[aTree.getLabels().size()];
		Boolean[] flags = new Boolean[aTree.getSize()];
		Arrays.fill(flags, false);
		
		for (int i=0; i<flags.length; i++) {
			String tag = aTree.getLabels().get(i).tag();
			if (tag.equals("``") && i+1 != flags.length) {
				flags[i+1] = true;
			} 
			
			if (!tag.equals("''") && i-1 >= 0 && flags[i-1] == true) {
				flags[i] = true;
			}
		}
		
		for (int i=0; i<flags.length; i++) {
			if (flags[i])
				features[i] = this.featureName;
			else
				features[i] = "";
		}
				
		return features;
	}
	
	@Override
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template) {
		return extractSingleFeature(dist, qTree, aTree);
	}
	
	@Override
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template, String[] appends) {
		return extractSingleFeature(dist, qTree, aTree);
	}

}
