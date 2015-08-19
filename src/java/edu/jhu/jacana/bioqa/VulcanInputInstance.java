/**
 * 
 */
package edu.jhu.jacana.bioqa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.jacana.nlp.LingPipeAuraNER;
import edu.jhu.jacana.nlp.StanfordCore;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

/**
 * @author Xuchen Yao
 *
 */
public class VulcanInputInstance {
	
	public class AnalyzedInstance {
		
		public String word_line, pos_line, lab_line, deps_line, ner_line, mstString;
		
		public AnalyzedInstance(CoreMap sentence) {
			Pair<String, String> pair;
			pair = StanfordCore.getTokenAndPosInOneLine(sentence);
			word_line = pair.first();
			pos_line = pair.second();
			pair =  StanfordCore.getDepAndIndexInOneLine(sentence);
			lab_line = pair.first();
			deps_line = pair.second();
			ner_line = LingPipeAuraNER.getNERsInOneLine(word_line);
			mstString = word_line+"\n"+pos_line+"\n"+lab_line+"\n"+deps_line+"\n"+ner_line;
		}
		String id, sentence, rank, relevance;
	}
	
	AnalyzedInstance qInstance;
	Set<String> answerSet;
	List<AnalyzedInstance> answerInstances;
	
	public VulcanInputInstance () { 
		answerSet = new HashSet<String>();
		answerInstances = new ArrayList<AnalyzedInstance>();
	}
	
	public VulcanInputInstance (CoreMap question) {
		qInstance = new AnalyzedInstance(question);
		answerSet = new HashSet<String>();
		answerInstances = new ArrayList<AnalyzedInstance>();
	}
}
