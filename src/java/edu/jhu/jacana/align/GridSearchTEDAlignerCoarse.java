/**
 * 
 */
package edu.jhu.jacana.align;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import approxlib.distance.EditDist;
import approxlib.distance.EditDistGridSearchCoarse;
import approxlib.tree.LblTree;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.reader.TextualEntailment;
import edu.jhu.jacana.util.FileManager;

/**
 * This class does a grid search over substitution cost, which is
 * a linear sum of the following:
 * substitution cost = C_pos + C_dep + C_lemma + C_wn
 * C_pos = 0 if POS tags match otherwise w
 * C_dep = 0 if DEP labels match otherwise w
 * C_lemma = 0 if lemmas match otherwise w
 * C_wn = 0 if wordnet hypernym/hyponym/synonym/same_synset relations hold, otherwise w
 * 
 * w ranges in [0, 6], that's our grid. We have different w's for different C's
 * 
 * @author Xuchen Yao
 *
 */
public class GridSearchTEDAlignerCoarse {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String trainFile, editFile = null;

		if (args.length >= 1) {
			trainFile = args[0];
			editFile = args[1];
		} else {
			trainFile = "alignment-data/msr/converted/parse/RTE2_dev_M.dat";
			editFile = "/tmp/RTE2_dev_M";
		}
		float min = 6, max = 6;
		float c_lemma_min = min, c_lemma_max = max, c_wn_min = min, c_wn_max = max;
		if (args.length >= 4) {
			c_lemma_min = c_lemma_max = Float.parseFloat(args[2]);
			c_wn_min = c_wn_max = Float.parseFloat(args[3]);
		}
		
		c_wn_min = c_wn_max = 2;
		float c_pos, c_dep, c_lemma, c_wn;
		for (c_lemma = c_lemma_min; c_lemma <= c_lemma_max; c_lemma++) {
			for (c_wn = c_wn_min; c_wn <= c_wn_max; c_wn++) {
				for (c_pos = 1; c_pos <= 1; c_pos++) {
					for (c_dep = 2; c_dep <= 2; c_dep++) {
						try {
							int counter = 1;
							BufferedReader in = FileManager.getReader(trainFile);
							String editFileName = String.format("%s.--c_lemma-%.1f--c_wn-%.1f--c_pos-%.1f--c_dep-%.1f--.align.txt", 
									editFile, c_lemma, c_wn, c_pos, c_dep);
							BufferedWriter out = FileManager.getWriter(editFileName);
							String line;
				
							while ((line = in.readLine()) != null) {
				
								StringBuilder sb = new StringBuilder();
								if (counter % 200 == 0)
									System.out.println(counter);
								String[] splits = line.split("\\t");
								DependencyTree[] trees = TextualEntailment.parseLine(splits);
								String aString = trees[0].toCompactString();
								LblTree aTree = LblTree.fromString(aString);
								aTree.setSentence(trees[0].getOrigSentence());
								//aTree.prettyPrint();
				
								String bString = trees[1].toCompactString();
								LblTree bTree = LblTree.fromString(bString);
								bTree.setSentence(trees[1].getOrigSentence());
								//bTree.prettyPrint();
				
								//EditDist a2b = new EditDist(true);
								// set q2a = false since the first tree (hypothesis) is longer
								EditDist a2b = new EditDistGridSearchCoarse(true, false, c_pos, c_dep, c_lemma, c_wn, 2);
								a2b.treeDist(aTree, bTree);
				
								a2b.printHumaneEditScript();
								
								sb.append("# sentence pair " + counter);
								sb.append("\n");
								sb.append(SimpleTEDAligner.getMSRalignFormat(a2b));
								sb.append("\n");
								out.write(sb.toString());
								
								counter += 1;
							}
							in.close();
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		EditDistGridSearchCoarse.printStat();
		/*
hypernym: 52
hyponym: 72
synonym: 1355
entails: 0
causing: 0
memberOf: 6
haveMember: 3
substanceOf: 0
haveSubstance: 0
partsOf: 4
haveParts: 5
sameSynset: 1659

lemma match: 9342
pp match: 3176

total length of tree1: 26451
total length of tree2: 11024
		 */
	}

}
