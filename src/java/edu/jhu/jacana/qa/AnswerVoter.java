/**
 * 
 */
package edu.jhu.jacana.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.jhu.jacana.util.MapUtil;
import edu.jhu.jacana.util.StringUtils;
import edu.stanford.nlp.util.Pair;

/**
 * A class for voting the final answer of sentences.
 * @author Xuchen Yao
 *
 */
public class AnswerVoter {
	
	/**
	 * 
	 * @param results list of string returned from by CRFSuiteWrapper.classifyFeatures
	 * @param ref references for each answer sentence
	 * @param uniform whether to take a uniform distribution of the final answer
	 *				 if set to false, then votes are normalized with its marginal prob. 
	 * @param force whether to force an answer when no one is found.
	 *				 this option is useful for TREC QA, but does worse for bio QA.
	 */
	public static Pair<List<Pair<String, Double>>, HashMap<String, Set<Integer>>> getVotes(List<String> results, List<String> ref, boolean uniform, boolean force) {
		/*
		 * sample <code>results</code>:
[@probability	0.565497, O	O:0.997451, O	O:0.998003, O	O:0.988038, O	O:0.993195, O	O:0.997363, 
O	O:0.982164, O	O:0.990316, O	O:0.984853, O	ANSWER-B:0.727661, O	O:0.990379, O	O:0.999409, 
O	O:0.999175, O	O:0.997070, O	O:0.996307, O	O:0.931872, O	O:0.915765, O	O:0.998186, O	O:0.980502, 
O	O:0.998990, O	O:0.999468, O	O:0.992070, O	O:0.977609, O	O:0.984442, O	O:0.999901, , 
@probability	0.819336, O	O:0.999897, O	O:0.999248, O	ANSWER-B:0.630457, O	ANSWER-I:0.577902, 
O	O:0.971727, O	O:0.998406, O	O:0.998584, O	O:0.990599, O	O:0.999645, O	O:0.977548, O	O:0.995301, 
O	O:0.999275, O	O:0.999588, O	O:0.997957, O	O:0.999861, O	O:0.999637, O	O:0.998123, O	O:0.999873, 
O	O:0.959640, O	O:0.997548, O	O:0.995018, O	O:0.996982, O	O:0.980873, O	O:0.976682, O	O:0.999774, , 
@probability	0.445356, O	O:0.999893, O	O:0.947855, O	O:0.956013, O	O:0.996022, O	O:0.985365, ,
Performance by label (#match, #model, #ref) (precision, recall, F1):,     O: (182, 182, 184) (1.0000, 0.9891, 0.9945),     
ANSWER-B: (0, 2, 0) (******, ******, ******),     ANSWER-I: (0, 0, 0) (******, ******, ******), 
Macro-average precision, recall, F1: (0.333333, 0.329710, 0.331512), Item accuracy: 182 / 184 (0.9891), 
Instance accuracy: 3 / 5 (0.6000), Elapsed time: 0.020000 [sec] (250.0 [instance/sec])]
		 *
		 * sample <code>ref</code>:
[what checkpoint of the cell cycle does p53 work at ?, animal, cells, generally, have, built-in, stop, signals, 
that, halt, the, cell, cycle, at, checkpoints, until, overridden, by, go-ahead, signals, ., , 
what checkpoint ......]
		 */
		
		double seqProb = 0.0, tagProb = 0.0;
		// tagProb is the mean of tagProbs, when there are multiple tokens for an answer
		// e.g., ANSWER-B:0.630457, O	ANSWER-I:0.577902,
		ArrayList<Double> tagProbs = new ArrayList<Double>(); 
		ArrayList<String> answers = new ArrayList<String>();
		String[] splits;
		HashMap<String, Double> ans2vote = new HashMap<String, Double>();
		// a mapping between each answer and the index (starting from 0) of sentence it comes from
		HashMap<String, Set<Integer>> ans2sentIdx = new HashMap<String, Set<Integer>>();
		
		int sentIdx = -1;
		boolean found = false;
		for (int i=0; i<results.size(); i++) {
			String s = results.get(i);
			if (s.startsWith("@prob")) {
				seqProb = Double.parseDouble(s.split("\\s+")[1]);
				sentIdx++;
			} else if (s.startsWith("O") || s.startsWith("ANS")) {
				splits = s.split("\\s+");
				if (splits[1].startsWith("ANS")) {
					// ANSWER-B:0.630457
					tagProbs.add(Double.parseDouble(splits[1].split(":")[1]));
					answers.add(ref.get(i));
				} else if (answers.size() > 0) {
					found = true;
				} else {
					found = false;
				}
			} 
			if (found) {
				if (tagProbs.size() > 0) {
					if (uniform) {
						tagProb = 1.0;
					} else {
						double sum = 0.0;
						for (Double d:tagProbs) sum+=d;
						// tag probability is the sequence probability times the average tag probability
						tagProb = seqProb*(sum/tagProbs.size());
					}
					String answer = StringUtils.join(answers, " ");
					if (!ans2vote.containsKey(answer)) {
						ans2vote.put(answer, 0.0);
						ans2sentIdx.put(answer, new HashSet<Integer>());
					}
					ans2vote.put(answer, ans2vote.get(answer)+tagProb);
					ans2sentIdx.get(answer).add(sentIdx);
					tagProbs.clear(); answers.clear();
				}
			} 
			if (s.startsWith("Performance")) {
				break;
			}
		}
		
		ans2vote = partialVote(ans2vote);
		return new Pair<List<Pair<String, Double>>, HashMap<String, Set<Integer>>>(normalizeSortVote(ans2vote), ans2sentIdx);
	}
	
	/**
	 * Given a mapping between answers and votes, return a list of answers with descending votes.
	 */
	protected static List<Pair<String, Double>> normalizeSortVote(HashMap<String, Double> ans2vote) {
		List<Pair<String, Double>> voteList = new ArrayList<Pair<String, Double>>();
		
		double sum = 0.0;
		for (Double d:ans2vote.values()) sum+=d;
		
		Map<String,Double> ans2voteSorted = MapUtil.sortByDescendingValue(ans2vote);
		for (Entry<String,Double> entry:ans2voteSorted.entrySet()) {
			voteList.add(new Pair<String,Double>(entry.getKey(), entry.getValue()/sum));
		}
		return voteList;
	}
	
	/**
	 * partial vote among all answers. the naive algorithm works like this:
     * say, two answers "april 1984" and "1984" with count 1
     * we add each answer #overlap/#total words = 1/3
	 * @param ans2vote
	 * @return
	 */
	protected static HashMap<String, Double> partialVote(HashMap<String, Double> ans2vote) {
		if (ans2vote.size() <= 1) return ans2vote;
		HashMap<String, Double> ans2voteBase = new HashMap<String, Double>();
		for (String ans:ans2vote.keySet())
			ans2voteBase.put(ans, 1.0);
		
		HashSet<String> iSet, jSet;
		String[] answers = ans2vote.keySet().toArray(new String[ans2vote.size()]);
		for (int i=0; i<answers.length-1; i++) {
			for (int j=i+1; j<answers.length; j++) {
				iSet = new HashSet<String>(Arrays.asList(answers[i].split(" ")));
				jSet = new HashSet<String>(Arrays.asList(answers[j].split(" ")));
				iSet.retainAll(jSet);
				double ratio = 1.0*iSet.size()/(iSet.size()+jSet.size());
				ans2voteBase.put(answers[i], ans2voteBase.get(answers[i])+ratio);
				ans2voteBase.put(answers[j], ans2voteBase.get(answers[j])+ratio);
			}
		}
		// scale it with the original distribution
		for (Entry<String,Double> entry:ans2vote.entrySet()) {
			ans2vote.put(entry.getKey(), entry.getValue()*ans2voteBase.get(entry.getKey()));
		}
		return ans2vote;
	}

}
