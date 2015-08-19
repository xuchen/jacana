/**
 * 
 */
package edu.jhu.jacana.feature;

import java.util.ArrayList;
import java.util.HashMap;

import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public class UneditedFeature extends NormalizedFeatureExtractor {

	protected final static String uneditedNodes="uneditedNodes", 
			uneditedNum="uneditedNum",
			uneditedNoun="uneditedNoun",
			uneditedVerb="uneditedVerb",
			uneditedProper="uneditedProperNoun",
			uneditedSubj="uneditedSubj",
			uneditedObj="uneditedObj",
			uneditedVMod="uneditedVMod",
			uneditedNMod="uneditedNMod",
			uneditedPMod="uneditedPMod";

	
	/**
	 * @param normalized
	 */
	public UneditedFeature(boolean normalized) {
		super(normalized);
		features = new String[]{uneditedNodes,
				uneditedNum,
				uneditedNoun,
				uneditedVerb,
				uneditedProper,
				uneditedSubj,
				uneditedObj,
				uneditedVMod,
				uneditedNMod,
				uneditedPMod
				};
		switches = new HashMap<String, Boolean>() {
			{	put(uneditedNodes, true);
				put(uneditedNum, true);
				put(uneditedNoun, true);
				put(uneditedVerb, true);
				put(uneditedProper, true);
				put(uneditedSubj, false);
				put(uneditedObj, false);
				put(uneditedVMod, false);
				put(uneditedNMod, false);
				put(uneditedPMod, false);
			}
		};
		
	}

	
	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#getFeatureValues(approxlib.distance.EditDist)
	 */
	public Double[] getFeatureValues(EditDist dist) {
		HashMap<String, Integer> featureVector = new HashMap<String, Integer> ();
		for (String f:features)
			featureVector.put(f, 0);

		double allNodes;
		if (normalized)
			allNodes = dist.getSize()*1.0;
		else
			allNodes = 1.0;

		HashMap<Integer, Integer> align1to2 = dist.getAlign1to2();
		String[] pos1 = dist.getPos1();
		String[] pos2 = dist.getPos2();
		
		String[] rel1 = dist.getRel1();
		String[] rel2 = dist.getRel2();
		// the aligned nodes are unedited nodes
		for (int i:align1to2.keySet()) {
			int j = align1to2.get(i);
			featureVector.put(uneditedNodes, featureVector.get(uneditedNodes)+2);
			
			if (pos1[i].startsWith("vb")) {
				featureVector.put(uneditedVerb, featureVector.get(uneditedVerb)+1);
			} else if (pos1[i].startsWith("cd")) {
				featureVector.put(uneditedNum, featureVector.get(uneditedNum)+1);
			} else if (pos1[i].startsWith("nnp")) {
				featureVector.put(uneditedProper, featureVector.get(uneditedProper)+1);
			} else if (pos1[i].startsWith("nn")) {
				featureVector.put(uneditedNoun, featureVector.get(uneditedNoun)+1);
			}  
			
			if (pos2[j].startsWith("vb")) {
				featureVector.put(uneditedVerb, featureVector.get(uneditedVerb)+1);
			} else if (pos2[j].startsWith("cd")) {
				featureVector.put(uneditedNum, featureVector.get(uneditedNum)+1);
			} else if (pos2[j].startsWith("nnp")) {
				featureVector.put(uneditedProper, featureVector.get(uneditedProper)+1);
			} else if (pos2[j].startsWith("nn")) {
				featureVector.put(uneditedNoun, featureVector.get(uneditedNoun)+1);
			}
			
			if (rel1[i].startsWith("sub")) {
				featureVector.put(uneditedSubj, featureVector.get(uneditedSubj)+1);
			} else if (rel1[i].startsWith("obj")) {
				featureVector.put(uneditedObj, featureVector.get(uneditedObj)+1);
			} else if (rel1[i].startsWith("vmod")) {
				featureVector.put(uneditedVMod, featureVector.get(uneditedVMod)+1);
			} else if (rel1[i].startsWith("nmod")) {
				featureVector.put(uneditedNMod, featureVector.get(uneditedNMod)+1);
			} else if (rel1[i].startsWith("pmod")) {
				featureVector.put(uneditedPMod, featureVector.get(uneditedPMod)+1);
			}

			if (rel2[j].startsWith("sub")) {
				featureVector.put(uneditedSubj, featureVector.get(uneditedSubj)+1);
			} else if (rel2[j].startsWith("obj")) {
				featureVector.put(uneditedObj, featureVector.get(uneditedObj)+1);
			} else if (rel2[j].startsWith("vmod")) {
				featureVector.put(uneditedVMod, featureVector.get(uneditedVMod)+1);
			} else if (rel2[j].startsWith("nmod")) {
				featureVector.put(uneditedNMod, featureVector.get(uneditedNMod)+1);
			} else if (rel2[j].startsWith("pmod")) {
				featureVector.put(uneditedPMod, featureVector.get(uneditedPMod)+1);
			} 
		}
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			if (switches.get(f))
				values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);
	}

}
