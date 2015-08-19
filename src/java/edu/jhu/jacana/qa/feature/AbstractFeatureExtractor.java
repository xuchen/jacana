/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.qa.feature.template.TemplateExpander;

/**
 * AbstractFeatureExtractor for QA was initially designed to work on edit sequences. However, a first
 * try with only EditTypeFeature gave some snopshot of the sequences:
 * 
O delLeaf
O delLeaf
O delLeaf
ANSWER-I del
ANSWER-B delLeaf
O insLeaf
O delLeaf
 * In the above, ANSWER-B is right behind ANSWER-I. Usually this is the case where ANSWER-B is a delLeaf edit.
 * Sometimes, we see:
 *
O delLeaf 
O del 
ANSWER-B del 
ANSWER-I del 
ANSWER-I delLeaf 
ANSWER-I delLeaf 
O delLeaf 
O delLeaf 
 *
 * ANSWER-B is in front of ANSWER-I. Usually this is the case where ANSWER-B is a del edit. 
 * What's more strange is something like the following:
O insLeaf 
O insLeaf 
ANSWER-I del 
O delLeaf 
O ins 
O insLeaf 
O ins 
O ins 
O insLeaf 
O renRel 
ANSWER-B del 
O delLeaf 
O del
 * There are other operations in between ANSWER-I and ANSWER-B. i.e., even that there shouldn't be
 * anything in between in word order, it still happened that in the post-order traversal various
 * other operations can happen. This is a very hard case to model (we have to make sure when we tag
 * ANSWER-B and ANSWER-I in edit sequences, the tagging has a hard constraint that the words being
 * tagged must be in adjacent order).
 * 
 * To replicate the above, check out SVN revision 63.
 * 
 *  Thus instead of building a MEMM/CRF on edit sequence in post-order, let's build a model on the 
 *  original sequence, and use the edit sequence as extra features (such as that answers can ONLY
 *  appear in deleted or renamed information, etc).

 * @author Xuchen Yao
 *
 */
public abstract class AbstractFeatureExtractor {
	String featureName;
	
	public AbstractFeatureExtractor(String featureName) {
		this.featureName = featureName;
	}
	
	public abstract String[] extractSingleFeature(EditDist dist, DependencyTree qTree, DependencyTree aTree);
	
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template, String[] appends) {
		return extract(dist, qTree, aTree, template, appends, null);
	}
	
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template, String[] appends, String[] tokenWiseAppends) {
		String[] expanded;
		if (template == null) 
			expanded = extractSingleFeature(dist, qTree, aTree);
		else
			expanded = TemplateExpander.expand(extractSingleFeature(dist, qTree, aTree), featureName, template);
		if (appends == null || appends.length == 0)
			return expanded;
		String[] appended = new String[expanded.length];

		for (int i=0; i<expanded.length; i++) {
			StringBuilder sb = new StringBuilder(expanded[i]);
			for (String e:expanded[i].split("\t")) {
				for (String append:appends)
					sb.append(String.format("\t%s|%s", append, e));
				if (tokenWiseAppends !=null && tokenWiseAppends[i].length()>0) {
					for (String append:tokenWiseAppends[i].split("\t"))
						sb.append(String.format("\t%s|%s", e, append));
				}
			}
			appended[i] = sb.toString();
		}
		return appended;
	}
	
	public String[] extract(EditDist dist, DependencyTree qTree, DependencyTree aTree, int[][] template) {
		return extract(dist, qTree, aTree, template, null);
	}

}
