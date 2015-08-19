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
 * renaming happens between different relation changes
 * @author Xuchen Yao
 *
 */
public class RenameRelFeature extends NormalizedFeatureExtractor {
	
	
	protected final static String renSubj="renSubject", renObj="renObject", renRoot="renRoot", 
			renVMod="renVMod", renNMod="renNMod", renPMod="renPMod";
	
	public RenameRelFeature(boolean normalized) {
		super(normalized);
		features = new String[]{renSubj, renObj, renRoot, renVMod, renNMod, renPMod};
		switches = new HashMap<String, Boolean>() {
			{	put(renSubj, true);
				put(renObj, true);
				put(renRoot, true);
				put(renVMod, true);
				put(renNMod, false);
				put(renPMod, false);
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
		String rel1, rel2;
		
		ArrayList<Edit> editList = dist.getCompactEditList();
		for (Edit e:editList) {
			if (e.getType() == Edit.TYPE.REN_REL || e.getType() == Edit.TYPE.REN_POS_REL) {
				rel1 = dist.getRel1()[e.getArgs()[0]];
				rel2 = dist.getRel2()[e.getArgs()[1]];
				if (rel1.contains("sub") || rel2.contains("sub")) {
					featureVector.put(renSubj, featureVector.get(renSubj)+1);
				} if (rel1.endsWith("obj") || rel2.endsWith("obj")) {
					featureVector.put(renObj, featureVector.get(renObj)+1);
				} if (rel1.endsWith("root") || rel2.endsWith("root")) {
					featureVector.put(renRoot, featureVector.get(renRoot)+1);
				} if (rel1.startsWith("vmod") || rel2.endsWith("vmod")) {
					featureVector.put(renVMod, featureVector.get(renVMod)+1);
				} if (rel1.startsWith("nmod") || rel2.endsWith("nmod")) {
					featureVector.put(renNMod, featureVector.get(renNMod)+1);
				} if (rel1.startsWith("pmod") || rel2.endsWith("pmod")) {
					featureVector.put(renPMod, featureVector.get(renPMod)+1);
				}
			}
		}
		
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			if (switches.get(f))
				values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);
	}

}
