/**
 * 
 */
package edu.jhu.jacana.feature;

import java.util.ArrayList;
import java.util.HashMap;

import approxlib.distance.Edit;
import approxlib.distance.EditDist;

/**
 * renaming only happens when lemmas match. This feature extractor captures cases where
 * renaming happens between:
 * different forms of nouns (NN*)
 * different forms of verbs (VB*)
 * for now we think these kinds of operations are not 'critical' since
 * the difference induced might simply be due to tagging errors, or different tense
 * @author Xuchen Yao
 *
 */
public class RenamePosFeature extends NormalizedFeatureExtractor {
		
	protected final static String renNoun="renNoun", renVerb="renVerb", renOther="renOtherPOS";
	
	
	public RenamePosFeature(boolean normalized) {
		super(normalized);
		features = new String[]{renNoun, renVerb, renOther};
		switches = null;
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
		String pos1, pos2;
		
		ArrayList<Edit> editList = dist.getCompactEditList();
		for (Edit e:editList) {
			if (e.getType() == Edit.TYPE.REN_POS || e.getType() == Edit.TYPE.REN_POS_REL) {
				pos1 = dist.getPos1()[e.getArgs()[0]];
				pos2 = dist.getPos2()[e.getArgs()[1]];
				if (pos1.startsWith("nn") && pos2.startsWith("nn")) {
					featureVector.put(renNoun, featureVector.get(renNoun)+1);
				} else if (pos1.startsWith("vb") && pos2.startsWith("vb")) {
					featureVector.put(renVerb, featureVector.get(renVerb)+1);
				} else
					featureVector.put(renOther, featureVector.get(renOther)+1);
			}
		}
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);
	}

}
