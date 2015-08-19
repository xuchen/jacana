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
import java.util.AbstractMap.SimpleImmutableEntry;

import edu.jhu.jacana.util.ArrayUtils;
import edu.jhu.jacana.util.FileManager;
import edu.jhu.jacana.util.KBest;
import edu.jhu.jacana.util.StringUtils;

import approxlib.distance.Edit;
import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

/**
 * @author Xuchen Yao
 *
 */
public class MergedParaphraseFeature extends NormalizedFeatureExtractor {

	protected String ppCount = "ppCount", ppScore="ppScore",
			ppSub="ppSub", ppObj="ppObj", ppNmod="ppNmod", ppVmod="ppVmod",
			ppPmod="ppPmod", ppMod="ppMod",
			ppNoun = "ppNoun", ppVerb = "ppVerb", ppDT="ppDT", ppIN="ppIN", ppJJ="ppJJ",
			ppDel = "ppDel", ppIns = "ppIns", ppRen = "ppRen", ppAlign = "ppAlign";
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
	HashMap<String, Double> pair2scores;
	HashSet<String> phrases1;
	HashSet<String> phrases2;
	
	// the index for specific method we want to use. 
	int methodIdx = -1;
	
	/**
	 * if set to false, then only ppCount/ppScore* are enabled.
	 * if set to true, then pp{Sub, Obj, ...} are also used. 
	 */
	boolean usePPclassFeature;


	public MergedParaphraseFeature(boolean normalized, String ppFile, String method, int topK) {
		super(normalized);
		String line, phrase = null;
		pair2scores = new HashMap<String, Double>();
		phrases1 = new HashSet<String>();
		phrases2 = new HashSet<String>();
		BufferedReader in;
		String[] splits;
		String p_pp;
		KBest<String> kbest = new KBest<String>(topK, true, true);;
		try {
			in = FileManager.getReader(ppFile);
			int counter = 0;
			while ((line = in.readLine()) != null) {

				splits = line.split("\t");
				if (counter++ == 0) {
					for (int i=2; i<splits.length; i++) {
						if (splits[i].equalsIgnoreCase(method)) {
							methodIdx = i;
							break;
						}
					}
					if (methodIdx == -1) {
						System.err.println(method+ " doesn't exist in header!");
						System.exit(-1);
					}
					continue;
				}
				
				if (phrase != null && !splits[0].equals(phrase)) {
					extractKbest(kbest);
					kbest = new KBest<String>(topK, true, true);
				}
				
				phrase = splits[0];
				p_pp = splits[0] + "\t" + splits[1];
				kbest.insert(p_pp, Double.valueOf(splits[this.methodIdx]));
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// the last one
		extractKbest(kbest);
		
		ArrayList<String> featList = new ArrayList<String>();
		
		ppCount += "."+method;
		ppScore += "."+method;
		ppSub += "."+method;
		ppObj += "."+method;
		ppNmod += "."+method;
		ppVmod += "."+method;
		ppPmod += "."+method;
		ppMod += "."+method;
		ppNoun += "."+method;
		ppVerb += "."+method; 
		ppDT += "."+method; 
		ppIN += "."+method;
		ppJJ += "."+method;
		ppDel += "."+method;
		ppIns += "."+method;
		ppRen += "."+method;
		ppAlign += "."+method;
		
		featList.add(ppScore);
		featList.add(ppSub);
		featList.add(ppObj);
		featList.add(ppNmod);
		featList.add(ppVmod);
		featList.add(ppPmod);
		featList.add(ppMod);
		featList.add(ppCount);
		featList.add(ppNoun);
		featList.add(ppVerb);
		featList.add(ppDT);
		featList.add(ppIN);
		featList.add(ppJJ);
		featList.add(ppDel);
		featList.add(ppIns);
		featList.add(ppRen);
		featList.add(ppAlign);
		features = featList.toArray(new String[featList.size()]);
		switches = new HashMap<String, Boolean>() {
			{
				put(ppScore, true);
				put(ppCount, true);
				
				// if enable any of the following, then also enable usePPclassFeature
				put(ppSub, false);
				put(ppObj, false);
				put(ppNmod, false);
				put(ppVmod, false);
				put(ppPmod, false);
				put(ppMod, false);
				
				put(ppDel, true);
				put(ppIns, true);
				put(ppRen, true);
				put(ppAlign, true);
				
				// don't enable the following, they are bad features
				put(ppNoun, false);
				put(ppVerb, false);
				put(ppDT, false);
				put(ppIN, false);
				put(ppJJ, false);
			}
		};
		usePPclassFeature = false;
		for (int i=2; i<features.length; i++)
			if (switches.get(features[i])) {
				usePPclassFeature = true;
				break;
			}
	}
	
	private void extractKbest(KBest<String> kbest) {
		SimpleImmutableEntry<String,Double>[] results = kbest.toArray();
		for (int i=0; i<results.length; i++) {
			String[] fields = results[i].getKey().split("\t");
			phrases1.add(fields[0]);
			phrases2.add(fields[1]);
			pair2scores.put(results[i].getKey(), results[i].getValue());
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
		int[][] index1 = ArrayUtils.getWindowedSlicesIndex(splits1, upTo, phrases1);
		int[][] index2 = ArrayUtils.getWindowedSlicesIndex(splits2, upTo, phrases2);
		boolean[] flag1 = new boolean[splits1.length];
		boolean[] flag2 = new boolean[splits2.length];
		Arrays.fill(flag1, false);
		Arrays.fill(flag2, false);
		String p_pp;
		double score;
		String s1, s2;
		boolean clean = true;
		int count = 1;
		
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
					// count = index1[i][1] - index1[i][0] + index2[j][1] - index2[j][0];
					count = 1;
					
					featureVector.put(ppCount, featureVector.get(ppCount)+count);
					score = pair2scores.get(p_pp);
					featureVector.put(ppScore, featureVector.get(ppScore)+score);
					
					if (usePPclassFeature) {
						for (int m=index1[i][0]; m<index1[i][1]; m++) {
							String rel = dist.getRel1()[dist.getIdxInWordOrder2node1().get(m).getIdxInPostOrder()];
							addPpRel(rel, featureVector);
							String pos = dist.getPos1()[dist.getIdxInWordOrder2node1().get(m).getIdxInPostOrder()];
							addPpPos(pos, featureVector);
						}
						
						for (int m=index2[j][0]; m<index2[j][1]; m++) {
							String rel = dist.getRel2()[dist.getIdxInWordOrder2node2().get(m).getIdxInPostOrder()];
							addPpRel(rel, featureVector);
							String pos = dist.getPos2()[dist.getIdxInWordOrder2node2().get(m).getIdxInPostOrder()];
							addPpPos(pos, featureVector);
						}
						
					}
				}
			}
		}
		
		// pp del/ins/ren/align related features
		ArrayList<Edit> editList = dist.getEditList();
		int idx, id;

		for (Edit edit:editList) {
			String editType = edit.getType().toString();
			id = edit.getArgs()[0];			
			switch (edit.getType()) {
			case DEL:
			case DEL_SUBTREE:
			case DEL_LEAF:
				idx = dist.id2idxInWordOrder1(id);
				if (idx == -1) continue;
				if (flag1[idx])
					featureVector.put(ppDel, featureVector.get(ppDel)+count);
				break;
			case INS:
			case INS_SUBTREE:
			case INS_LEAF:
				idx = dist.id2idxInWordOrder2(id);
				if (idx == -1) continue;
				if (flag2[idx])
					featureVector.put(ppIns, featureVector.get(ppIns)+count);
				break;
			case REN_POS:
			case REN_REL:
			case REN_POS_REL:
				idx = dist.id2idxInWordOrder1(id);
				if (flag1[idx])
					featureVector.put(ppRen, featureVector.get(ppRen)+count);
				break;
			}
		}
		
		for (Integer id1:dist.getAlign1to2().keySet()) {
			if (id1 == -1) continue;
			int idx1 = dist.id2idxInWordOrder1(id1);
			if (idx1 == -1) continue;
			if (flag1[idx1])
				featureVector.put(ppAlign, featureVector.get(ppAlign)+count);
		}
		
		double all = featureVector.get(ppCount);
		// average scores among all counts of pairs
		featureVector.put(ppScore, featureVector.get(ppScore)/all);

		
		ArrayList<Double> values = new ArrayList<Double>();
		for (String f:features)
			if (switches.get(f))
				values.add(featureVector.get(f)/allNodes);

		return values.toArray(new Double[values.size()]);

	}
	
	private void addPpRel(String rel, HashMap<String, Double> featureVector) {
		int count = 1;
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
		if (rel.endsWith("mod"))
			featureVector.put(ppMod, featureVector.get(ppMod)+count);
	}
	
	private void addPpPos(String pos, HashMap<String, Double> featureVector) {
		int count = 1;
		if (pos.startsWith("nn"))
			featureVector.put(ppNoun, featureVector.get(ppNoun)+count);
		else if (pos.startsWith("vb"))
			featureVector.put(ppVerb, featureVector.get(ppVerb)+count);
		else if (pos.startsWith("dt"))
			featureVector.put(ppDT, featureVector.get(ppDT)+count);
		else if (pos.startsWith("jj"))
			featureVector.put(ppJJ, featureVector.get(ppJJ)+count);
		else if (pos.startsWith("in"))
			featureVector.put(ppIN, featureVector.get(ppIN)+count);
	}
	
	// precise counting isn't as good as the over counting method, thus commenting out
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
//		final int upTo = 4;
//		int[][] index1 = ArrayUtils.getWindowedSlicesIndex(splits1, upTo, phrases1);
//		int[][] index2 = ArrayUtils.getWindowedSlicesIndex(splits2, upTo, phrases2);
//		boolean[] flag1 = new boolean[splits1.length];
//		boolean[] flag2 = new boolean[splits2.length];
//		Arrays.fill(flag1, false);
//		Arrays.fill(flag2, false);
//		String p_pp;
//		double score;
//		String s1, s2;
//		boolean clean = true;
//		
//		String[][] span2rel1 = null, span2rel2 = null;
//		
//		if (usePPclassFeature) {
//			span2rel1 = getSpan2DepRel(dist.getTree1(), upTo);
//			span2rel2 = getSpan2DepRel(dist.getTree2(), upTo);
//		}
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
//					// the following analysis makes sense, but doesn't increase F1, so commented...
////					if (!(index1[i][1] - index1[i][0] == 1 && index2[j][1] - index2[j][0] == 1) && 
////							(s1.contains(s2) || s2.contains(s1)))
////						// we see a lot of pairs such as history<->in history, of this issue<->this issue
////						// we don't want to count them as paraphrases.
////						// however, if both phrases are single words, such as american<->americans, we count them.
////						continue;
//
//					for (int m=index1[i][0]; m<index1[i][1]; m++)
//						flag1[m] = true;
//					for (int m=index2[j][0]; m<index2[j][1]; m++)
//						flag2[m] = true;
//
//					// count can be 1: the number of paraphrase substitutions.
//					// or it can be the word counts of phrase+paraphrase.
//					// experiment shows that both lead to a very similar results (0.1% diff in recall)
//					//int count = index1[i][1] - index1[i][0] + index2[j][1] - index2[j][0];
//					int count = 1;
//					
//					featureVector.put(ppCount, featureVector.get(ppCount)+count);
//					score = pair2scores.get(p_pp);
//					featureVector.put(ppScore, featureVector.get(ppScore)+score);
//					
//					if (usePPclassFeature) {
//						if (span2rel1[index1[i][0]][index1[i][1]] != null && span2rel2[index2[j][0]][index2[j][1]] != null
//								/*&& span2rel1[index1[i][0]][index1[i][1]].equals(span2rel2[index2[j][0]][index2[j][1]])*/) {
//							String rel = span2rel1[index1[i][0]][index1[i][1]];
//							String rel2 = span2rel2[index2[j][0]][index2[j][1]];
//							
//							boolean debug = false;
//							if (debug) {
//								String p1="", p2="";
//								for (int n=index1[i][0]; n<index1[i][1]; n++)
//									p1 += splits1[n]+ " ";
//								for (int n=index2[j][0]; n<index2[j][1]; n++)
//									p2 += splits2[n] + " ";
//								System.out.println(p1+"/"+rel+"\t"+p2+"/"+rel2);
//							}
//							
//							if (span2rel1[index1[i][0]][index1[i][1]].equals(span2rel2[index2[j][0]][index2[j][1]])) {
//								if (rel.startsWith("sub")) {
//									featureVector.put(ppSub, featureVector.get(ppSub)+count);
//								} else if (rel.startsWith("obj")) {
//									featureVector.put(ppObj, featureVector.get(ppObj)+count);
//								} else if (rel.endsWith("nmod")) {
//									featureVector.put(ppNmod, featureVector.get(ppNmod)+count);
//								} else if (rel.endsWith("vmod")) {
//									featureVector.put(ppVmod, featureVector.get(ppVmod)+count);
//								} else if (rel.endsWith("pmod")) {
//									featureVector.put(ppPmod, featureVector.get(ppPmod)+count);
//								}
//							} else {
//								if (rel.endsWith("mod") && rel2.endsWith("mod"))
//									featureVector.put(ppMod, featureVector.get(ppMod)+count);
//							}
//
//						}
//					}
//				}
//			}
//		}
//		
//		double count = featureVector.get(ppCount);
//		// average scores among all counts of pairs
//		featureVector.put(ppScore, featureVector.get(ppScore)/count);
//
//		
//		ArrayList<Double> values = new ArrayList<Double>();
//		for (String f:features)
//			if (switches.get(f))
//				values.add(featureVector.get(f)/allNodes);
//
//		return values.toArray(new Double[values.size()]);
//
//	}
	
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
