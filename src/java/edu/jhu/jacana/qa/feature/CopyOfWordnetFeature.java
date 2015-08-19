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
public class CopyOfWordnetFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public CopyOfWordnetFeature() {
		super("wordnet");
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
		
		final String hypernymFeature = "_hyponym_of_q=yes";
		
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
					System.out.println(aTree.getLabels().get(i).word());
					System.out.println("-----------------------");
					System.out.println(qWord+"\t|||\t"+qTree.getSentence());
					System.out.println(aTree.getAnswers().get(i)+"\t|||\t"+aTree.getLabels().get(i).word()+"\t|||\t"+aTree.getSentence());
					StringBuilder sb = new StringBuilder();
					String f=this.featureName + hypernymFeature;
					//sb.append(f);
					String tag = aTree.getLabels().get(i).tag();
					String dep = aTree.getDependencies().get(i).reln().toString();
					//sb.append("\t"+f+"_pos="+tag);
					//sb.append("\t"+f+"_dep="+dep);
					//features[i] += "\t" + this.featureName + hypernymFeature + "_qword=" + qWord;
					if (hypernyms.contains(qContentWord)) {
						/*
						 * the question is "what sport does ... play ?"
						 * the answer is "tennis player ..."
						 * then the content word is "sport", and it's the hypernym of "tennis"
						 * then we add one indication feature for "tennis" here
						 */
						f = this.featureName + "_hyponym_of_question_content_word";
						sb.append("\t" + f);
						//sb.append("\t"+f+"_pos="+tag);
						//sb.append("\t"+f+"_dep="+dep);
						f = this.featureName + "_hyponym_of_question_content_word" + "_qword=" + qWord;
						sb.append("\t" + f);
						//sb.append("\t"+f+"_pos="+tag);
						//sb.append("\t"+f+"_dep="+dep);
						System.out.println("===================");
						System.out.println(qWord+"\t|||\t"+qContentWord+"\t|||\t"+qTree.getSentence());
						System.out.println(aTree.getAnswers().get(i)+"\t|||\t"+aTree.getLabels().get(i).word()+"\t|||\t"+aTree.getSentence());
					}
					features[i] = sb.toString();
				}
			}
		}

		return features;
	}
}