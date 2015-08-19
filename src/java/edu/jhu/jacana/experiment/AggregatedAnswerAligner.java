/**
 * 
 */
package edu.jhu.jacana.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import edu.jhu.jacana.util.MapUtil;

/**
 * This class reads in the pseudo-xml files and outputs what the questions words align to.
 * For each question, if accompanied by multiple answer sentences, it outputs the alignment
 * in a ranked list, with each answer candidates voted by the number of supporting sentences.
 * 
 * @author Xuchen Yao
 *
 */
public class AggregatedAnswerAligner {

	private enum TYPE {QUESTION, POSITIVE, NEGATIVE}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String inFile, outFile;
		String id = null;
		// <QApairs id='32.1'>
		Pattern p = Pattern.compile("<QApairs id='(.*)'>");
		if (args.length >= 2) {
			inFile = args[0];
			outFile = args[1];
		} else {
			inFile = "tree-edit-data/answerSelectionExperiments/answer/dev-less-than-40.manual-edit.xml";
			outFile = "/tmp/test.aligner.answer";
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
				if (line.startsWith("</QApairs")) {

					HashMap<String, Integer> answer2vote = new HashMap<String, Integer>(); 
					if (aTreeList.size() > 0) {

						String qString = qTree.toCompactString();

						LblTree qtree = LblTree.fromString(qString);
						qtree.setSentence(qTree.getSentence());

						for (int i=0; i<aTreeList.size(); i++) {
							counter ++;
							if (counter % 200 == 0)
								System.out.println(counter);

							DependencyTree tree = aTreeList.get(i);
							
							String aString = tree.toCompactString();
							LblTree atree = LblTree.fromString(aString);
							atree.setSentence(tree.getSentence());
							EditDistQuestion dis2 = new EditDistQuestion(true);
							dis2.treeDist(qtree, atree);
							dis2.printHumaneEditScript();
							
							
							if (dis2.getQWord() != null) {
								//String answer = dis2.getQWord() + "<->" + dis2.getAWord();
								String answer = dis2.getAWord();
								if (answer != null && answer.length() != 0) {
									if (!answer2vote.containsKey(answer))
										answer2vote.put(answer, 0);
									answer2vote.put(answer, answer2vote.get(answer)+1);
								}
							}

						}
						StringBuilder sb = new StringBuilder();
						if (answer2vote.size() > 0) {
							Map<String, Integer> sorted = MapUtil.sortByDescendingValue(answer2vote);
							for (String answer:sorted.keySet()) {
								sb.append(String.format("%s\t%s\t%d\n", id, answer, sorted.get(answer)));
							}
						} else {
							sb.append(String.format("%s\t%s\t%d\n", id, "null", 0));
						}
						out.write(sb.toString());
					}
					aTreeList.clear();
					types.clear();

				} else if (line.startsWith("<question>")) {
					type = TYPE.QUESTION;
				} else if (line.startsWith("<positive>")) {
					type = TYPE.POSITIVE;
				} else if (line.startsWith("<negative>")) {
					type = TYPE.NEGATIVE;
				} else if (line.startsWith("<QApairs")) {
					Matcher m = p.matcher(line);
					m.matches();
					id = m.group(1);
				} else if (line.startsWith("</question>") ||
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
				    case POSITIVE: aTreeList.add(tree); types.add(type); in.readLine(); in.readLine(); break;
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
