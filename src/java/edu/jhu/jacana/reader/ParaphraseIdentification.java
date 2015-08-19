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
 * This class performs the paraphrase identification task from data under folder
 * tree-edit-data/paraphraseIdentification/data
 * @author Xuchen Yao
 *
 */
public class ParaphraseIdentification {
	
	/**
	 * The direction to extract features on two sentences.
	 * LONG2SHORT: transform from the long sentence to the short one
	 * SHORT2LONG: transform from the short sentence to the long one
	 * in the case of a tie, transform from aTree to bTree
	 * BOTH: transform in both directions and add them up (as in Heilman's paper)
	 * @author Xuchen Yao
	 *
	 */
	protected enum DIRECTION {BOTH, LONG2SHORT, SHORT2LONG};

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
		// 2 more symbols I don't know what's for (e.g., false   -426.53599504880304)
		int fromIndex = 1 + size*5 + 2;
		DependencyTree tree2 = parseFields(fields.subList(fromIndex, fields.size()));
			
		return new DependencyTree[]{tree1, tree2};
	}
	
	protected static DependencyTree parseFields (List<String> fields) {
		int size = Integer.parseInt(fields.get(0));
		String word_line = StringUtils.join(fields.subList(1, 1+size), "\t");
	    String pos_line = StringUtils.join(fields.subList(1+size, 1+size*2), "\t");
	    String lab_line = StringUtils.join(fields.subList(1+size*2, 1+size*3), "\t");
	    String deps_line = StringUtils.join(fields.subList(1+size*3, 1+size*4), "\t");

	    DependencyTree tree = MstReader.read(word_line, pos_line, lab_line, deps_line);
	    return tree;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String trainFile, arffFile, editFile = null;
		boolean normalized = true;
		DIRECTION dir = DIRECTION.BOTH;
		if (args.length >= 2) {
			trainFile = args[0];
			arffFile = args[1];
			if (args.length == 3)
				// for inspection, print out all edit sequences to this file
				editFile = args[2];
		} else {
			//trainFile = "tree-edit-data/paraphraseIdentification/data/par_train.dat";
			trainFile = "tree-edit-data/paraphraseIdentification/data/stanford-parsed/t";
			arffFile = "/tmp/pp.train.arff";
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
				//new ParaphraseFeature(false, "tree-edit-data/paraphraseIdentification/p_pp_all_pairs_with_score.gz"),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BiP", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "MDS", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "svm.g", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "svm.m", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BasicNgramMonoWithVocab.g", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BasicNgramMonoWithVocab.m", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BasicSyntMonoWithVocab.g", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BasicSyntMonoWithVocab.m", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BasicSyntClassOnlyDepMonoWithVocab.g", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "BasicSyntClassOnlyDepMonoWithVocab.m", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "FamilySyntMonoWithVocab.g", 5),
//				new MergedParaphraseFeature(normalized, "/home/xuchen/Vulcan/merged.context_independent.gz", "FamilySyntMonoWithVocab.m", 5),
		};
		
		try {
			BufferedReader in = FileManager.getReader(trainFile);
			BufferedWriter out = FileManager.getWriter(arffFile);
			BufferedWriter editOut = null;
			if (editFile != null)
				editOut = FileManager.getWriter(editFile);
			String line;
			
			out.write("@RELATION Paraphrase\n");
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
				
				String aString = trees[0].toCompactString();
				LblTree aTree = LblTree.fromString(aString);
				aTree.setSentence(trees[0].getSentence());
				
				String bString = trees[1].toCompactString();
				LblTree bTree = LblTree.fromString(bString);
				bTree.setSentence(trees[1].getSentence());
				
				//System.out.println(trees[0].getSentence());
				//System.out.println(trees[1].getSentence());
				
				EditDist a2b = new EditDist(true);
				a2b.treeDist(aTree, bTree);
				
				EditDist b2a = new EditDist(true);
				b2a.treeDist(bTree, aTree);
				
				EditDist long2short, short2long;
				if (trees[0].getSize() >= trees[1].getSize()) {
					long2short = a2b;
					short2long = b2a;
				} else {
					long2short = b2a;
					short2long = a2b;
				}
				
				switch (dir) {
					case BOTH:
						if (editOut != null)
							editOut.write(a2b.printHumaneEditScript()+"\t"+b2a.printHumaneEditScript()+"\n");
						else {
							a2b.printHumaneEditScript();
							b2a.printHumaneEditScript();
						}
						counter += 2;
						break;
					case LONG2SHORT:
						if (editOut != null)
							editOut.write(long2short.printHumaneEditScript()+"\n");
						else {
							long2short.printHumaneEditScript();
						}
						counter += 1;
						break;
					case SHORT2LONG:
						if (editOut != null)
							editOut.write(short2long.printHumaneEditScript()+"\n");
						else {
							short2long.printHumaneEditScript();
						}
						counter += 1;
						break;
				}		
				
				
		
				StringBuilder sb = new StringBuilder();
				switch (dir) {
				case BOTH:
					for (FeatureExtractor ex:extractors) {
						Double[] a2bValues = ex.getFeatureValues(a2b);
						Double[] b2aValues = ex.getFeatureValues(b2a);
						for (int j=0; j<a2bValues.length; j++)
							sb.append(String.format("%f,", a2bValues[j]+b2aValues[j]));
					}
					break;
				case LONG2SHORT:
					for (FeatureExtractor ex:extractors) {
						Double[] l2sValues = ex.getFeatureValues(long2short);
						for (int j=0; j<l2sValues.length; j++)
							sb.append(String.format("%f,", l2sValues[j]));
					}
					break;
				case SHORT2LONG:
					for (FeatureExtractor ex:extractors) {
						Double[] s2lValues = ex.getFeatureValues(short2long);
						for (int j=0; j<s2lValues.length; j++)
							sb.append(String.format("%f,", s2lValues[j]));
					}
					break;
					
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
