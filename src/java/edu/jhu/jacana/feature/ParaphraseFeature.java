/**
 * 
 */
package edu.jhu.jacana.feature;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

import edu.jhu.jacana.util.ArrayUtils;
import edu.jhu.jacana.util.FileManager;
import edu.jhu.jacana.util.StringUtils;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

/**
 * @author Xuchen Yao
 *
 */
public class ParaphraseFeature extends NormalizedFeatureExtractor {

	protected final static String ppCount = "ppCount", ppScore="ppScore",
			ppSub="ppSub", ppObj="ppObj", ppNmod="ppNmod", ppVmod="ppVmod",
			ppPmod="ppPmod", ppMod="ppMod";
	/**
	 * stores a mapping between:
	 * phrase paraphrase -> an array of scores
	 * paraphrase phrase -> the above array (pointer, not copy)
	 * usually the MonoDSscores for "p pp" and "pp p" are the same.
	 * in the case of BiP scores, they can be different, we take
	 * an average here since in quite some tasks (such as paraphrase 
	 * identification) we don't know which is the original phrase
	 * and which is the paraphrase.
	 */
	HashMap<String, double[]> pair2scores;
	HashSet<String> phrases;
	int scoreSize;
	
	/**
	 * if set to false, then only ppCount/ppScore* are enabled.
	 * if set to true, then pp{Sub, Obj, ...} are also used. 
	 */
	boolean usePPclassFeature;


	public ParaphraseFeature(boolean normalized, String ppFile) {
		super(normalized);
		String line;
		pair2scores = new HashMap<String, double[]>();
		phrases = new HashSet<String>();
		BufferedReader in;
		String[] splits;
		String p_pp, pp_p;
		scoreSize = 0;
		double[] scores, oldScores;
		try {
			in = FileManager.getReader(ppFile);
			while ((line = in.readLine()) != null) {
				// drug trafficking    the drug trade  3.715948    0.792000
				splits = line.split("\t");
				if (Double.valueOf(splits[2]) > 5) continue;
				if (Double.valueOf(splits[3]) < 0.3) continue;
				phrases.add(splits[0]);
				phrases.add(splits[1]);
				p_pp = splits[0] + "\t" + splits[1];
				pp_p = splits[1] + "\t" + splits[0];
				if (!pair2scores.containsKey(p_pp) && !pair2scores.containsKey(pp_p)) {
					scoreSize = splits.length-2;
					scores = new double[scoreSize];
					for (int i=0; i<scoreSize; i++)
						scores[i] = Double.valueOf(splits[i+2]);
					pair2scores.put(p_pp, scores);
					pair2scores.put(pp_p, scores);
				} else {
					scoreSize = splits.length-2;
					scores = new double[scoreSize];
					for (int i=0; i<scoreSize; i++)
						scores[i] = Double.valueOf(splits[i+2]);
					oldScores = pair2scores.containsKey(p_pp)?pair2scores.get(p_pp):pair2scores.get(pp_p);
					for (int i=0; i<scoreSize; i++)
						// take an average of the new scores and old scores
						oldScores[i] = 0.5*(oldScores[i] + scores[i]);
				}
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ArrayList<String> featList = new ArrayList<String>();
		
		
		for (int i=0; i<scoreSize; i++)
			featList.add(ppScore+i);
		featList.add(ppSub);
		featList.add(ppObj);
		featList.add(ppNmod);
		featList.add(ppVmod);
		featList.add(ppPmod);
		featList.add(ppMod);
		featList.add(ppCount);
		features = featList.toArray(new String[featList.size()]);
		switches = new HashMap<String, Boolean>() {
			{
				for (int i=0; i<scoreSize; i++)
					put(ppScore+i, true);
				put(ppCount, true);
				
				// if enable any of the following, then also enable usePPclassFeature
				put(ppSub, false);
				put(ppObj, false);
				put(ppNmod, false);
				put(ppVmod, false);
				put(ppPmod, false);
				put(ppMod, false);
			}
		};
		usePPclassFeature = false;
		for (int i=1+scoreSize; i<features.length; i++)
			if (switches.get(features[i])) {
				usePPclassFeature = true;
				break;
			}
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.feature.NormalizedFeatureExtractor#getFeatureValues(approxlib.distance.EditDist)
	 * 
	 * the original paraphrase counting feature would over-count overlapping paraphrases (say, 
	 * the doc <-> the report, doc <-> report, then it would count twice). Changed it to longest greedy match
	 */
	@Override
	public Double[] getFeatureValues(EditDist dist) {
		HashMap<String, Double> featureVector = new HashMap<String, Double> ();
		for (String f:features)
			featureVector.put(f, 0.0);

		double allNodes;
		if (normalized)
			allNodes = dist.getSize()*1.0;
		else
			allNodes = 1.0;
		
		String[] splits1 = dist.getSentence1().split("\\s+");
		String[] splits2 = dist.getSentence2().split("\\s+");
		final int upTo = 4;
		int[][] index1 = ArrayUtils.getWindowedSlicesIndex(splits1, upTo, phrases);
		int[][] index2 = ArrayUtils.getWindowedSlicesIndex(splits2, upTo, phrases);
		boolean[] flag1 = new boolean[splits1.length];
		boolean[] flag2 = new boolean[splits2.length];
		Arrays.fill(flag1, false);
		Arrays.fill(flag2, false);
		String p_pp;
		double[] scores;
		String s1, s2;
		boolean clean = true;
		
		String[][] span2rel1 = null, span2rel2 = null;
		
		if (usePPclassFeature) {
			span2rel1 = getSpan2DepRel(dist.getTree1(), upTo);
			span2rel2 = getSpan2DepRel(dist.getTree2(), upTo);
		}

		// try to match the longest ones
		// index1/2 are sorted by window length, so look from back to front
		for (int i=index1.length-1; i>=0; i--) {
			clean = true;
			for (int m=index1[i][0]; m<index1[i][1]; m++)
				if (flag1[m]) { clean = false; break; }
			if (!clean) continue;
			s1 = StringUtils.joinWithSpaces(Arrays.<String>copyOfRange(splits1, index1[i][0], index1[i][1]));
			for (int j=index2.length-1; j>=0; j--) {
				clean = true;
				for (int m=index2[j][0]; m<index2[j][1]; m++)
					if (flag2[m]) { clean = false; break; }
				if (!clean) continue;
				s2 = StringUtils.joinWithSpaces(Arrays.<String>copyOfRange(splits2, index2[j][0], index2[j][1]));
				p_pp = s1+"\t"+s2;
				if (pair2scores.containsKey(p_pp)) {
					
					// the following analysis makes sense, but doesn't increase F1, so commented...
//					if (!(index1[i][1] - index1[i][0] == 1 && index2[j][1] - index2[j][0] == 1) && 
//							(s1.contains(s2) || s2.contains(s1)))
//						// we see a lot of pairs such as history<->in history, of this issue<->this issue
//						// we don't want to count them as paraphrases.
//						// however, if both phrases are single words, such as american<->americans, we count them.
//						continue;

					for (int m=index1[i][0]; m<index1[i][1]; m++)
						flag1[m] = true;
					for (int m=index2[j][0]; m<index2[j][1]; m++)
						flag2[m] = true;

					// count can be 1: the number of paraphrase substitutions.
					// or it can be the word counts of phrase+paraphrase.
					// experiment shows that both lead to a very similar results (0.1% diff in recall)
					//int count = index1[i][1] - index1[i][0] + index2[j][1] - index2[j][0];
					int count = 1;
					
					featureVector.put(ppCount, featureVector.get(ppCount)+count);
					scores = pair2scores.get(p_pp);
					for (int k=0; k<scoreSize; k++) {
						featureVector.put(ppScore+k, featureVector.get(ppScore+k)+scores[k]);
					}
					
					if (usePPclassFeature) {
						if (span2rel1[index1[i][0]][index1[i][1]] != null && span2rel2[index2[j][0]][index2[j][1]] != null
								/*&& span2rel1[index1[i][0]][index1[i][1]].equals(span2rel2[index2[j][0]][index2[j][1]])*/) {
							String rel = span2rel1[index1[i][0]][index1[i][1]];
							String rel2 = span2rel2[index2[j][0]][index2[j][1]];
							
							boolean debug = false;
							if (debug) {
								String p1="", p2="";
								for (int n=index1[i][0]; n<index1[i][1]; n++)
									p1 += splits1[n]+ " ";
								for (int n=index2[j][0]; n<index2[j][1]; n++)
									p2 += splits2[n] + " ";
								System.out.println(p1+"/"+rel+"\t"+p2+"/"+rel2);
							}
							
							if (span2rel1[index1[i][0]][index1[i][1]].equals(span2rel2[index2[j][0]][index2[j][1]])) {
								if (rel.startsWith("sub")) {
									featureVector.put(ppSub, featureVector.get(ppSub)+count);
								} else if (rel.startsWith("obj")) {
									featureVector.put(ppObj, featureVector.get(ppObj)+count);
								} else if (rel.endsWith("nmod")) {
									featureVector.put(ppNmod, featureVector.get(ppNmod)+count);
								} else if (rel.endsWith("vmod")) {
									featureVector.put(ppVmod, featureVector.get(ppVmod)+count);
								} else if (rel.endsWith("pmod")) {
									featureVector.put(ppPmod, featureVector.get(ppPmod)+count);
								}
							} else {
								if (rel.endsWith("mod") && rel2.endsWith("mod"))
									featureVector.put(ppMod, featureVector.get(ppMod)+count);
							}

						}
					}
				}
			}
		}
		
		double count = featureVector.get(ppCount);
		// average scores among all counts of pairs
		for (int i=0; i<scoreSize; i++) {
			featureVector.put(ppScore+i, featureVector.get(ppScore+i)/count);
		}
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			if (switches.get(f))
				values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);

	}
	
	/**
	 * Given a tree/sentence such as "john harper likes mary queen", returns a mapping
	 * between the spans of the relation and the relation name. for instance,
	 * [0][2] -> sub; [2][3] -> root, [3][5] -> obj. the length of the span is subject
	 * to <code>upTo</code>
	 * @param tree
	 * @param upTo
	 * @return a mapping of [start][end] (starting position included, end position excluded) and dependency relation name
	 */
	private static String[][] getSpan2DepRel (LblTree tree, int upTo) {
		String[][] span2rel = new String[tree.getNodeCount()+1][tree.getNodeCount()+1];
		for (int i=0; i<span2rel.length; i++)
			Arrays.fill(span2rel[i], null);
		LblTree node;
		Enumeration<LblTree> e = tree.breadthFirstEnumeration();
		ArrayList<Integer> spanList = new ArrayList<Integer>(); 
		while (e.hasMoreElements()) {
			node = e.nextElement();
			spanList.clear();
			for (Enumeration<LblTree> f=node.postorderEnumeration(); f.hasMoreElements();){
				spanList.add(f.nextElement().getIdxInWordOrder());
			}
			if (spanList.size() <= upTo) {
				int idx = spanList.indexOf(-1);
				// in case the fake root gets in.
				if (idx != -1) spanList.remove(idx);
				Integer[] spans = spanList.toArray(new Integer[spanList.size()]);
				Arrays.sort(spans);
				if (spans[spans.length-1] - spans[0] +1 != spanList.size()) {
					System.err.println("Error: span "+ spans.toString() + " doesn't cover exactly size "+spanList.size());
					System.err.println("Check your code in ParaphraseFeature.java!");
				}
				String rel = node.getLabel().substring(node.getLabel().lastIndexOf("/")+1);
				span2rel[spans[0]][spans[spans.length-1]+1] = rel;
			}
		}
		return span2rel;
	}

//	longest greedy match method using ONLY ppCount and ppScore* as features.
//	public Double[] getFeatureValues(EditDist dist) {
//		HashMap<String, Double> featureVector = new HashMap<String, Double> ();
//		for (String f:features)
//			featureVector.put(f, 0.0);
//
//		double allNodes;
//		if (normalized)
//			allNodes = dist.getSize()*1.0;
//		else
//			allNodes = 1.0;
//		
//		String[] splits1 = dist.getSentence1().split("\\s+");
//		String[] splits2 = dist.getSentence2().split("\\s+");
//		int upTo = 4;
//		int[][] index1 = ArrayUtils.getWindowedSlicesIndex(splits1, upTo, phrases);
//		int[][] index2 = ArrayUtils.getWindowedSlicesIndex(splits2, upTo, phrases);
//		boolean[] flag1 = new boolean[splits1.length];
//		boolean[] flag2 = new boolean[splits2.length];
//		Arrays.fill(flag1, false);
//		Arrays.fill(flag2, false);
//		String p_pp;
//		double[] scores;
//		String s1, s2;
//		boolean clean = true;
//
//		// try to match the longest ones
//		// index1/2 are sorted by window length, so look from back to front
//		for (int i=index1.length-1; i>=0; i--) {
//			clean = true;
//			for (int m=index1[i][0]; m<index1[i][1]; m++)
//				if (flag1[m]) { clean = false; break; }
//			if (!clean) continue;
//			s1 = StringUtils.joinWithSpaces(Arrays.<String>copyOfRange(splits1, index1[i][0], index1[i][1]));
//			for (int j=index2.length-1; j>=0; j--) {
//				clean = true;
//				for (int m=index2[j][0]; m<index2[j][1]; m++)
//					if (flag2[m]) { clean = false; break; }
//				if (!clean) continue;
//				s2 = StringUtils.joinWithSpaces(Arrays.<String>copyOfRange(splits2, index2[j][0], index2[j][1]));
//				p_pp = s1+"\t"+s2;
//				if (pair2scores.containsKey(p_pp)) {
//
//					
//					for (int m=index1[i][0]; m<index1[i][1]; m++)
//						flag1[m] = true;
//					for (int m=index2[j][0]; m<index2[j][1]; m++)
//						flag2[m] = true;
//
//					featureVector.put(ppCount, featureVector.get(ppCount)+1);
//					scores = pair2scores.get(p_pp);
//					for (int k=0; k<scoreSize; k++) {
//						featureVector.put(ppScore+k, featureVector.get(ppScore+k)+scores[k]);
//					}
//				}
//			}
//		}
//		
//		double count = featureVector.get(ppCount);
//		// average scores among all counts of pairs
//		for (int i=0; i<scoreSize; i++) {
//			featureVector.put(ppScore+i, featureVector.get(ppScore+i)/count);
//		}
//		
//		ArrayList<Double> values = new ArrayList<Double>();
//		for (String f:features)
//			if (switches.get(f))
//				values.add(featureVector.get(f)/allNodes);
//
//		return values.toArray(new Double[values.size()]);
//
//	}

	// original over-counting method
//	public Double[] getFeatureValues(EditDist dist) {
//		HashMap<String, Double> featureVector = new HashMap<String, Double> ();
//		for (String f:features)
//			featureVector.put(f, 0.0);
//
//		double allNodes;
//		if (normalized)
//			allNodes = dist.getSize()*1.0;
//		else
//			allNodes = 1.0;
//		
//		String[] splits1 = dist.getSentence1().split("\\s+");
//		String[] splits2 = dist.getSentence2().split("\\s+");
//		int upTo = 4;
//		HashSet<String> slice1 = ArrayUtils.getWindowedSlices(splits1, upTo, phrases);
//		HashSet<String> slice2 = ArrayUtils.getWindowedSlices(splits2, upTo, phrases);
//		String p_pp;
//		double[] scores;
//
//		for (String s1:slice1) {
//			for (String s2:slice2) {
//				p_pp = s1+"\t"+s2;
//				if (pair2scores.containsKey(p_pp)) {
//					featureVector.put(ppCount, featureVector.get(ppCount)+1);
//					scores = pair2scores.get(p_pp);
//					for (int i=0; i<scoreSize; i++) {
//						featureVector.put(ppScore+i, featureVector.get(ppScore+i)+scores[i]);
//					}
//				}
//			}
//		}
//		
//		double count = featureVector.get(ppCount);
//		// average scores among all counts of pairs
//		for (int i=0; i<scoreSize; i++) {
//			featureVector.put(ppScore+i, featureVector.get(ppScore+i)/count);
//		}
//		
//		ArrayList<Double> values = new ArrayList<Double>();
//		for (String f:features)
//			if (switches.get(f))
//				values.add(featureVector.get(f)/allNodes);
//
//		return values.toArray(new Double[values.size()]);
//
//	}
}
