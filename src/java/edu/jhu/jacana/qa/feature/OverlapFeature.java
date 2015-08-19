/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.HashSet;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.stanford.nlp.ling.WordLemmaTag;

/**
 * This class outputs a flag on whether the word in the answer sentence overlaps with
 * any words in the question sentence. The functionality might overlap with EditFeature.
 * However1: there are some cases that even if overlapping, TED still chooses to delete
 * the word (and then insert) instead of just renaming, then the CRF might tag this deleted
 * chunk as answer, even though it should be renaming. Thus we add one extra flag here.
 * However2: test shows that adding this feature degrades score from 33/222 to 31/222...
 * Well, let's not enable it for now... 
 * @author Xuchen Yao
 *
 */
public class OverlapFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public OverlapFeature() {
		super("overlap");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		String[] features = new String[aTree.getSize()];
		HashSet<String> qSet = new HashSet<String>();
		for (WordLemmaTag wlt: qTree.getLabels()) {
			qSet.add(wlt.lemma());
		}
		for (int i=0; i<aTree.getLabels().size(); i++) {
			if (qSet.contains(aTree.getLabels().get(i).lemma()))
				features[i] = "overlap=yes";
			else
				features[i] = "overlap=no";
		}
		return features;
	}

}
