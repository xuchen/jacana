/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;

/**
 * 
 * @author Xuchen Yao
 *
 */
@Deprecated
public class ModifierOfAlignmentFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public ModifierOfAlignmentFeature() {
		super("child_of_alignment");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		int size = aTree.getSize();
		String[] features = new String[size];
		ArrayList<Integer> alignIndex = new ArrayList<Integer>();
		ArrayList<String> featList = new ArrayList<String>(); 
		
		String qWord = QuestionWordExtractor.getQuestionWords(qTree);
		// get all alignment index for non stopwords
		for (Integer id:dist.getAlign1to2().keySet()) {
			//if (!EditDist.stopWordPosTags.contains(dist.getPos1()[id])) {
				alignIndex.add(dist.id2idxInWordOrder1(id));
			//}
		}
		
		// isn't necessary
		Integer[] sortedAlignIndex = alignIndex.toArray(new Integer[alignIndex.size()]);
		Arrays.sort(sortedAlignIndex);
		
		for (int i=0; i<size; i++) {
			TreeGraphNode child = aTree.getTree().get(i);
			TreeGraphNode parent = (TreeGraphNode) child.parent();
			
			if (parent == null || aTree.getRoot() == parent || !alignIndex.contains(aTree.getIdxOfNode(parent))) {
				features[i] = "";
				continue;
			}
			int parentIdx = aTree.getIdxOfNode(parent);
			StringBuilder sb = new StringBuilder();
			sb.append(this.featureName+"_"+qWord+"=yes");
			
			sb.append(String.format("\t%s_child_pos_%s=%s", this.featureName, qWord, aTree.getLabels().get(i).tag()));
			sb.append(String.format("\t%s_child_dep_%s=%s", this.featureName, qWord, aTree.getDependencies().get(i).reln()));
			
			sb.append(String.format("\t%s_parent_pos_%s=%s", this.featureName, qWord, aTree.getLabels().get(parentIdx).tag()));
			sb.append(String.format("\t%s_parent_dep_%s=%s", this.featureName, qWord, aTree.getDependencies().get(parentIdx).reln()));
			features[i] = sb.toString();			
		}
		
		return features;
	}

}
