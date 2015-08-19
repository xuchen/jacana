/**
 * 
 */
package edu.jhu.jacana.feature;

import java.util.ArrayList;
import java.util.HashMap;

import approxlib.distance.Edit;
import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public class DeleteFeature extends NormalizedFeatureExtractor {

	protected final static String delVerb="delVerb", 
			delNoun="delNoun", 
			delPunc="delPunc",
			delDet="delDet",
			delOtherPos="delOtherPos",
			// end of POS related
			
			// relation related:
			delNMod="delNMod",
			delVMod="delVMod", 
			delPMod="delPMod", 
			delSub="delSubect", 
			delObj="delObject",
			delRoot="delRoot",
			delOtherRel="delOtherRel";
	
	
	/**
	 * @param normalized
	 */
	public DeleteFeature(boolean normalized) {
		super(normalized);
		features = new String[]{delVerb, 
				delNoun, 
				delPunc,
				delDet, 
				delOtherPos,
				// end of POS related
				
				// relation related
				delNMod,
				delVMod, 
				delPMod, 
				delSub, 
				delObj,
				delRoot,
				delOtherRel
				};
		switches = new HashMap<String, Boolean>() {
			{	put(delVerb, true);
				put(delNoun, true);
				put(delPunc, true);
				put(delDet, true);
				put(delOtherPos, true);
				// end of POS related
				
				// relation related
				put(delNMod, true);
				put(delVMod,  true);
				put(delPMod,  true);
				put(delSub,  true);
				put(delObj, true);
				put(delRoot, true);
				put(delOtherRel, true);
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
		String pos, rel;
		
		ArrayList<Edit> editList = dist.getCompactEditList();
		for (Edit e:editList) {
			switch (e.getType()) {
			case DEL:
			case DEL_LEAF:
			case DEL_SUBTREE:
				// deletion is deleting from source tree (tree1) 
				pos = dist.getPos1()[e.getArgs()[0]];
				rel = dist.getRel1()[e.getArgs()[0]];
				if (pos.startsWith("nn"))
					featureVector.put(delNoun, featureVector.get(delNoun)+1);
				else if (pos.startsWith("vb"))
					featureVector.put(delVerb, featureVector.get(delVerb)+1);
				else if (pos.startsWith("dt"))
					featureVector.put(delDet, featureVector.get(delDet)+1);
				else if (pos.matches(".*\\W+.*"))
					featureVector.put(delPunc, featureVector.get(delPunc)+1);
				else
					featureVector.put(delOtherPos, featureVector.get(delOtherPos)+1);
				
				if (rel.startsWith("nmod") || rel.startsWith("amod"))
					featureVector.put(delNMod, featureVector.get(delNMod)+1);
				else if (rel.startsWith("pmod") || rel.startsWith("prep"))
					featureVector.put(delPMod, featureVector.get(delPMod)+1);
				else if (rel.startsWith("vmod") || rel.startsWith("advmod"))
					featureVector.put(delVMod, featureVector.get(delVMod)+1);
				// the MST parser uses 'sub' and 'obj' while Stanford parser uses 'nsubj' and 'nobj'
				else if (rel.endsWith("sub") || rel.endsWith("subj"))
					featureVector.put(delSub, featureVector.get(delSub)+1);
				else if (rel.endsWith("obj"))
					featureVector.put(delObj, featureVector.get(delObj)+1);
				else if (rel.endsWith("root"))
					featureVector.put(delRoot, featureVector.get(delRoot)+1);
				else
					featureVector.put(delOtherRel, featureVector.get(delOtherRel)+1);

				break;
			default:
				break;
			}
		}
		
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			if (switches.get(f))
				values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);	
	}

}
