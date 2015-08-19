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
 * a pattern file with answers.
 * @author Xuchen Yao
 *
 */
public class Raw2MstPattern {
	
	// Earth is changing rapidly as a result of human actions (pp. 1254-1260) Agriculture removes plant nutrients from ecosystems, so large supplements are usually required.
	// if next after (pp. ..) starts with a capital letter, then break it
	// 8.4 Enzymes speed up metabolic reactions by lowering energy barriers (pp. 152-157)
	// Figure 6.18 The chloroplast, site of photosynthesis. (a) Many plants have disk-shaped choloroplasts, as shown here.
	// Since the cell sap has a lower water potential than that of the solution outside the living cell, water enters the cell by osmosis (endosmosis). the partially permeable membrane here is the plasma membrane and not the cellulose cell wall.
	// If the illuminated molecule exists in isolation, its excited electron immediately drops back down to the ground-state orbital, and its excess energy is given off as heat and fluorescence (light). (b) A chlorophyll solution excited with ultraviolet light fluoresces with a red-orange glow.
	// Figure 4.6 The role of hydrocarbons in fats. (a) Mammalian adipose cells stockpile fat molecules as a fuel reserve.
	public static String process(String line) {
		return process(line, null);
	}
	
	public static String process(String line, String answer) {
		String word_line, pos_line, lab_line, deps_line, ner_line, mstString;
		Pair<String, String> pair;
		Annotation document = StanfordCore.process(line);
		List<CoreMap> sentences = StanfordCore.getSentences(document);
		if (sentences.size() > 1) {
			System.err.println("Multiple sentences: " + line);
			System.err.println("Modify the source to make it contain only one sentence per QA pair.");
			return null;
		} else {
			pair = StanfordCore.getTokenAndPosInOneLine(sentences.get(0));
			word_line = pair.first();
			pos_line = pair.second();
			pair =  StanfordCore.getDepAndIndexInOneLine(sentences.get(0));
			lab_line = pair.first();
			deps_line = pair.second();
			ner_line = LingPipeAuraNER.getNERsInOneLine(word_line);
			mstString = word_line+"\n"+pos_line+"\n"+lab_line+"\n"+deps_line+"\n"+ner_line;
			if (answer != null) {
				String ans_line, ans_idx_line;
				answer = answer.toLowerCase().replaceAll("\"", "");
				// add a final marker in case the answer is the last word and the splits fails
				word_line += "\t###";
				word_line = word_line.toLowerCase();
				
				// have to quote it otherwise plain string like "H+" would mean multiple H's
				// ( "+" is interpreted as a regex)
				// also have to add word boundary \b so that 'female' is not split by 'male'
				String[] splits = word_line.split("\\b"+Pattern.quote(answer)+"\\b");
				int ansLen = answer.split("\\s+").length;
				if (splits == null || splits.length <= 1) {
					System.err.println("Error: not found an answer in: " + line);
				} else {
					ans_line = answer;
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
					mstString += "\n" + ans_line + "\n" + ans_idx_line;
				}
			}
			return mstString;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//System.out.println(process("In  humans  ,   does    the male    or  the female  gamete  determine   gender  ?", "male"));
		//System.exit(-1);
		String rawFName = "bio-data/oct17-for-xuchen.txt";
		//String rawFName = "bio-data/test.txt";
		
		String mstFName = "bio-data/oct17-for-xuchen.xml";
		String patternFName = "bio-data/oct17-for-xuchen.pattern";
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
					
					for (int i=0; i<candidateList.size(); i++) {
						if(polarityList.get(i))
							mstWriter.write("<positive>\n");
						else
							mstWriter.write("<negative>\n");
						
						//mstWriter.write(candidateList.get(i)+"\n");
						mstWriter.write(candidateMstList.get(i)+"\n");
						if(polarityList.get(i))
							mstWriter.write("</positive>\n");
						else
							mstWriter.write("</negative>\n");
					}
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
				} else if (line.startsWith("A")) {
					// A   "Enzymes"   "Enzymes catalyze reactions"    "enzymes"   "enzyme"    "ribozyme" "catalyzing agent"
					answers.clear();
					for (int i=1; i<splits.length; i++) {
						answers.add(splits[i].replaceAll("\"", ""));
					}
				} else if (line.startsWith("S")) {
					if (splits[1].equals("-")) {
						// negative example
						// S       -   1.10.5.41.1 In both cases, only photosystem I is used.
						sent = splits[3];
						candidateList.add(sent);
						candidateMstList.add(process(sent));
						polarityList.add(false);
					} else {
						// positive example
						// S   "Light-Harvesting"  +   1.10.2.4.1  A Photosystem: A Reaction-Center Complex Associated with Light-Harvesting Complexes
						answer = splits[1];
						answers.add(answer);
						
						// tokenize it
						Annotation document = StanfordCore.process(answer);		
						List<CoreMap> sentences = StanfordCore.getSentences(document);
						Pair<String, String> pair;
						pair = StanfordCore.getTokenAndPosInOneLine(sentences.get(0));
						answer = pair.first();
						answers.add(answer);
						
						sent = splits[4];
						candidateList.add(sent);
						candidateMstList.add(process(sent, answer));
						polarityList.add(true);
					}
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

	}

}
