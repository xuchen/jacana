/**
 * 
 */
package edu.jhu.jacana.qa.questionanalysis;

import java.util.HashSet;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.reader.JeopardyMetaInfoJsonReader;
import edu.jhu.jacana.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class JeopardyClueFeature {

	public static String[] extract(DependencyTree qTree, JeopardyMetaInfoJsonReader meta, String id) {
		HashSet<String> features = new HashSet<String>();
		String focus = meta.getFocus(id).toLowerCase();
		features.add("q-focus-full="+focus.replaceAll(" ", "_"));
		String[] splits = focus.split("\\s+");
		if (splits.length > 1) {
			features.add("q-focus-last="+splits[splits.length-1]);
		}
		int start = meta.getFocusFrom(id);
		int end = meta.getFocusTo(id);

		String ner = StringUtils.join(qTree.getEntities(), "_", start, end);
		features.add("q-focus-full-ner="+ner);
		features.add("q-focus-last-ner="+qTree.getEntities().get(end-1));
		
		String pos = StringUtils.join(qTree.getPosTags(), "_", start, end);
		features.add("q-focus-full-pos="+pos);
		features.add("q-focus-last-pos="+qTree.getPosTags().get(end-1));

		String[] fs = features.toArray(new String[features.size()]);
		
		// : is a special separator for attribute:value in crfsuite
		for (int i=0; i<fs.length; i++)
			fs[i] = fs[i].replaceAll(":", "_");
		return fs;
	}

}
