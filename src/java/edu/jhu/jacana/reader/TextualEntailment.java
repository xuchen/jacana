/**
 * 
 */
package edu.jhu.jacana.reader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.feature.CountingFeature;
import edu.jhu.jacana.feature.DeleteFeature;
import edu.jhu.jacana.feature.DistFeature;
import edu.jhu.jacana.feature.FeatureExtractor;
import edu.jhu.jacana.feature.InsertFeature;
import edu.jhu.jacana.feature.MergedParaphraseFeature;
import edu.jhu.jacana.feature.ParaphraseFeature;
import edu.jhu.jacana.feature.RenamePosFeature;
import edu.jhu.jacana.feature.RenameRelFeature;
import edu.jhu.jacana.feature.UneditedFeature;
import edu.jhu.jacana.util.FileManager;
import edu.jhu.jacana.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class TextualEntailment {

	/**
	 * Given a split line, return the two trees represented by this line
	 * @param splits a split line
	 * @return an array of size two
	 */
	public static DependencyTree[] parseLine (String[] splits) {
		List<String> fields = Arrays.<String>asList(splits);
		int size = Integer.parseInt(fields.get(0));
		
		DependencyTree tree1 = parseFields(fields);
		// 1 leading size number + 5 chunks in size (word, pos, lab, dep, ne) + 
		// 1 more symbol I don't know what's for (e.g., false)
		int fromIndex = 1 + size*5 + 1;
		DependencyTree tree2 = parseFields(fields.subList(fromIndex, fields.size()));
			
		return new DependencyTree[]{tree1, tree2};
	}
	
	protected static DependencyTree parseFields (List<String> fields) {
		int size = Integer.parseInt(fields.get(0));
		String word_line = StringUtils.join(fields.subList(1, 1+size), "\t");
	    String pos_line = StringUtils.join(fields.subList(1+size, 1+size*2), "\t");
	    String lab_line = StringUtils.join(fields.subList(1+size*2, 1+size*3), "\t");
	    String deps_line = StringUtils.join(fields.subList(1+size*3, 1+size*4), "\t");

	    DependencyTree tree = MstReader.read(word_line, pos_line, lab_line, deps_line, true);
	    return tree;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String trainFile, arffFile, editFile = null;
		boolean normalized = false;

		if (args.length >= 2) {
			trainFile = args[0];
			arffFile = args[1];
			if (args.length == 3)
				// for inspection, print out all edit sequences to this file
				editFile = args[2];
		} else {
			//trainFile = "tree-edit-data/entailment/data/trainData.dat";
			trainFile = "tree-edit-data/entailment/data/stanford-parsed/t";
			arffFile = "/tmp/rte.train.arff";
		}

		String type;

		long time1 = (new Date()).getTime();
		int counter = 0;

		FeatureExtractor[] extractors = {
				new CountingFeature(normalized), 
				new DistFeature(),
				new RenamePosFeature(normalized),
				new InsertFeature(normalized),
				new DeleteFeature(normalized),
				new RenameRelFeature(normalized),
				new UneditedFeature(normalized),
				//new ParaphraseFeature(false, "tree-edit-data/entailment/p_pp_all_pairs_with_score.gz"),
				//new MergedParaphraseFeature(false, "/home/xuchen/Vulcan/merged.context_independent.gz",	"BasicSyntClassOnlyDepMonoWithVocab.m", 5),
		};

		try {
			BufferedReader in = FileManager.getReader(trainFile);
			BufferedWriter out = FileManager.getWriter(arffFile);
			BufferedWriter editOut = null;
			if (editFile != null)
				editOut = FileManager.getWriter(editFile);
			String line;

			out.write("@RELATION Entailment\n");
			for (FeatureExtractor ex:extractors) {
				out.write(ex.arffAttributeHeader());
			}

			out.write("@ATTRIBUTE class        {positive,negative}\n");
			out.write("@DATA\n");

			while ((line = in.readLine()) != null) {

				if (counter % 200 == 0)
					System.out.println(counter);
				String[] splits = line.split("\\t");
				DependencyTree[] trees = parseLine(splits);
				if (splits[splits.length-1].equals("1"))
					type = "positive";
				else
					type = "negative";
				//System.out.println(trees[0].getSentence());
				//System.out.println(trees[1].getSentence());
				String aString = trees[0].toCompactString();
				LblTree aTree = LblTree.fromString(aString);
				aTree.setSentence(trees[0].getSentence());
				//aTree.prettyPrint();

				String bString = trees[1].toCompactString();
				LblTree bTree = LblTree.fromString(bString);
				bTree.setSentence(trees[1].getSentence());
				//bTree.prettyPrint();

				EditDist a2b = new EditDist(true);
				a2b.treeDist(aTree, bTree);

				if (editOut != null)
					editOut.write(a2b.printHumaneEditScript()+"\n");
				else {
					a2b.printHumaneEditScript();
				}
				counter += 1;

				StringBuilder sb = new StringBuilder();
				
				for (FeatureExtractor ex:extractors) {
					sb.append(ex.arffData(a2b));
				}
				out.write(sb.toString()+String.format("%s\n", type));
			}
			in.close();
			out.close();
			if (editOut != null) editOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		long time2 = (new Date()).getTime();
		double duration = (time2-time1)/1000.0;
		System.out.println(String.format("runtime: %.1fs, %d instances, %.1fms/instance", duration, counter, duration*1000/counter));
	}

}
