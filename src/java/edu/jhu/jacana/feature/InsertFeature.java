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
public class InsertFeature extends NormalizedFeatureExtractor {

	protected final static String insVerb="insVerb", 
			insNoun="insNoun", 
			insPunc="insPunc",
			insDet="insDet",
			insOtherPos="insOtherPos",
			// end of POS related
			
			// relation related:
			insNMod="insNMod",
			insVMod="insVMod", 
			insPMod="insPMod", 
			insSub="insSubect", 
			insObj="insObject",
			insOtherRel="insOtherRel";
	
	/**
	 * @param normalized
	 */
	public InsertFeature(boolean normalized) {
		super(normalized);
		features = new String[]{insVerb, 
				insNoun, 
				insPunc,
				insDet, 
				insOtherPos,
				// end of POS related
				
				// relation related
				insNMod,
				insVMod, 
				insPMod, 
				insSub, 
				insObj,
				insOtherRel
				};

		switches = new HashMap<String, Boolean>() {
			{	put(insVerb, true);
				put(insNoun, true);
				put(insPunc, true);
				put(insDet, true);
				put(insOtherPos, true);
				// end of POS related
				
				// relation related
				put(insNMod, true);
				put(insVMod,  true);
				put(insPMod,  true);
				put(insSub,  true);
				put(insObj, true);
				put(insOtherRel, true);
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
			case INS:
			case INS_LEAF:
			case INS_SUBTREE:
				// insertion is inserting from target tree (tree2) 
				pos = dist.getPos2()[e.getArgs()[0]];
				rel = dist.getRel2()[e.getArgs()[0]];
				if (pos.startsWith("nn"))
					featureVector.put(insNoun, featureVector.get(insNoun)+1);
				else if (pos.startsWith("vb"))
					featureVector.put(insVerb, featureVector.get(insVerb)+1);
				else if (pos.startsWith("dt"))
					featureVector.put(insDet, featureVector.get(insDet)+1);
				else if (pos.matches(".*\\W+.*"))
					featureVector.put(insPunc, featureVector.get(insPunc)+1);
				else
					featureVector.put(insOtherPos, featureVector.get(insOtherPos)+1);
				
				if (rel.startsWith("nmod") || rel.startsWith("amod"))
					featureVector.put(insNMod, featureVector.get(insNMod)+1);
				else if (rel.startsWith("pmod") || rel.startsWith("prep"))
					featureVector.put(insPMod, featureVector.get(insPMod)+1);
				else if (rel.startsWith("vmod") || rel.startsWith("advmod"))
					featureVector.put(insVMod, featureVector.get(insVMod)+1);
				// the MST parser uses 'sub' and 'obj' while Stanford parser uses 'nsubj' and 'nobj'
				else if (rel.contains("sub"))
					featureVector.put(insSub, featureVector.get(insSub)+1);
				else if (rel.endsWith("obj"))
					featureVector.put(insObj, featureVector.get(insObj)+1);
				else
					featureVector.put(insOtherRel, featureVector.get(insOtherRel)+1);

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
