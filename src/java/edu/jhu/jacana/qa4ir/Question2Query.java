/**
 * 
 */
package edu.jhu.jacana.qa4ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.nlp.StanfordCore;
import edu.jhu.jacana.qa.questionanalysis.QuestionFeature;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.FileManager;
import edu.jhu.jacana.util.MapUtil;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Xuchen Yao
 *
 */
public class Question2Query {

	private static Logger logger = Logger.getLogger(Question2Query.class.getName());
	
	static Pattern allPunct = Pattern.compile("^\\p{Punct}$");

	public static void printUsage() {
		System.out.println("======== Usage =========");
		System.out.println("Question2Query model.txt < in.questions > out.quries");
	}
	
	static boolean containsOnlyPunctuation (String s) {
		if (allPunct.matcher(s).matches())
			return true;
		else
			return false;
	}
	
	static String reformatPos (String s) {
		// keep consistent with the python reformat_pos function
		s = s.replaceAll("\\$", "DOLLAR");
		s = s.replaceAll(",", "COMMA");
		s = s.replaceAll("#", "POUND");
		s = s.replaceAll("``", "LQUOTE");
		s = s.replaceAll("''", "RQUOTE");
		s = s.replaceAll("\\.", "PERIOD");
		s = s.replaceAll(":", "COLON");
		return s;
	}
	
	/**
	 * for features like this: qword=what|ner[0]|ner[1]|ner[2]=B-GPE|O|B-FAC 
	 * extract only the field for [0]
	 * @param f feature from the CRF model
	 * @return the field for unigram feature, null if [0] not present or is punctuation
	 */
	public static String getUnigramFeature(String f) {
		String unigram = null;
		System.out.println(f);
		String[] splits = f.split("=");
		if (splits.length != 3) return null;
		String[] leftSplits = splits[1].split("\\|");
		String[] rightSplits = splits[2].split("\\|");
		// leftSplits: what ner[0] ner[1] ner[2]
		// rightSplits: B-GPE 0 B-FAC
		if (leftSplits.length != rightSplits.length+1) {
			logger.severe("Error: feature doesn't satisfy assumption, check your code: " + f);
			return null;
		}
		for (int i=1; i<leftSplits.length; i++) {
			if (leftSplits[i].endsWith("[0]")) {
				unigram = rightSplits[i-1];
				if (containsOnlyPunctuation(unigram) || unigram.equals("#colon#") || unigram.equals("#slash#") || unigram.equals("O")) {
					unigram = null;
					break;
				}
				// sth like ner-b-gpe
				//unigram = leftSplits[i].substring(0, 3)+"-"+reformatPos(rightSplits[i-1]);
				unigram = leftSplits[i].substring(0, 3)+"-"+unigram;
				break;
			}
		}
		
		return unigram;
	}


	/**
	 * This class takes in a question and a trained CRF model,
	 * then generate a (structured) query for each question according
	 * to the most weighted features for each question type from the model.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		/**
		 * All what questions: 54
All what questions with LAT: 54
All what questions with LAT found in training: 40
		 */
		// only use the top 5 feature, unless all feature weights are the same.
		int top = 5;

		if (args.length == 0 || (args.length > 0 && (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help")
				|| args[0].equalsIgnoreCase("--help")))) {
			printUsage();
			System.exit(0);
		}
		String crfModel = args[0];
		BufferedReader reader;
		BufferedWriter writer;
		
		if (args.length > 1) {
			reader = FileManager.getReader(args[1]);
			if (args.length > 2) {
				writer = FileManager.getWriter(args[2]);
			} else
				writer = new BufferedWriter(new OutputStreamWriter(System.out));
		} else {
			reader = new BufferedReader(new InputStreamReader(System.in));
			writer = new BufferedWriter(new OutputStreamWriter(System.out));
		}
		
		StanfordCore.init();
		String line;
		BufferedReader crfReader = FileManager.getReader(crfModel);
		boolean record = false;
		String feature, state, qword;
		float weight;
		String[] splits;
		HashMap<String, HashMap<String, Float>> qword2feature2weight = new HashMap<String, HashMap<String, Float>>();
		while ((line=crfReader.readLine()) != null) {
			if (line.startsWith("STATE_FEATURES"))
				record = true;
			if (record) {
				line = line.trim();
				if (line.startsWith("(0)")) {
					splits = line.split("\\s+");
					feature = splits[1];state=splits[3]; //weight=splits[4];
					if (feature.startsWith("qword") || feature.startsWith("what-lemma") &&
							state.startsWith("ANSWER")) {
						weight = Float.parseFloat(splits[4]);
						if (weight > 0){
							splits = feature.split("\\|");
							qword = splits[0];
							if (!qword2feature2weight.containsKey(qword)) {
								qword2feature2weight.put(qword, new HashMap<String, Float>());
							}
							qword2feature2weight.get(qword).put(feature, weight);
						}
					}
				}
			}
		}
		crfReader.close();
		
		Map<String, Map<String, Float>> qword2sorted_feature2weight = new HashMap<String, Map<String, Float>>();
		for (String q:qword2feature2weight.keySet()) {
			HashMap<String, Float> feature2weight = qword2feature2weight.get(q);
			qword2sorted_feature2weight.put(q, MapUtil.sortByDescendingValue(feature2weight));
//			System.out.println(q);
//			for (String q1:qword2sorted_feature2weight.get(q).keySet()){
//				System.out.println(q1 + "\t" + qword2sorted_feature2weight.get(q).get(q1));
//				break;
//			}
		}
		
		DependencyTree qTree;
		String[] qFeatures;
		float old_weight = 0;
		HashSet<String> posFeatures = new HashSet<String>();
		HashSet<String> nerFeatures = new HashSet<String>();
		String number;
		int all_what_q = 0, all_what_q_with_lat = 0, all_what_q_with_lat_in_feature = 0;
		while ((line=reader.readLine()) != null) {
			boolean is_what_q = false, has_lat_in_feature = false;
			splits = line.split("\\t");
			number = splits[0];
			line = splits[1];
			posFeatures.clear();
			nerFeatures.clear();
			logger.info(line+"\n");
			List<CoreMap> sentences = StanfordCore.getSentences(StanfordCore.process(line));
			if (sentences.size() > 1) {
				logger.warning("Multiple questions in one line, only using the first sentence:\n" + line);
			}
			qTree = MstReader.read(sentences.get(0), false);
			qFeatures = QuestionFeature.extract(qTree);
			StringBuffer sb = new StringBuffer();
			StringBuffer query = new StringBuffer();
			query.append("\t<query>\n");
			query.append(String.format("\t\t<number>%s</number>\n", number));
			
			//  add the original question with all punctuation replaced with space
			// query.append("\t\t<text>#combine("+line.replaceAll("\\p{Punct}", " "));
			// query.append(" #max(");
			
			// weighted annotation, we can either downweight it (<1) or upweight it (>1) since the base weight is 1.
			query.append("\t\t<text>#weight(1.0 "+line.replaceAll("\\p{Punct}", " ").trim().replaceAll("\\s+", " 1.0 "));
			query.append(" 0.1 #max(");
			for (String q:qFeatures) {
				if (q.startsWith("qword=what")) { all_what_q++; is_what_q = true; break; }
			}
			
			for (String q:qFeatures) {
				if (q.startsWith("what-lemma=")) { 
					all_what_q_with_lat++;  
					if (qword2sorted_feature2weight.containsKey(q)) {
						all_what_q_with_lat_in_feature++;
						has_lat_in_feature = true;
					}
					break; }
			}
			String unigram;
			for (String q:qFeatures) {
				if (q.startsWith("qword=") || q.startsWith("what-lemma=")) {
					logger.info(q+"\n");
					if (qword2sorted_feature2weight.containsKey(q)) {
						int counter = 0;
						for (String f:qword2sorted_feature2weight.get(q).keySet()) {
							weight = qword2sorted_feature2weight.get(q).get(f);
							sb.append(String.format("%s:%f\t", f, weight));
							unigram = getUnigramFeature(f);
							if (unigram != null && unigram.startsWith("pos"))
								posFeatures.add(unigram);
							else if (unigram != null && unigram.startsWith("ner"))
								nerFeatures.add(unigram);
							if (old_weight == 0 || (old_weight != 0 && weight != old_weight))
								counter++;
							if (counter>top) break;
							old_weight = weight;
						}
					}
				}
			}
			
			// commented out since in practice this didn't work well
			/* if (is_what_q && !has_lat_in_feature) {
				System.out.println("FALLLL BAAACK =====================================================================================");
				// fall back to POS feature since this what question has not a LAT in training
				for (String f:posFeatures) {
					query.append(" #any:"+f.toLowerCase());
				}
			} else*/ {
				// include only NER feature if there's one
				if (nerFeatures.size() > 0) {
					for (String f:nerFeatures) {
						query.append(" #any:"+f.toLowerCase());
					}
				} else {
					System.out.println("FALLLL BAAACK =====================================================================================");
					// otherwise fall back to POS feature
					for (String f:posFeatures) {
						query.append(" #any:"+f.toLowerCase());
					}
				}
			}
			query.append("))</text>\n");
			query.append(String.format("\t\t<!--%s-->\n", sb.toString()));
			query.append("\t</query>\n");
			logger.info(sb.toString()+"\n");
			logger.info("\n");
			writer.write(query.toString());
			writer.flush();
		}
		reader.close();
		
		System.out.println("All what questions: " + all_what_q);
		System.out.println("All what questions with LAT: " + all_what_q_with_lat);
		System.out.println("All what questions with LAT found in training: " + all_what_q_with_lat_in_feature);
	}

}
