/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.Arrays;
import java.util.HashSet;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.nlp.WordNet;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;

/**
 * Notes:
 * 1. what film <-> what movie
 * in training, we only observe 'what film' and 'what movie' once each, this is not enough statistics for
 * gathering prob weight, maybe we can have what film == what movie when analyzing questions
 * 2. what year/month/date etc, how do we know this is asking about time?
 * 3. What    kind    of  dog was Toto    in  the Wizard  of  Oz
 * Answer: cairn   terrier (\in terrier \in hunting dog \in dog)
 *  What    kind    of  animal  was Winnie  the Pooh    ?
 * Answer: bear (\in animal)
 * What    kind    of  sports  team    is  the Buffalo Sabres  ?
 * Answer: hockey (\in sport)
 * What    sport   does    Jennifer    Capriati    play    ?
 * Answer: tennis (\in sport)
 * @author Xuchen Yao
 *
 */
public class WordnetQWordFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public WordnetQWordFeature() {
		super("wordnetQword_hyponym_of_q");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {

		String[] features = new String[aTree.getSize()];
		Arrays.fill(features, "");
		
		String qWord = QuestionWordExtractor.getQuestionWords(qTree);
		
		String qContentWord = null;
		int contentWordIdx = qTree.getQContentWordIdx();
		if (contentWordIdx != -1) {
			qContentWord = qTree.getLabels().get(contentWordIdx).lemma();
		}
		
		HashSet<String> qLemmas = new HashSet<String>();
		for (String s:dist.getLemma2())
			qLemmas.add(s);
		for (int i=0; i<aTree.getSize(); i++) {
			//System.out.println(aTree.getLabels().get(i).word() + " " + aTree.getLabels().get(i).tag());
			if (aTree.getLabels().get(i).tag().startsWith("vb"))
				continue;
			HashSet<String> hypernyms = WordNet.getAllHypernyms(aTree.getLabels().get(i).word(), aTree.getLabels().get(i).tag());
			if (hypernyms != null) {
				// intersect two sets
				hypernyms.retainAll(qLemmas);
				if (hypernyms.size() != 0) {
					if (hypernyms.contains(qContentWord)) {
						/*
						 * the question is "what sport does ... play ?"
						 * the answer is "tennis player ..."
						 * then the content word is "sport", and it's the hypernym of "tennis"
						 * then we add one indication feature for "tennis" here
						 */
						features[i] = featureName+"=yes";
					}

				}
			}
		}

		return features;
	}
}
