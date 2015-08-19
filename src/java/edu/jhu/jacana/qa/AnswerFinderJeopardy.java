/**
 * 
 */
package edu.jhu.jacana.qa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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

import approxlib.distance.EditDistExternalAlign;
import approxlib.distance.EditDist;
import approxlib.distance.EditDistWordnet;
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
import edu.jhu.jacana.qa.questionanalysis.JeopardyClueFeature;
import edu.jhu.jacana.qa.questionanalysis.QuestionFeature;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;
import edu.jhu.jacana.reader.JeopardyMetaInfoJsonReader;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.FileManager;

/**
 * This class reads in the pseudo-xml files and outputs what the questions words align to.
 * For each question, if accompanied by multiple answer sentences, it outputs the alignment
 * in a ranked list, with each answer candidates voted by the number of supporting sentences.
 * 
 * This class uses pre-aligned pseudo-xml input, instead of aligning with TED on the fly.
 * Also, the data comes from Jeopardy. We used external annotation when analyzing questions (clues).
 * 
 * @author Xuchen Yao
 *
 */
public class AnswerFinderJeopardy {

	private enum TYPE {QUESTION, POSITIVE, NEGATIVE}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String inFile = null, outFile = null, refFile = null, editFile = null, metaFile = null;

		/**
		 * the ratio of positive/negative examples in training is about 3:17, this will severely
		 * Imbalance the model to prefer "O" rather than "ANSER-B" and "ANSWER-I". For now we
		 * enable using only positive examples. That is: only train on positive examples AND
		 * only test on positive examples
		 */
		boolean onlyPositive = true;
		boolean useTED = false;
		boolean baseline = false;
		int context = 2;
		
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("i", "input", true, "input pseudo-xml file");
		options.addOption("j", "jeopardy-meta", true, "jeopardy-meta .json file");
		options.addOption("o", "output", true, "output bnf feature file");
		options.addOption("r", "ref", true, "reference file for output");
		options.addOption("e", "edit", true, "edit sequence between q/a pairs");
		options.addOption("d", "ted", false, "use the internal TED aligner, don't use external align");
		options.addOption("b", "baseline", false, "baseline: no alignment-based features");
		options.addOption("a", "all", false, "process all sentences (both positives and negatives).\n"+
											"Default is false: only process positive ones.\n" +
											"Must be used when testing: you don't know the example is positive or negative.");
		options.addOption("c", "context", true, "context window size (default 2): 0, 1, or 2. 0 means only unigram features.");
		//HelpFormatter formatter = new HelpFormatter();
		

		try {
		    CommandLine line = parser.parse( options, args );
		    
		    inFile = line.getOptionValue("input", "/Volumes/Data1/xuchen/j-archive/jeopardy_sentences.aligned.meteor.test.xml");
		    outFile = line.getOptionValue("output", "/Users/xuchen/data1/xuchen/j-archive/jeopardy.test.bnf");
		    metaFile = line.getOptionValue("jeopardy-meta", "/Users/xuchen/data1/xuchen/j-archive/jeopardy.id2meta.json.gz");
		    refFile = line.getOptionValue("ref", null);
		    editFile = line.getOptionValue("edit", null);
		    onlyPositive = !line.hasOption("all");
		    useTED = line.hasOption("ted");
		    baseline = line.hasOption("baseline");
		    context = Integer.parseInt(line.getOptionValue("context", "2"));

//		    if ( line.hasOption("input") ) {
//		    } else {
//		    	formatter.printHelp( "edu.jhu.jacana.qa.AnswerFinder", options );
//		    	System.exit(-1);
//		    }
		}
		catch( ParseException exp ) {
		    System.out.println( "Unexpected exception:" + exp.getMessage() );
		}
		
		JeopardyMetaInfoJsonReader meta = new JeopardyMetaInfoJsonReader(metaFile);
		String id = null;
		// <QApairs id='32.1'>
		Pattern p = Pattern.compile("<QApairs id='(.*)'>");
		
		DependencyTree qTree = null;

		List<DependencyTree> aTreeList = new ArrayList<DependencyTree>();
		
		AbstractFeatureExtractor[] extractors = new AbstractFeatureExtractor[] {
				// ClassExtractor is for sanity check: it uses the true label as feature
				// so a classifier should learn perfectly (P/R/F1=1/1/1) on this feature.
				//new ClassExtractor(),
				new PosFeature(),
				new NerFeature(),
				new DepFeature(),
				new QuoteFeature(),
				//new WordnetTagFeature(),
				//new WordnetHypernymFeature(),
				//new WordnetHypernymDepFeature(),
				//new WordnetHypernymPosFeature(),
				//new WordnetQWordFeature(),
		};
		
		AbstractFeatureExtractor[] editExtractors = null;
		
		if (baseline) {
			editExtractors = new AbstractFeatureExtractor[] {};
		} else {
			editExtractors = new AbstractFeatureExtractor[] {
				new EditFeature(),
				new NearestDistanceToAlignmentFeature(),
				//new WordnetQWordFeature(),
				//new WordnetFeature(),
				//new ModifierOfAlignmentFeature(),
				//new NearestDistanceTreeToAlignmentFeature(),
				//new OverlapFeature(),
			};
		}
		
		WordnetFeature wnFeature = new WordnetFeature();
		
		String[][] features = new String[extractors.length + editExtractors.length][];
		
		ClassExtractor classExtractor = new ClassExtractor(); 


		TYPE type = null;
		List<TYPE> types = new ArrayList<TYPE>();
		
		long time1 = (new Date()).getTime();
		int counter = 0;
		
		boolean hasPositive = false;
		
		try {
			BufferedReader in = FileManager.getReader(inFile);
			BufferedWriter out = FileManager.getWriter(outFile);
			BufferedWriter ref = (refFile==null? null:FileManager.getWriter(refFile));
			BufferedWriter edit = (editFile==null? null:FileManager.getWriter(editFile));

			String line, qWord;
			
			int line_num = 0;
			
			while ((line = in.readLine()) != null) {
				line_num += 1;
				// System.out.println(" line number: " + line_num + " " + id);
				if (line.startsWith("</QApairs")) {

					if (aTreeList.size() > 0 && (hasPositive || !onlyPositive)) {
					//if (aTreeList.size() > 0 && hasPositive) {

						LblTree qtree = null;
						if (useTED) {
							try { 
								// System.out.println(qTree.getSentence());
								String qString = qTree.toCompactString("qRoot");
								qtree = LblTree.fromString(qString);
								qtree.setSentence(qTree.getSentence());
							} catch (Exception e) {
								System.err.println(String.format("toCompactString error in qTree at line %d, QApair %s", line_num, id));
								System.err.println(line);
								aTreeList.clear();
								types.clear();
								hasPositive = false;
								e.printStackTrace();
								continue;
							}
						}

						if (edit != null) {
							edit.write(qTree.getSentence());
							edit.write("\n============================\n");
						}
						// qWord = QuestionWordExtractor.getQuestionWords(qTree);
						qWord = meta.getFocus(id);
						//System.out.println(qTree.getSentence());

						for (int i=0; i<aTreeList.size(); i++) {
							counter ++;
							if (counter % 200 == 0)
								System.out.println(counter+" line number: " + line_num);

							DependencyTree aTree = aTreeList.get(i);
							//System.out.println(aTree.getSentence());
							
							EditDist dist;
							if (useTED) {
								LblTree atree = null;
								try {
									// System.out.println(aTree.getSentence());
									String aString = aTree.toCompactString("aRoot");
									atree = LblTree.fromString(aString);
									atree.setSentence(aTree.getSentence());
								} catch (Exception e) {
									System.err.println(String.format("toCompactString error in aTree at line %d, QApair %s", line_num, id));
									System.err.println(line);
									e.printStackTrace();
									continue;
								}

								dist = new EditDistWordnet(true, false);
								dist.treeDist(atree, qtree);
							} else {
								dist = new EditDistExternalAlign (true);
								((EditDistExternalAlign)dist).treeDist(aTree, qTree);
							}

							// EditDistExternalAlign dist;
							
							try { 
								dist.printEditScript();
							} catch (Exception e) {
								System.err.println(String.format("line %d, QApair %s", line_num, id));
								System.err.println(line);
								e.printStackTrace();
								continue;
							}
							
							// String[] questionFeatures = QuestionFeature.extract(qTree);
							String[] questionFeatures = JeopardyClueFeature.extract(qTree, meta, id);

							String[] wnFeatures = null;
							
							for (int j=0; j<extractors.length; j++) {
								AbstractFeatureExtractor ex = extractors[j];
								if (ex instanceof DepFeature) {
									if (context == 0)
										features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_ZERO);
									else if (context == 1)
										features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_ONE);
									else
										features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_TWO);
								}
//								else if (ex instanceof QuoteFeature)
//									features[j] = ex.extract(dist, qTree, aTree, null, questionFeatures);
								else { 
									if (context == 0)
										features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_ZERO, questionFeatures, wnFeatures);
									else if (context == 1)
										features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_ONE, questionFeatures, wnFeatures);
									else
										features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_TWO, questionFeatures, wnFeatures);
								}
							}
							
							if (!baseline) {
								for (int j=extractors.length; j<features.length; j++) {
									AbstractFeatureExtractor ex = editExtractors[j-extractors.length];
									//features[j] = ex.extract(dist, qTree, aTree, null, questionFeatures);
									features[j] = ex.extractSingleFeature(dist, qTree, aTree);
								}
							}
							
							//String[] questionMatchFeatures = QuestionMatchFeature.extract(features, qTree);
							
							String[] classes = classExtractor.extract(dist, qTree, aTree, null);
							
							if (ref != null) {
								String[] words =aTree.getSentence().split("\\s+");
								ref.write(qWord+"\n");
								ref.write(id+"\t"+qTree.getSentence()+"\n");
								for (String w:words)
									ref.write(w+"\n");
								ref.write("\n");
							}
						
							StringBuilder sb = new StringBuilder();
							for (int k=0; k<classes.length; k++) {
								sb.append(classes[k] + "\t");
								for (int m=0; m<features.length; m++) {
									sb.append(features[m][k]);
									if (m != features.length-1)
										sb.append("\t");
								}
								//sb.append(questionMatchFeatures[k]);
								sb.append("\n");
							}
							sb.append("\n");
							out.write(sb.toString());
						}
						
					}
					aTreeList.clear();
					types.clear();
					hasPositive = false;
				} else if (line.startsWith("<question")) {
					type = TYPE.QUESTION;
				} else if (line.startsWith("<positive")) {
					type = TYPE.POSITIVE;
				} else if (line.startsWith("<negative")) {
					type = TYPE.NEGATIVE;
				} else if (line.startsWith("<QApairs")) {
					Matcher m = p.matcher(line);
					m.matches();
					id = m.group(1);
				} else if (line.startsWith("</question>") ||
						line.startsWith("</positive>") || line.startsWith("</negative>") ) {
					//do nothing
				} else {
				    String pos_line = in.readLine(); line_num += 1;
				    String lab_line = in.readLine(); line_num += 1;
				    String deps_line = in.readLine(); line_num += 1;
				    String ner_line = in.readLine(); line_num += 1;
				    String ans_line = null;
				    String align_line = null;
				    if (type == TYPE.POSITIVE) {
				    	in.readLine(); line_num += 1;
				    	ans_line = in.readLine(); 
				    }
				    if (type == TYPE.POSITIVE || type == TYPE.NEGATIVE) {
				        align_line = in.readLine(); line_num += 1;
				    }
				    
				    if (useTED && !MstReader.validDepLabelLine(lab_line)) {
				    	continue;
				    }
	
				    //System.out.println(line);
				    DependencyTree tree;
				    try {
						if (useTED) {
							tree = MstReader.read(line, pos_line, lab_line, deps_line, ner_line, ans_line, false);
						} else {
							tree = MstReader.read(line, pos_line, lab_line, deps_line, ner_line, ans_line, align_line, false);
						}
				    } catch (Exception e) {
				        System.err.println(String.format("line %d, QApair %s", line_num, id));
				        System.err.println(line);
				    	e.printStackTrace();
				        continue;
				    }
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
			if (ref != null) ref.close();
			if (edit != null) edit.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long time2 = (new Date()).getTime();
		double duration = (time2-time1)/1000.0;
		System.out.println(String.format("runtime: %.1fs, %d instances, %.1fms/instance", duration, counter, duration*1000/counter));


	}


}
