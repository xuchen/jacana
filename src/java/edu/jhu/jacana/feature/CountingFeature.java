/**
 * 
 */
package edu.jhu.jacana.feature;

import java.util.ArrayList;
import java.util.Arrays;

import approxlib.distance.Edit;
import approxlib.distance.Edit.TYPE;
import approxlib.distance.EditDist;

/**
 * @author Xuchen Yao
 *
 */
public class CountingFeature extends NormalizedFeatureExtractor {
	
	boolean useLeafFeature;

	/**
	 * adding the leaf feature will lead a small decrease (about 1%) in MAP and MRR
	 * we don't know for sure why this happens as the leaf feature linguistically makes sense
	 * (leaves are usually modifiers and thus not as important as their parents)
	 * but still give users the option to use it or not by the boolean switch.
	 * i.e., setting it to false gives slightly better scores.
	 * Default: true
	 * @param useLeafFeature
	 */
	public CountingFeature(boolean useLeafFeature, boolean normalized) {
		this.useLeafFeature = useLeafFeature;
		this.normalized = normalized;
	}
	
	public CountingFeature() {
		this(true, false);
	}
	
	public CountingFeature(boolean normalized) {
		this(true, normalized);
		ArrayList<String> fs = new ArrayList<String>();
		final String[] originalEdits = {"insOrig", "delOrig", "renOrig"};
		for (TYPE t:Edit.TYPE.values()) {
			// NONE operation
			if (t.toString().length() == 0) continue;
			if ((t == TYPE.INS_LEAF || t == TYPE.DEL_LEAF) && !useLeafFeature) continue;
			fs.add(t.toString());
		}
		for (String s:originalEdits) {
			fs.add(s);
		}
		features = fs.<String>toArray(new String[fs.size()]);
	}
	

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.FeatureExtractor#getFeatureValues(approxlib.distance.EditDist)
	 */
	public Double[] getFeatureValues(EditDist dist) {
		int[] type2num = null;
		type2num = new int[Edit.TYPE.values().length];
		double allNodes;
		if (normalized)
			allNodes = dist.getSize()*1.0;
		else
			allNodes = 1.0;
		
		for (TYPE t:Edit.TYPE.values()) {
			type2num[t.ordinal()] = 0;
		}
		
		ArrayList<Edit> editList = dist.getCompactEditList();
		for (Edit e:editList) {
			type2num[e.getType().ordinal()] += 1;
		}

		if (!useLeafFeature) {
			type2num[Edit.TYPE.INS.ordinal()] += type2num[Edit.TYPE.INS_LEAF.ordinal()];
			type2num[Edit.TYPE.INS_LEAF.ordinal()] = 0;
			type2num[Edit.TYPE.DEL.ordinal()] += type2num[Edit.TYPE.DEL_LEAF.ordinal()];
			type2num[Edit.TYPE.DEL_LEAF.ordinal()] = 0;
		}
		StringBuilder sb = new StringBuilder();
		
		ArrayList<Double> values = new ArrayList<Double>();
		
		for (int i=1; i<type2num.length; i++) {
			if ((i == TYPE.INS_LEAF.ordinal() || i == TYPE.DEL_LEAF.ordinal()) && !useLeafFeature) continue;
			values.add(type2num[i]/allNodes);
		}
		
		int[] num = new int[3];
		Arrays.fill(num, 0);
		for (Edit e:dist.getEditList()) {
			switch (e.getType()) {
			case INS:
			case INS_LEAF:
				num[0] += 1; break;
			case DEL: 
			case DEL_LEAF:
				num[1] += 1; break;
			case REN_POS:
			case REN_REL:
			case REN_POS_REL:
				num[2] += 1; break;
			default: break;
			}
		}
		
		for (int c:num)
			values.add(c/allNodes);

		return values.toArray(new Double[values.size()]);
	}

}
