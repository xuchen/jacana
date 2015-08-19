/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.nlp.WordNet;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;

/**
 *
 * 2. tag each word with its synset level 5 from top (entity)
 */
public class WordnetTagFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public WordnetTagFeature() {
		super("wordnet_tag");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		int level = 7;
		String[] features = new String[aTree.getSize()];
		Arrays.fill(features, "null");
		
		for (int i=0; i<aTree.getSize(); i++) {
			String word = aTree.getLabels().get(i).word();
			String pos = aTree.getLabels().get(i).tag();
			//if (!pos.startsWith("nn")) continue;
			//System.out.println(word+" "+pos);
			String hypernym = WordNet.getHypernymByLevel(word, pos, level);
			if (hypernym != null)
				features[i] = hypernym;
			else
				features[i] = pos;
		}
		System.out.println("======================");
		System.out.println(aTree.getSentence());
		System.out.println(Arrays.toString(features));
		return features;
	}
	
}