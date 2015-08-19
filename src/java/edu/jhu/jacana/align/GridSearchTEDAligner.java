/**
 * 
 */
package edu.jhu.jacana.align;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import approxlib.distance.EditDist;
import approxlib.distance.EditDistGridSearch;
import approxlib.distance.EditDistWordnet;
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
public class GridSearchTEDAligner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String trainFile, editFile = null;

		float min = 0, max = 6;

		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("a", "c_lemma_min", true, "");
		options.addOption("b", "c_lemma_max", true, "");
		options.addOption("c", "c_pp_min", true, "");
		options.addOption("d", "c_pp_max", true, "");
		options.addOption("e", "c_hypernym_min", true, "");
		options.addOption("f", "c_hypernym_max", true, "");
		options.addOption("g", "train", true, "");
		options.addOption("h", "edit", true, "");
		options.addOption("i", "c_hyponym_min", true, "");
		options.addOption("j", "c_hyponym_max", true, "");
		options.addOption("k", "c_synonym_min", true, "");
		options.addOption("l", "c_synonym_max", true, "");
		options.addOption("m", "c_synset_min", true, "");
		options.addOption("n", "c_synset_max", true, "");
		options.addOption("o", "c_pos_min", true, "");
		options.addOption("p", "c_pos_max", true, "");

		CommandLine cmdline = null;
		try {
			cmdline = parser.parse( options, args );
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		float c_lemma_min = Float.parseFloat(cmdline.getOptionValue("c_lemma_min", Float.toString(min)));
		float c_lemma_max = Float.parseFloat(cmdline.getOptionValue("c_lemma_max", Float.toString(max)));
		float c_pp_min = Float.parseFloat(cmdline.getOptionValue("c_pp_min", Float.toString(min)));
		float c_pp_max = Float.parseFloat(cmdline.getOptionValue("c_pp_max", Float.toString(max)));
		float c_hypernym_min = Float.parseFloat(cmdline.getOptionValue("c_hypernym_min", Float.toString(min)));
		float c_hypernym_max = Float.parseFloat(cmdline.getOptionValue("c_hypernym_max", Float.toString(max)));
		float c_hyponym_min = Float.parseFloat(cmdline.getOptionValue("c_hyponym_min", Float.toString(min)));
		float c_hyponym_max = Float.parseFloat(cmdline.getOptionValue("c_hyponym_max", Float.toString(max)));
		float c_synonym_min = Float.parseFloat(cmdline.getOptionValue("c_synonym_min", Float.toString(min)));
		float c_synonym_max = Float.parseFloat(cmdline.getOptionValue("c_synonym_max", Float.toString(max)));
		float c_synset_min = Float.parseFloat(cmdline.getOptionValue("c_synset_min", Float.toString(min)));
		float c_synset_max = Float.parseFloat(cmdline.getOptionValue("c_synset_max", Float.toString(max)));
		float c_pos_min = Float.parseFloat(cmdline.getOptionValue("c_pos_min", Float.toString(min)));
		float c_pos_max = Float.parseFloat(cmdline.getOptionValue("c_pos_max", Float.toString(max)));
		float c_dep_min = Float.parseFloat(cmdline.getOptionValue("c_dep_min", Float.toString(min)));
		float c_dep_max = Float.parseFloat(cmdline.getOptionValue("c_dep_max", Float.toString(max)));
		trainFile = cmdline.getOptionValue("train", "alignment-data/msr/converted/parse/RTE2_dev_M.dat");
		editFile = cmdline.getOptionValue("edit", "/tmp/RTE2_dev_M");

		List<LblTree> aTreeList = new ArrayList<LblTree>(), bTreeList = new ArrayList<LblTree>();
		BufferedReader in;
		try {
			in = FileManager.getReader(trainFile);
			String line;

			while ((line = in.readLine()) != null) {

				//if (counter % 200 == 0)
				//	System.out.println(counter);
				String[] splits = line.split("\\t");
				DependencyTree[] trees = TextualEntailment.parseLine(splits);
				String aString = trees[0].toCompactString();
				LblTree aTree = LblTree.fromString(aString);
				aTree.setSentence(trees[0].getOrigSentence());

				String bString = trees[1].toCompactString();
				LblTree bTree = LblTree.fromString(bString);
				bTree.setSentence(trees[1].getOrigSentence());

				aTreeList.add(aTree);
				bTreeList.add(bTree);


			}
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		float c_pos, c_dep, c_lemma, c_hypernym, c_hyponym, c_synonym, c_sameSynset, c_pp;
		for (c_lemma = c_lemma_min; c_lemma <= c_lemma_max; c_lemma++) {
			for (c_pp = c_pp_min; c_pp <= c_pp_max; c_pp++) {
				for (c_hypernym = c_hypernym_min; c_hypernym <= c_hypernym_max; c_hypernym++) {

					try {

						String editFileName = String.format("%s.--c_lemma-%.1f--c_pp-%.1f--c_hypernym-%.1f--.align.txt.gz", editFile, c_lemma, c_pp, c_hypernym);
						BufferedWriter out = FileManager.getWriter(editFileName, true);
						for (c_hyponym = c_hyponym_min; c_hyponym <= c_hyponym_max; c_hyponym++) {
							for (c_synonym = c_synonym_min; c_synonym <= c_synonym_max; c_synonym++) {
								for (c_sameSynset = c_synset_min; c_sameSynset <= c_synset_max; c_sameSynset++) {
									for (c_pos = c_pos_min; c_pos <= c_pos_max; c_pos++) {
										for (c_dep = c_dep_min; c_dep <= c_dep_max; c_dep++) {
											int counter = 1;
											String editName = String.format("=====%s.--c_lemma-%.1f--c_pp-%.1f--c_hypernym-%.1f--c_hyponym-%.1f--c_synonym-%.1f--c_sameSynset-%.1f--c_pos-%.1f--c_dep-%.1f--.align.txt\n", 
													editFile, c_lemma, c_pp, c_hypernym, c_hyponym, c_synonym, c_sameSynset, c_pos, c_dep);
											String tabs = String.format("-----%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\n", c_lemma, c_pp, c_hypernym, c_hyponym, c_synonym, c_sameSynset, c_pos, c_dep);

											StringBuilder sb = new StringBuilder();
											sb.append(editName);
											sb.append(tabs);
											for (int i=0; i<aTreeList.size(); i++) {
												counter = i+1;
												//EditDist a2b = new EditDist(true);
												// set q2a = false since the first tree (hypothesis) is longer
												EditDist a2b = new EditDistGridSearch(true, false, c_pos, c_dep, c_lemma, 
														c_hypernym, c_hyponym, c_synonym, c_sameSynset, c_pp);
												a2b.treeDist(aTreeList.get(i), bTreeList.get(i));

												a2b.printHumaneEditScript();

												sb.append("# sentence pair " + counter);
												sb.append("\n");
												sb.append(SimpleTEDAligner.getMSRalignFormat(a2b));
												sb.append("\n");
											}
											sb.append("++++++\n");
											out.write(sb.toString());

										}
									}}}}

						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}


				}
			}
		}
		//EditDistGridSearch.printStat();
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
