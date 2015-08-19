/**
 * 
 */
package edu.jhu.jacana.bioqa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import edu.jhu.jacana.nlp.LingPipeAuraNER;
import edu.jhu.jacana.nlp.StanfordCore;
import edu.jhu.jacana.util.FileManager;
import edu.jhu.jacana.util.StringUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

/**
 * Convert a raw file with QA pairs to a standard MST file and
 * a pattern file with answers. The second batch of files only have answer specified for each
 * question, but not for each retrieved sentence. So we need to match the answers. 
 * @author Xuchen Yao
 *
 */
public class Raw2MstPatternWithAnswerMatching {
	
	public static String process(String line) {
		return process(line, null).second();
	}
	
	public static Pair<Boolean, String> process(String line, Set<String> answers) {
		String word_line, pos_line, lab_line, deps_line, ner_line, mstString = "";
		Pair<String, String> pair;
		Annotation document = StanfordCore.process(line);
		List<CoreMap> sentences = StanfordCore.getSentences(document);
		boolean containsAnswer = false;
		if (sentences.size() > 1) {
			System.err.println("Multiple sentences: " + line);
			System.err.println("Modify the source to make it contain only one sentence per QA pair.");
			return new Pair<Boolean, String>(containsAnswer, mstString);
		} else {
			pair = StanfordCore.getTokenAndPosInOneLine(sentences.get(0));
			word_line = pair.first();
			pos_line = pair.second();
			pair =  StanfordCore.getDepAndIndexInOneLine(sentences.get(0));
			lab_line = pair.first();
			deps_line = pair.second();
			ner_line = LingPipeAuraNER.getNERsInOneLine(word_line);
			mstString = word_line+"\n"+pos_line+"\n"+lab_line+"\n"+deps_line+"\n"+ner_line;
			if (answers != null && answers.size() > 0) {
				String ans_line = "", ans_idx_line = "";
				// add a final marker in case the answer is the last word and the splits fails
				word_line += "\t###";
				word_line = word_line.toLowerCase();
				
				
				for (String answer:answers) {
					answer = answer.toLowerCase().replaceAll("\"", "");
					
					// have to quote it otherwise plain string like "H+" would mean multiple H's
					// ( "+" is interpreted as a regex)
					// also have to add word boundary \b so that 'female' is not split by 'male'
					String[] splits = word_line.split("\\b"+Pattern.quote(answer)+"\\b");
					int ansLen = answer.split("\\s+").length;
					if (splits == null || splits.length <= 1) {
						;
					} else {
						ans_line += answer;
						List<String> indices = new ArrayList<String>();
						int start = 0;
						for (int i=0; i<splits.length-1; i++) {
							start += splits[i].trim().split("\t").length + 1;
							// special case: answer starts the line
							if (splits[i].trim().length() == 0) start = 1;
							int end = start + ansLen;
							if (indices.size() > 0) {
								// multiple match
								indices.add("#");
								ans_line += "\tMULTIPLE";
							}
							for (int j=start; j<end; j++)
								indices.add(Integer.toString(j));
							start = end - 1;
						}
						ans_idx_line = StringUtils.join(indices, "\t");
					}
				}
				mstString += "\n" + ans_line + "\n" + ans_idx_line;
				if (ans_line.length() != 0)
					containsAnswer = true;
			}
			return new Pair<Boolean, String>(containsAnswer, mstString.trim());
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String rawFName = "bio-data/nov3-one-train-10sentences.txt";
		//String rawFName = "bio-data/test.txt";
		
		String mstFName = "bio-data/nov3-one-train-10sentences.xml";
		String patternFName = "bio-data/nov3-one-train-10sentences.pattern";
		int numQuestion = 0, numPositive = 0, numSentence = 0, numAnswered = 0;
		try {
			BufferedReader f = FileManager.getReader(rawFName);
			BufferedWriter mstWriter = FileManager.getWriter(mstFName);
			BufferedWriter patternWriter = FileManager.getWriter(patternFName);
			
			String line, pline = "", sent, answer, question = "", id = "", qMstString = "";
			int counter = 0;
			String[] splits;
			Set<String> answers = new HashSet<String>();
			List<String> candidateList = new ArrayList<String>();
			List<String> candidateMstList = new ArrayList<String>();
			List<Boolean> polarityList = new ArrayList<Boolean>();
			while ((line = f.readLine()) != null) {
				//System.err.println(line);
				if (line.length() == 0) {
					if (answers.size() > 0) {
						for (String s:answers) {
							patternWriter.write(String.format("%s %s\n", id, s.replaceAll("\t", "")));
						}
						mstWriter.write(String.format("<QApairs id='%s'>\n", id));
						mstWriter.write("<question>\n");
						//mstWriter.write(question+"\n");
						mstWriter.write(qMstString+"\n");
						mstWriter.write("</question>\n");
					}
					
					boolean answered = false;
					for (int i=0; i<candidateList.size(); i++) {
						if(polarityList.get(i))
							mstWriter.write("<positive>\n");
						else
							mstWriter.write("<negative>\n");
						
						//mstWriter.write(candidateList.get(i)+"\n");
						mstWriter.write(candidateMstList.get(i)+"\n");
						if(polarityList.get(i)) {
							mstWriter.write("</positive>\n");
							answered = true;
						}
						else
							mstWriter.write("</negative>\n");
					}
					if (answered)
						numAnswered += 1;
					mstWriter.write("</QApairs>\n");
					
					answers.clear();
					candidateList.clear();
					candidateMstList.clear();
					polarityList.clear();
					continue;
				}
				line = line.replaceAll("\"", "").trim();
				splits = line.split("\t+");
				if (line.startsWith("Q")) {
					if (answers.size() > 0) {
						System.err.println("Error: No empty line before a new question: " + line);
						System.exit(-1);
					}
					counter++;
					id = "cb"+counter;
					question = splits[1];
					qMstString = process(question);
					numQuestion += 1;
				} else if (line.startsWith("A")) {
					// A   "Enzymes"   "Enzymes catalyze reactions"    "enzymes"   "enzyme"    "ribozyme" "catalyzing agent"
					answers.clear();
					for (int i=1; i<splits.length; i++) {
						// tokenize it
						answer = splits[i].replaceAll("\"", "");
						Annotation document = StanfordCore.process(answer);		
						List<CoreMap> sentences = StanfordCore.getSentences(document);
						Pair<String, String> pair;
						pair = StanfordCore.getTokenAndPosInOneLine(sentences.get(0));
						answer = pair.first();
						answers.add(answer);
					}
				} else if (line.startsWith("S")) {
					// S           1.12.3.2.3.3    Animal cells generally have built-in stop ...
					numSentence += 1;
					sent = splits[2];
					candidateList.add(sent);
					Pair<Boolean, String> pair;
					pair = process(sent, answers);
					polarityList.add(pair.first());
					candidateMstList.add(pair.second());
					if (pair.first())
						numPositive += 1;
				}
				pline = line;
			}
			f.close();
			mstWriter.close();
			patternWriter.close();
			
			if (pline.length() == 0) {
				System.err.println("Error: the last line is not saved. Make sure there's an empty line at the end of your file");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(String.format("total questions: %d\n" +
				"total questions with an answer: %d (%.1f)\n" +
				"total sentences: %d\n" +
				"total positive sentences: %d (%.1f)\n" +
				"", numQuestion, numAnswered, numAnswered*1.0/numQuestion,
				numSentence, numPositive, numPositive*1.0/numSentence));
		
		//total questions: 520
		//total questions with an answer: 145 (0.3)
		//total sentences: 5189
		//total positive sentences: 413 (0.1)


	}

}
