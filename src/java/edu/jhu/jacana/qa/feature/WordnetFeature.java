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
 * two ideas:
 * 1. nearest distance to hypernym/synonym/hyponym
 * 2. tag each word with its synset level 5 from top (entity)
 */
public class WordnetFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public WordnetFeature() {
		super("wordnet");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		
		boolean dist_feature = true;

		String[] features = new String[aTree.getSize()];
		Arrays.fill(features, "");
		
		String qWord = QuestionWordExtractor.getQuestionWords(qTree);
		
		String qContentWord = null;
		int contentWordIdx = qTree.getQContentWordIdx();
		if (contentWordIdx != -1) {
			qContentWord = qTree.getLabels().get(contentWordIdx).lemma();
		}
		
		final String hypernymFeature = "_has_q_hypernym";
		final String hypernymDistFeature = "_dist_to_q_hypernym:";
		final String hypernymDistContentFeature = "_dist_to_q_content_hypernym:";

		final String synonymFeature = "_has_q_synonym";
		final String synonymDistFeature = "_dist_to_q_synonym:";
		final String synonymDistContentFeature = "_dist_to_q_content_synonym:";
		
		final String hyponymFeature = "_has_q_hyponym";
		final String hyponymDistFeature = "_dist_to_q_hyponym:";
		final String hyponymDistContentFeature = "_dist_to_q_content_hyponym:";
		
		ArrayList<Integer> hypernymIdx = new ArrayList<Integer>();
		ArrayList<Integer> synonymIdx = new ArrayList<Integer>();
		ArrayList<Integer> hyponymIdx = new ArrayList<Integer>();
		// don't consider verbs in the question
		ArrayList<Integer> qLemmaIdx = new ArrayList<Integer>();
		for (int i=0; i<qTree.getSize(); i++) {
			if (!qTree.getLabels().get(i).tag().startsWith("vb"))
				qLemmaIdx.add(i);
		}
		for (int i=0; i<aTree.getSize(); i++) {
			//System.out.println(aTree.getLabels().get(i).word() + " " + aTree.getLabels().get(i).tag());
			if (aTree.getLabels().get(i).tag().startsWith("vb"))
				continue;
			StringBuilder sb = new StringBuilder();
			
			// hypernyms
			HashSet<String> hypernyms = WordNet.getAllHypernyms(aTree.getLabels().get(i).word(), aTree.getLabels().get(i).tag());
			
			if (hypernyms != null) {
				for(int j=0; j<qLemmaIdx.size(); j++) {
					int qIdx = qLemmaIdx.get(j);
					if (hypernyms.contains(qTree.getLabels().get(qIdx).lemma())) {
						hypernymIdx.add(i);
						String f=this.featureName + hypernymFeature;
						sb.append(f);
						f = this.featureName + hypernymFeature + "_qword=" + qWord;
						sb.append("\t"+f);
						// the distance to the front of question sentence
						f = this.featureName + hypernymDistFeature + qIdx;
						if (dist_feature) sb.append("\t"+f);
						if (contentWordIdx != -1) {
							// the distance to the question content word
							f = this.featureName + hypernymDistContentFeature + (qIdx-contentWordIdx+1);
							if (dist_feature) sb.append("\t"+f);
							if (contentWordIdx == qIdx) {
								f = this.featureName + hypernymFeature + "_q_content_word=" + qContentWord;
								sb.append("\t"+f);
							}
						}
						break;
					}
				}
			}
			
			// synonyms
			HashSet<String> synonyms = WordNet.getAllSynonyms(aTree.getLabels().get(i).word(), aTree.getLabels().get(i).tag());
			
			if (synonyms != null) {
				for(int j=0; j<qLemmaIdx.size(); j++) {
					int qIdx = qLemmaIdx.get(j);
					if (synonyms.contains(qTree.getLabels().get(qIdx).lemma())) {
						synonymIdx.add(i);
						String f=this.featureName + synonymFeature;
						sb.append("\t"+f);
						f = this.featureName + synonymFeature + "_qword=" + qWord;
						sb.append("\t"+f);
						// the distance to the front of question sentence
						f = this.featureName + synonymDistFeature + qIdx;
						if (dist_feature) sb.append("\t"+f);
						if (contentWordIdx != -1) {
							// the distance to the question content word
							f = this.featureName + synonymDistContentFeature + (qIdx-contentWordIdx+1);
							if (dist_feature) sb.append("\t"+f);
							if (contentWordIdx == qIdx) {
								f = this.featureName + synonymFeature + "_q_content_word=" + qContentWord;
								sb.append("\t"+f);
							}
						}
						break;
					}
				}
			}
			
			// hyponyms
			HashSet<String> hyponyms = WordNet.getAllHyponyms(aTree.getLabels().get(i).word(), aTree.getLabels().get(i).tag());
			
			if (hyponyms != null) {
				for(int j=0; j<qLemmaIdx.size(); j++) {
					int qIdx = qLemmaIdx.get(j);
					if (hyponyms.contains(qTree.getLabels().get(qIdx).lemma())) {
						hyponymIdx.add(i);
						String f=this.featureName + hyponymFeature;
						sb.append("\t"+f);
						f = this.featureName + hyponymFeature + "_qword=" + qWord;
						sb.append("\t"+f);
						// the distance to the front of question sentence
						f = this.featureName + hyponymDistFeature + qIdx;
						if (dist_feature) sb.append("\t"+f);
						if (contentWordIdx != -1) {
							// the distance to the question content word
							f = this.featureName + hyponymDistContentFeature + (qIdx-contentWordIdx+1);
							if (dist_feature) sb.append("\t"+f);
							if (contentWordIdx == qIdx) {
								f = this.featureName + hyponymFeature + "_q_content_word=" + qContentWord;
								sb.append("\t"+f);
							}
						}
						break;
					}
				}
			}
			
			
			features[i] = sb.toString();
		}
		
		// the following really didn't work...
//		double[] minDistHypernym = getMinDist(hypernymIdx, aTree.getSize());
//		double[] minDistSynonym = getMinDist(synonymIdx, aTree.getSize());
//		double[] minDistHyponym = getMinDist(hyponymIdx, aTree.getSize());
//		for (int i=0; i<aTree.getSize(); i++) {
//			StringBuilder sb = new StringBuilder();
//			if (minDistHypernym!=null) sb.append("\t"+"nearest_hypernym:"+minDistHypernym[i]);
//			if (minDistSynonym!=null) sb.append("\t"+"nearest_synonym:"+minDistSynonym[i]);
//			if (minDistHyponym!=null) sb.append("\t"+"nearest_hyponym:"+minDistHyponym[i]);
//			features[i] += sb.toString();
//		}

		return features;
	}
	
	private double[] getMinDist(ArrayList<Integer> alignIndex, int size) {
		Integer[] sortedAlignIndex = alignIndex.toArray(new Integer[alignIndex.size()]);
		Arrays.sort(sortedAlignIndex);
		
		double[] minDist = new double[size];
		int[] nearestIdx = new int[size];
		
		if (sortedAlignIndex.length == 0) {
			return null;
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
		return minDist;
	}
}