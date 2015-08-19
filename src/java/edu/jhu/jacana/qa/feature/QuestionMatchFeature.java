package edu.jhu.jacana.qa.feature;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;


@Deprecated
public class QuestionMatchFeature {
	
	public static String[] extract(String[][] features, DependencyTree qTree) {
		String[] extracted = new String[features[0].length];
		String q = QuestionWordExtractor.getQuestionWords(qTree);
		for (int i=0; i<features[0].length; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j=0; j<features.length; j++) {
				for (String f:features[j][i].split("\t"))
					sb.append(String.format("qword=%s|%s\t", q, f));
			}
			extracted[i] = sb.toString().trim();
		}
		return extracted;
	}

}
