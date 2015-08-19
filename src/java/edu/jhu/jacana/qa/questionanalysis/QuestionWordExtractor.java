package edu.jhu.jacana.qa.questionanalysis;

import java.util.HashSet;

import edu.jhu.jacana.dependency.DependencyTree;

public class QuestionWordExtractor {
	
	@SuppressWarnings("serial")
	public static HashSet<String> qWords = new HashSet<String> () {
		{add("who"); add("when"); add("what"); add("where"); add("how"); add("which"); add("why"); add("whom"); add("whose");}
	};
		
	public static String getQuestionWords (DependencyTree tree) {
		String q = null;
		String[] words = tree.getSentence().split("\\s+");
		for (int i=0; i<words.length; i++) {
			if (qWords.contains(words[i])) {
				// how often, when, how many, how much, how, how long, how old, how far, how large, how often
				if (words[i].equals("how")){
					if (i+1 < words.length) {
						// look at the POS tag of next word
						String pos = tree.getLabels().get(i+1).tag();
						if (pos.startsWith("vb"))
							q = "how";
						else
							q = "how_" + words[i+1];
						break;
					}
				} else {
					q = words[i];
					// break here in case there's more than one question word
					// (such as, who was the president ... when ...?)
					break;
				}
			}
		}
		if (q == null) {
			// name the first private citizen to fly in space .
			// name a film that has won the golden bear in the berlin film festival ?
			if (words[0].equals("name"))
				q = "name";
		}
		if (q == null) {
			System.err.println("Error: can't find question word in sentence " + tree.getSentence());
			q = "none";
		}
		return q;
	}

}
