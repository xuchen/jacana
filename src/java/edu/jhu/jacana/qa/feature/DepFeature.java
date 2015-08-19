/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import java.util.ArrayList;
import java.util.HashMap;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class DepFeature extends AbstractFeatureExtractor {

	public DepFeature() {
		super("dep");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		String[] features = new String[aTree.getDependencies().size()];
		
		for (int i=0; i<features.length; i++) {
			features[i] = String.format("%s[%d]=%s", featureName, 0, aTree.getDependencies().get(i).reln().toString());
		}
		return features;
	}
	
	// for a template of {1, 0, -1}, assume that 1 means parent node, 0 means current node, 
	// then -1 is child node: however a node can have multiple children. Thus for each child,
	// we compute a feature for dep[1]|dep[0]|dep[-1] while dep[-1] is one of the children
	// it could be that there are multiple identical dep[-1], so we use feature count.
	@Override
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template) {
		if (template == null) return extractSingleFeature(dist, qTree, aTree);
		final String BOT = "root", EOT = "leaf";
		String[] features = new String[aTree.getDependencies().size()];
		
		String feature;
		
		for (int i=0; i<features.length; i++) {
			StringBuilder sb = new StringBuilder();
			HashMap<String, Integer> feature2count = new HashMap<String, Integer>();
			for (int j=0; j<template.length; j++) {
				ArrayList<String> leftFeature = new ArrayList<String>();
				ArrayList<String> rightFeature = new ArrayList<String>();
				boolean getChildren = false;
				for (int k=0; k<template[j].length; k++) {
					int level = template[j][k];
					
					if (level == 0) {
						leftFeature.add(String.format("%s[%d]", featureName, level));
						rightFeature.add(aTree.getDependencies().get(i).reln().toString());
					} else if (level > 0) {
						leftFeature.add(String.format("%s[%d]", featureName, level));
						// look for parent
						Tree gov = aTree.getTree().get(i).parent();
						while (gov != null && level > 1) {
							gov = gov.parent();
							level--;
						}
						if (gov != null) {
							int govIdx = aTree.getIdxOfNode((TreeGraphNode)gov);
							rightFeature.add(aTree.getDependencies().get(govIdx).reln().toString());
						} else {
							rightFeature.add(BOT);
						}

					} else if (level == -1) {
						// only deal with one level of children
						getChildren = true;
					}
				}
//				if (getChildren) {
//					leftFeature.add(String.format("%s[-1]", featureName));
//				}
				String left = StringUtils.join(leftFeature, "|");
				String right = StringUtils.join(rightFeature, "|");
				if (getChildren) {
					ArrayList<String> newLeftFeature = new ArrayList<String>(leftFeature);
					newLeftFeature.add(String.format("%s[-1]", featureName));
					if (!aTree.getTree().get(i).isLeaf()) {
						for (TreeGraphNode child: aTree.getTree().get(i).children()) {
							int childIdx = aTree.getIdxOfNode(child);
							String childDep = aTree.getDependencies().get(childIdx).reln().toString();
							ArrayList<String> newRightFeature = new ArrayList<String>(rightFeature);
							newRightFeature.add(childDep);
							feature = StringUtils.join(newLeftFeature, "|") +"="+ StringUtils.join(newRightFeature, "|");
							if (!feature2count.containsKey(feature))
								feature2count.put(feature, 0);
							feature2count.put(feature, feature2count.get(feature)+1);
						}
					} else {
						ArrayList<String> newRightFeature = new ArrayList<String>(rightFeature);
						newRightFeature.add(EOT);
						feature = StringUtils.join(newLeftFeature, "|") +"="+ StringUtils.join(newRightFeature, "|");
						if (!feature2count.containsKey(feature))
							feature2count.put(feature, 0);
						feature2count.put(feature, feature2count.get(feature)+1);
					}
				} else {
					feature = left+"="+right;
					if (!feature2count.containsKey(feature))
						feature2count.put(feature, 0);
					feature2count.put(feature, feature2count.get(feature)+1);
				}
			}
			for (String f:feature2count.keySet()) {
				sb.append(String.format("%s:%d\t", f, feature2count.get(f)));
			}
			features[i] = sb.toString().trim();
		}
		return features;
	}

}
