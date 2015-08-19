/**
 * 
 */
package edu.jhu.jacana.qa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import approxlib.distance.EditDist;
import approxlib.distance.EditDistQuestion;
import approxlib.tree.LblTree;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.qa.feature.AbstractFeatureExtractor;
import edu.jhu.jacana.qa.feature.ClassExtractor;
import edu.jhu.jacana.qa.feature.DepFeature;
import edu.jhu.jacana.qa.feature.EditFeature;
import edu.jhu.jacana.qa.feature.EditTypeFeature;
import edu.jhu.jacana.qa.feature.ModifierOfAlignmentFeature;
import edu.jhu.jacana.qa.feature.NearestDistanceToAlignmentFeature;
import edu.jhu.jacana.qa.feature.NearestDistanceTreeToAlignmentFeature;
import edu.jhu.jacana.qa.feature.NerFeature;
import edu.jhu.jacana.qa.feature.OverlapFeature;
import edu.jhu.jacana.qa.feature.PosFeature;
import edu.jhu.jacana.qa.feature.QuestionMatchDepFeature;
import edu.jhu.jacana.qa.feature.QuestionMatchFeature;
import edu.jhu.jacana.qa.feature.QuestionMatchNerFeature;
import edu.jhu.jacana.qa.feature.QuestionMatchPosFeature;
import edu.jhu.jacana.qa.feature.QuoteFeature;
import edu.jhu.jacana.qa.feature.WordnetFeature;
import edu.jhu.jacana.qa.feature.WordnetHypernymDepFeature;
import edu.jhu.jacana.qa.feature.WordnetHypernymFeature;
import edu.jhu.jacana.qa.feature.WordnetHypernymPosFeature;
import edu.jhu.jacana.qa.feature.WordnetQWordFeature;
import edu.jhu.jacana.qa.feature.WordnetTagFeature;
import edu.jhu.jacana.qa.feature.template.TemplateExpander;
import edu.jhu.jacana.qa.questionanalysis.QuestionFeature;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.FileManager;


/**
 * This class reads in the pseudo-xml files and outputs what the questions words align to.
 * 
 * @author Xuchen Yao
 *
 */
public class AnswerFinderBaseline {

	private enum TYPE {QUESTION, POSITIVE, NEGATIVE}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inFile = null, outFile = null;

		/**
		 * the ratio of positive/negative examples in training is about 3:17, this will severely
		 * Imbalance the model to prefer "O" rather than "ANSER-B" and "ANSWER-I". For now we
		 * enable using only positive examples. That is: only train on positive examples AND
		 * only test on positive examples
		 */
		boolean onlyPositive = true;
		
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("i", "input", true, "input pseudo-xml file");
		options.addOption("o", "output", true, "output tagged file");
		options.addOption("a", "all", false, "process all sentences (both positives and negatives).\n"+
											"Default is false: only process positive ones.\n" +
											"Must be used when testing: you don't know the example is positive or negative.");
		//HelpFormatter formatter = new HelpFormatter();
		
		try {
		    CommandLine line = parser.parse( options, args );
		    
		    inFile = line.getOptionValue("input", "tree-edit-data/answerSelectionExperiments/answer/dev-less-than-40.manual-edit.xml");
		    outFile = line.getOptionValue("output", "/tmp/answerfinder.dev.baseline.tagged");
		    onlyPositive = !line.hasOption("all");

//		    if ( line.hasOption("input") ) {
//		    } else {
//		    	formatter.printHelp( "edu.jhu.jacana.qa.AnswerFinder", options );
//		    	System.exit(-1);
//		    }
		}
		catch( ParseException exp ) {
		    System.out.println( "Unexpected exception:" + exp.getMessage() );
		}
		
		String id = null;
		// <QApairs id='32.1'>
		Pattern p = Pattern.compile("<QApairs id='(.*)'>");
		
		DependencyTree qTree = null;

		List<DependencyTree> aTreeList = new ArrayList<DependencyTree>();
		

		TYPE type = null;
		List<TYPE> types = new ArrayList<TYPE>();
		
		long time1 = (new Date()).getTime();
		int counter = 0;
		
		boolean hasPositive = false;
		
		try {
			BufferedReader in = FileManager.getReader(inFile);
			BufferedWriter out = FileManager.getWriter(outFile);

			String line, qWord;
			ArrayList<Integer> aWordList;
	
			
			while ((line = in.readLine()) != null) {
				if (line.startsWith("</QApairs")) {

					if (aTreeList.size() > 0 && hasPositive) {

						String qString = qTree.toCompactString("qRoot");

						LblTree qtree = LblTree.fromString(qString);
						qtree.setSentence(qTree.getSentence());

						qWord = QuestionWordExtractor.getQuestionWords(qTree);

						for (int i=0; i<aTreeList.size(); i++) {
							counter ++;
							if (counter % 200 == 0)
								System.out.println(counter);

							DependencyTree aTree = aTreeList.get(i);
							//System.out.println(aTree.getSentence());
							
							String aString = aTree.toCompactString("aRoot");
							LblTree atree = LblTree.fromString(aString);
							atree.setSentence(aTree.getSentence());
							EditDistQuestion dist = new EditDistQuestion(true);
							// MUST be from qtree to atree as EditDistQuestion
							// assums qtree as tree 1 and atree as tree 2
							dist.treeDist(qtree, atree);
							dist.printHumaneEditScript();
							aWordList = dist.getAWordList();
							System.out.println(dist.getAWord());
							
						
							StringBuilder sb = new StringBuilder();
							sb.append(String.format("PAIR%d\t%s\n", counter, qWord));
							sb.append(String.format("Q:\t%s\t%s\n", id, qTree.getSentence()));
							boolean correct = true;
							String[] predicted = new String[aTree.getAnswers().size()];
							Arrays.fill(predicted, "O");
							if (aWordList != null) {
								for (int m:aWordList) {
									predicted[m] = "ANSWER-I";
								}
								if (aWordList.size() > 0)
									predicted[aWordList.get(0)] = "ANSWER-B";
							}

							String[] words = aTree.getSentence().split("\\s+");
							for (int m=0; m<words.length; m++) {
								sb.append(String.format("%s\t%s:1\t%s\n", aTree.getAnswers().get(m),
										predicted[m], words[m]));
								if (!aTree.getAnswers().get(m).equals(predicted[m]))
									correct = false;
							}
							if (correct)
								sb.append("RIGHT\n");
							else
								sb.append("WRONG\n");

							sb.append("\n");
							out.write(sb.toString());
						}
						
					}
					aTreeList.clear();
					types.clear();
					hasPositive = false;
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
				    String ner_line = in.readLine();
				    String ans_line = null;
				    if (type == TYPE.POSITIVE) {
				    	in.readLine(); 
				    	ans_line = in.readLine(); 
				    }
	
				    //System.out.println(line);
				    DependencyTree tree = MstReader.read(line, pos_line, lab_line, deps_line, ner_line, ans_line, false);
				    switch (type) {
				    case QUESTION: qTree = tree; break;
				    case POSITIVE: aTreeList.add(tree); types.add(type); hasPositive = true; break;
				    case NEGATIVE: if (!onlyPositive) aTreeList.add(tree); types.add(type); break;
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
