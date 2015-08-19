/**
 * 
 */
package edu.jhu.jacana.reader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import approxlib.distance.EditDist;
import approxlib.distance.EditDistWordnet;
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
import edu.jhu.jacana.feature.WordnetFeature;
import edu.jhu.jacana.util.FileManager;

/**
 * This Class reads in the pseudo-xml files under the answerSelectionExperiments folder
 * and outputs extracted features.
 * @author Xuchen Yao
 *
 */
public class AnswerSelection {

	private enum TYPE {QUESTION, POSITIVE, NEGATIVE}
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
			trainFile = "tree-edit-data/answerSelectionExperiments/data/train-less-than-40.xml";
			//trainFile = "tree-edit-data/answerSelectionExperiments/data/test.xml";
			arffFile = "/tmp/qa.train.arff";
		}
		
		
		DependencyTree qTree = null;

		List<DependencyTree> aTreeList = new ArrayList<DependencyTree>();


		TYPE type = null;
		List<TYPE> types = new ArrayList<TYPE>();
		
		long time1 = (new Date()).getTime();
		int counter = 0;
		
		FeatureExtractor[] extractors = {new CountingFeature(normalized), 
				new DistFeature(),
				new RenamePosFeature(normalized),
				new InsertFeature(normalized),
				new DeleteFeature(normalized),
				new UneditedFeature(normalized),
				new RenameRelFeature(normalized),
				new WordnetFeature(normalized),
				//new ParaphraseFeature(false, "tree-edit-data/answerSelectionExperiments/p_pp_all_pairs_with_score.gz"),
				//new MergedParaphraseFeature(false, "/home/hltcoe/xuchen/Vulcan/tree-edit-data/LSH/pphraseScoreRegression/merged.context_independent.gz",	"BasicSyntClassOnlyDepMonoWithVocab.m", 5),
				//new MergedParaphraseFeature(false, "/home/hltcoe/xuchen/Vulcan/tree-edit-data/LSH/pphraseScoreRegression/merged.context_independent.gz",	"BiP", 5),
		};
		
		try {
			BufferedReader in = FileManager.getReader(trainFile);
			BufferedWriter out = FileManager.getWriter(arffFile);
			BufferedWriter editOut = null;
			if (editFile != null)
				editOut = FileManager.getWriter(editFile);
			String line;
			
			out.write("@RELATION QA\n");
			for (FeatureExtractor ex:extractors) {
				out.write(ex.arffAttributeHeader());
			}
			/**
			 * the class name {positive,negative} here is strict:
			 * the perl script convertWekaOutputToTRECFormat.pl matches negative
			 * by ":n" (such as in ":negative") so make sure the negative class
			 * starts with lower-case n
			 */
			out.write("@ATTRIBUTE class        {positive,negative}\n");
			out.write("@DATA\n");
			
			while ((line = in.readLine()) != null) {
				if (line.startsWith("</QApairs")) {
					if (aTreeList.size() > 0) {
						
						//String qString = qTree.stringInRTEDtree();
						String qString = qTree.toCompactString();

						LblTree qtree = LblTree.fromString(qString);
						qtree.setSentence(qTree.getSentence());
						counter += aTreeList.size();
						//System.out.println(qTree.getSentence());
						
						for (int i=0; i<aTreeList.size(); i++) {
							DependencyTree tree = aTreeList.get(i);
							type = types.get(i);
							String aString = tree.toCompactString();
							LblTree atree = LblTree.fromString(aString);
							atree.setSentence(tree.getSentence());
							//System.out.println(tree.getSentence());
							EditDist dis2 = new EditDist(true);
							//EditDistWordnet dis2 = new EditDistWordnet(true);
							dis2.treeDist(qtree, atree);
							
							//EditDistWordnet dis2 = new EditDistWordnet(true, false);
							//dis2.treeDist(atree, qtree);
							if (editOut != null)
								editOut.write(dis2.printHumaneEditScript()+"\n");
							else
								dis2.printHumaneEditScript();
							
							String pn =  type == TYPE.POSITIVE?"positive":"negative";
							
							StringBuilder sb = new StringBuilder();
							for (FeatureExtractor ex:extractors) {
								sb.append(ex.arffData(dis2));
							}
							out.write(sb.toString()+String.format("%s\n", pn));
						}
					}
					aTreeList.clear();
					types.clear();

				} else if (line.startsWith("<question>")) {
					type = TYPE.QUESTION;
				} else if (line.startsWith("<positive>")) {
					type = TYPE.POSITIVE;
				} else if (line.startsWith("<negative>")) {
					type = TYPE.NEGATIVE;
				} else if (line.startsWith("</question>") || line.startsWith("<QApairs") ||
						line.startsWith("</positive>") || line.startsWith("</negative>") ) {
					//do nothing
				} else {
				    String pos_line = in.readLine();
				    String lab_line = in.readLine();
				    String deps_line = in.readLine();
				    in.readLine(); // NER line
	
				    //System.out.println(line);
				    DependencyTree tree = MstReader.read(line, pos_line, lab_line, deps_line);
				    switch (type) {
				    case QUESTION: qTree = tree; break;
				    case POSITIVE: aTreeList.add(tree); types.add(type); break;
				    case NEGATIVE: aTreeList.add(tree); types.add(type); break;
				    default: System.err.println("landed in the middle of nowhere. check your code!"); break;
				    }
				}
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
