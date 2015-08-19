/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import approxlib.distance.Edit;
import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.stanford.nlp.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class EditFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public EditFeature() {
		super("edit");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		ArrayList<Edit> editList = dist.getEditList();
		//System.out.println(dist.printHumaneEditScript());
		String[] features = new String[aTree.getSize()];
		Arrays.fill(features, null);
		
		// stores DEL/DEL_LEAF
		HashSet<Integer> delIndexFromEditList = new HashSet<Integer>();
		// stores DEL/DEAL_LEAF/DEAL_SUBTREE
		HashSet<Integer> delIndexFromCompactEditList = new HashSet<Integer>();
		for (Edit edit:editList) {
			String editType = edit.getType().toString();
			int aId, aIdx=-1, qId, qIdx;
			ArrayList<String> featList = new ArrayList<String>();
			aId = edit.getArgs()[0];
			switch (edit.getType()) {
			case DEL:
			case DEL_SUBTREE:
			case DEL_LEAF:

				aIdx = dist.id2idxInWordOrder1(aId);
				if (aIdx == -1) continue;
				delIndexFromEditList.add(aIdx);
				featList.add("edit="+editType);
				featList.add(String.format("edit_%s_pos=%s", editType, aTree.getLabels().get(aIdx).tag()));
				featList.add(String.format("edit_%s_dep=%s", editType, aTree.getDependencies().get(aIdx).reln()));
				featList.add(String.format("edit_%s_ner=%s", editType, aTree.getEntities().get(aIdx)));
				break;
				
				// superceded by align feature 'cause in EditDist renaming==align
				// this is a bit different than paper description, but it is the 
				// same thing, just with different names
//			case REN_REL:
//			case REN_POS:
//			case REN_POS_REL:
//
//				aIdx = dist.id2idxInWordOrder1(aId);
//				if (aIdx == -1) continue;
//				qId = edit.getArgs()[1];
//				qIdx = dist.id2idxInWordOrder2(qId);
//				if (qIdx == -1) continue;
//				featList.add(String.format("edit_%s_from_pos=%s", editType, aTree.getLabels().get(aIdx).tag()));
//				featList.add(String.format("edit_%s_to_pos=%s", editType, qTree.getLabels().get(qIdx).tag()));
//				featList.add(String.format("edit_%s_from_to_pos=%s_%s", editType, aTree.getLabels().get(aIdx).tag(), qTree.getLabels().get(qIdx).tag()));
//
//				featList.add(String.format("edit_%s_from_dep=%s", editType, aTree.getDependencies().get(aIdx).reln()));
//				featList.add(String.format("edit_%s_to_dep=%s", editType, qTree.getDependencies().get(qIdx).reln()));
//				featList.add(String.format("edit_%s_from_to_dep=%s_%s=", editType, qTree.getDependencies().get(qIdx).reln(), aTree.getDependencies().get(aIdx).reln()));
//				
//				featList.add(String.format("edit_%s_from_ner=%s", editType, aTree.getEntities().get(aIdx)));
//				featList.add(String.format("edit_%s_to_ner=%s", editType, qTree.getEntities().get(qIdx)));
//				featList.add(String.format("edit_%s_from_to_ner=%s_%s", editType, aTree.getEntities().get(aIdx), qTree.getEntities().get(qIdx)));
//				featList.add("edit="+editType);
//				break;
			default:
				break;	
			}
			if (aIdx != -1)
				features[aIdx] = StringUtils.join(featList, "\t");
		}
		HashMap<Integer, Integer> align1to2 = dist.getAlign1to2();
		for (Integer aId:align1to2.keySet()) {
			ArrayList<String> featList = new ArrayList<String>();
			Integer qId = align1to2.get(aId);
			int aIdx = dist.id2idxInWordOrder1(aId);
			int qIdx = dist.id2idxInWordOrder2(qId);
			if (aIdx == -1 || qIdx == -1) continue;
			featList.add("edit=align");
			featList.add("edit_align_from_pos="+aTree.getLabels().get(aIdx).tag());
			featList.add("edit_align_to_pos="+qTree.getLabels().get(qIdx).tag());
			featList.add("edit_align_from_to_pos="+aTree.getLabels().get(aIdx).tag()+"_"+qTree.getLabels().get(qIdx).tag());

			featList.add("edit_align_from_dep="+aTree.getDependencies().get(aIdx).reln());
			featList.add("edit_align_to_dep="+qTree.getDependencies().get(qIdx).reln());
			featList.add("edit_align_from_to_dep="+qTree.getDependencies().get(qIdx).reln()+"_"+aTree.getDependencies().get(aIdx).reln());
			
			featList.add("edit_align_from_ner="+aTree.getEntities().get(aIdx));
			featList.add("edit_align_to_ner="+qTree.getEntities().get(qIdx));
			featList.add("edit_align_from_to_ner="+aTree.getEntities().get(aIdx)+"_"+qTree.getEntities().get(qIdx));
			features[aIdx] = StringUtils.join(featList, "\t");
		}
		for (int i=0; i<features.length; i++) {
			if (features[i] == null) {
				// if it's not deleted or renamed, then this word must align with another word in the question.
				features[i] = "edit=align";
			}
		}
		
		// add one feature that whether this deleted information is also in a subtree
//		for (Edit edit:dist.getCompactEditList()) {
//			switch (edit.getType()) {
//			case DEL:
//			case DEL_LEAF:
//				int aId = edit.getArgs()[0];
//				int aIdx = dist.id2idxInWordOrder1(aId);
//				if (aIdx == -1) continue;
//				delIndexFromCompactEditList.add(aIdx);
//				break;
//			default:
//				break;
//			}
//		}
//		
//		for (int idx:delIndexFromEditList) {
//			if (!delIndexFromCompactEditList.contains(idx))
//				features[idx] += "\t"+"in_del_subtree=true";
//		}
		return features;
	}

}
