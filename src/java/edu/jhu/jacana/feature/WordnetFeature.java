/**
 * 
 */
package edu.jhu.jacana.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.jhu.jacana.nlp.WordNet;

import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public class WordnetFeature extends NormalizedFeatureExtractor {

	protected final static String numHypernym = "numHypernym",
			numSynonym = "numSynonym",
			numHyponym = "numHyponym",
			numEntail = "numEntail", 
			numCausing = "numCausing", 
			numMembersOf = "numMembersOf",
			numSubstancesOf = "numSubstancesOf",
			numPartsOf = "numPartsOf",
			numHaveMember = "numHaveMember",
			numHaveSubstance = "numHaveSubstance",
			numHavePart = "numHavePart",
			numAllWordnetCount = "numAllWordnetCount";

	/**
	 * @param normalized
	 */
	public WordnetFeature(boolean normalized) {
		super(normalized);
		features = new String[]{numHypernym, numSynonym, numHyponym, numEntail, numCausing, numMembersOf,
				numSubstancesOf, numPartsOf, numHaveMember, numHaveSubstance, numHavePart, numAllWordnetCount };
		switches = new HashMap<String, Boolean>() {
			{	
				put(numHypernym, true);
				put(numSynonym, true);
				put(numHyponym, true);
				put(numEntail, true);
				put(numCausing, true);
				put(numMembersOf, true);
				put(numSubstancesOf, true);
				put(numPartsOf, true);
				put(numHaveMember, true);
				put(numHaveSubstance, true);
				put(numHavePart, true);
				put(numAllWordnetCount, true);
			}
		};
	}


	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.NormalizedFeatureExtractor#getFeatureValues(approxlib.distance.EditDist)
	 */
	@Override
	public Double[] getFeatureValues(EditDist dist) {
		HashMap<String, Integer> featureVector = new HashMap<String, Integer> ();
		for (String f:features)
			featureVector.put(f, 0);
		double allNodes;
		if (normalized)
			allNodes = dist.getSize()*1.0;
		else
			allNodes = 1.0;
		
		int allCount = 0;
		String[] words1 = dist.getSentence1().split("\\s+");
		String[] words2 = dist.getSentence2().split("\\s+");
		String[] pos1 = dist.getPos1();
		String[] pos2 = dist.getPos2();
		
		HashSet<String> qLemmas = new HashSet<String>();
		for (String s:dist.getLemma1())
			qLemmas.add(s);
		int idx;
		for (int i=1; i<pos2.length; i++) {
			if (!pos2[i].startsWith("nn") && !pos2[i].startsWith("vb")) continue;
			idx = dist.id2idxInWordOrder2(i);
			HashSet<String> hypernyms = WordNet.getAllHypernyms(words2[idx], pos2[i]);
			HashSet<String> synonyms = WordNet.getAllSynonyms(words2[idx], pos2[i]);
			HashSet<String> hyponyms = WordNet.getAllHyponyms(words2[idx], pos2[i]);
			if (hypernyms != null) {
				// intersect two sets
				hypernyms.retainAll(qLemmas);
				if (hypernyms.size() != 0) {
					featureVector.put(numHypernym, featureVector.get(numHypernym)+1);
					allCount++;
				}
			}
			if (synonyms != null) {
				// intersect two sets
				synonyms.retainAll(qLemmas);
				if (synonyms.size() != 0) {
					featureVector.put(numSynonym, featureVector.get(numSynonym)+1);
					allCount++;
				}
			}
			if (hyponyms != null) {
				// intersect two sets
				hyponyms.retainAll(qLemmas);
				if (hyponyms.size() != 0) {
					featureVector.put(numHyponym, featureVector.get(numHyponym)+1);
					allCount++;
				}
			}
			if (pos2[i].startsWith("vb")) {
				HashSet<String> entails = WordNet.getEntailingSet(words2[idx]);
				if (entails != null) {
					// intersect two sets
					entails.retainAll(qLemmas);
					if (entails.size() != 0) {
						featureVector.put(numEntail, featureVector.get(numEntail)+1);
						allCount++;
					}
				}
				HashSet<String> causing = WordNet.getCausingSet(words2[idx]);
				if (causing != null) {
					// intersect two sets
					causing.retainAll(qLemmas);
					if (causing.size() != 0) {
						featureVector.put(numCausing, featureVector.get(numCausing)+1);
						allCount++;
					}
				}
			}
			if (pos2[i].startsWith("nn")) {
				HashSet<String> membersOf = WordNet.getMembersOfSet(words2[idx]);
				if (membersOf != null) {
					// intersect two sets
					membersOf.retainAll(qLemmas);
					if (membersOf.size() != 0) {
						featureVector.put(numMembersOf, featureVector.get(numMembersOf)+1);
						allCount++;
					}
				}
				HashSet<String> substancesOf = WordNet.getSubstancesOfSet(words2[idx]);
				if (substancesOf != null) {
					// intersect two sets
					substancesOf.retainAll(qLemmas);
					if (substancesOf.size() != 0) {
						featureVector.put(numSubstancesOf, featureVector.get(numSubstancesOf)+1);
						allCount++;
					}
				}
				HashSet<String> partsOf = WordNet.getPartsOfSet(words2[idx]);
				if (partsOf != null) {
					// intersect two sets
					partsOf.retainAll(qLemmas);
					if (partsOf.size() != 0) {
						featureVector.put(numPartsOf, featureVector.get(numPartsOf)+1);
						allCount++;
					}
				}
				HashSet<String> haveMember = WordNet.getHaveMemberSet(words2[idx]);
				if (haveMember != null) {
					// intersect two sets
					haveMember.retainAll(qLemmas);
					if (haveMember.size() != 0) {
						featureVector.put(numHaveMember, featureVector.get(numHaveMember)+1);
						allCount++;
					}
				}
				HashSet<String> haveSubstance = WordNet.getHaveSubstanceSet(words2[idx]);
				if (haveSubstance != null) {
					// intersect two sets
					haveSubstance.retainAll(qLemmas);
					if (haveSubstance.size() != 0) {
						featureVector.put(numHaveSubstance, featureVector.get(numHaveSubstance)+1);
						allCount++;
					}
				}
				HashSet<String> havePart = WordNet.getHavePartSet(words2[idx]);
				if (havePart != null) {
					// intersect two sets
					havePart.retainAll(qLemmas);
					if (havePart.size() != 0) {
						featureVector.put(numHavePart, featureVector.get(numHavePart)+1);
						allCount++;
					}
				}
			}
		}
	
		featureVector.put(numAllWordnetCount, featureVector.get(numAllWordnetCount)+allCount);
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			if (switches.get(f))
				values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);	
	}

}
