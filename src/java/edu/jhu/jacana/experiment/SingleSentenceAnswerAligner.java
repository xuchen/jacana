/**
 * 
 */
package edu.jhu.jacana.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import approxlib.distance.EditDist;
import approxlib.distance.EditDistQuestion;
import approxlib.tree.LblTree;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.feature.CountingFeature;
import edu.jhu.jacana.feature.DeleteFeature;
import edu.jhu.jacana.feature.DistFeature;
import edu.jhu.jacana.feature.FeatureExtractor;
import edu.jhu.jacana.feature.InsertFeature;
import edu.jhu.jacana.feature.ParaphraseFeature;
import edu.jhu.jacana.feature.RenamePosFeature;
import edu.jhu.jacana.feature.RenameRelFeature;
import edu.jhu.jacana.feature.UneditedFeature;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.FileManager;

/**
 * This class reads in the pseudo-xml files and outputs what the questions words align to.
 * It outputs alignment for each sentence candidate. 
 * 
 * @author Xuchen Yao
 *
 */
public class SingleSentenceAnswerAligner {

	private enum TYPE {QUESTION, POSITIVE, NEGATIVE}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String inFile, outFile;
		boolean normalized = false;
		if (args.length >= 2) {
			inFile = args[0];
			outFile = args[1];
		} else {
//			inFile = "tree-edit-data/answerSelectionExperiments/data/train-less-than-40.xml";
//			outFile = "/tmp/qa.aligner.txt";
			inFile = "/home/xuchen/Vulcan/answerAligner/what-questions-and-search-results.4perGroup.xml";
			outFile = "/home/xuchen/Vulcan/answerAligner/what-questions-and-search-results.4perGroup.multi-word-answer";
		}
		
		
		DependencyTree qTree = null;

		List<DependencyTree> aTreeList = new ArrayList<DependencyTree>();


		TYPE type = null;
		List<TYPE> types = new ArrayList<TYPE>();
		
		long time1 = (new Date()).getTime();
		int counter = 0;
		
		
		try {
			BufferedReader in = FileManager.getReader(inFile);
			BufferedWriter out = FileManager.getWriter(outFile);

			String line;
			
			
			while ((line = in.readLine()) != null) {
				if (line.startsWith("<QApairs")) {
					if (aTreeList.size() > 0) {
						
						//String qString = qTree.stringInRTEDtree();
						String qString = qTree.toCompactString();
						
						if (qString.contains("who") || qString.contains("when") || 
								qString.contains("what") || qString.contains("where")) {

							LblTree qtree = LblTree.fromString(qString);
							qtree.setSentence(qTree.getSentence());

							StringBuilder sb = new StringBuilder();
							sb.append("===================\n");
							sb.append(qTree.getSentence()+"\n");
							sb.append("===================\n");
							
							for (int i=0; i<aTreeList.size(); i++) {
								counter ++;
								if (counter % 200 == 0)
									System.out.println(counter);

								DependencyTree tree = aTreeList.get(i);
								type = types.get(i);
								if (type == TYPE.NEGATIVE)
									continue;
								String aString = tree.toCompactString();
								LblTree atree = LblTree.fromString(aString);
								atree.setSentence(tree.getSentence());
								EditDistQuestion dis2 = new EditDistQuestion(true);
								dis2.treeDist(qtree, atree);
								dis2.printHumaneEditScript();
								
//								String pn =  type == TYPE.POSITIVE?"positive":"negative";
//								sb.append(String.format("%s\t%s<->%s\t|||\t%s\n", pn, dis2.getQWord(), dis2.getAWord(), atree.getSentence()));
								
								sb.append(String.format("%s<->%s\t|||\t%s\n", dis2.getQWord(), dis2.getAWord(), atree.getSentence()));
							}
							out.write(sb.toString());
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
				} else if (line.startsWith("</question>") || line.startsWith("</QApairs>") ||
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long time2 = (new Date()).getTime();
		double duration = (time2-time1)/1000.0;
		System.out.println(String.format("runtime: %.1fs, %d instances, %.1fms/instance", duration, counter, duration*1000/counter));


	}

}
