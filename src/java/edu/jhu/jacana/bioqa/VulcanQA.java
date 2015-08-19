/**
 * 
 */
package edu.jhu.jacana.bioqa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.ml.CRFSuiteWrapper;
import edu.jhu.jacana.nlp.LingPipeAuraNER;
import edu.jhu.jacana.nlp.StanfordCore;
import edu.jhu.jacana.qa.AnswerVoter;
import edu.jhu.jacana.qa.feature.*;
import edu.jhu.jacana.qa.feature.template.TemplateExpander;
import edu.jhu.jacana.qa.questionanalysis.QuestionFeature;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.FileManager;
import edu.stanford.nlp.util.Pair;

/**
 * The main class for answering biology questions.
 * @author Xuchen Yao
 *
 */
public class VulcanQA {

	private static Logger logger = Logger.getLogger(VulcanQA.class.getName());

	public static void printUsage() {
		System.out.println("======== Usage =========");
		System.out.println("jacana bioqa accepts file input, if not presented,\n" +
				"then accepts input from stdio, and outputs to stdout");
		System.out.println("1. java -jar jacana-bioqa.jar [crf model file] [--force] input.xml output.xml");
		System.out.println("2. java -jar jacana-bioqa.jar [crf model file] [--force] < input.xml > output.xml");
		System.out.println("3. cat input.xml | java -jar jacana-bioqa.jar [--force] > output.xml");
		System.out.println("4. java -jar jacana-bioqa.jar [crf model file] [--force] --server:4444");
		System.out.println("\tThen send raw input.xml via socket (default address is localhost:4444)");
		System.out.println("\t[crf model file] defaults at resources/model/bio.crfsuite.model");
		//System.out.println("\tcurl http://localhost:4444 --data-binary @input.xml");
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws XMLStreamException, IOException {

		if (args.length > 0 && (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help")
				|| args[0].equalsIgnoreCase("--help"))) {
			printUsage();
			System.exit(0);
		}
		CRFSuiteWrapper crf = null;
		
		boolean force = false;
		
		List<String> list = new ArrayList<String>(Arrays.asList(args));
		// don't really work since the 'force' option isn't coded in the voter.
		force = list.remove("--force");
		args = list.toArray(new String[list.size()]);
		
		if (args.length > 0) {
			File model = new File(args[0]);
			if (model.exists() && !args[0].endsWith(".xml")) {
				crf = new CRFSuiteWrapper(args[0]);
				String[] oldargs = args;
				args = new String[oldargs.length-1];
				System.arraycopy(oldargs, 1, args, 0, oldargs.length-1);
			}
		}
		if (crf == null)
			crf = new CRFSuiteWrapper(FileManager.getResource("resources/model/bio.crfsuite.model"));
		//CRFSuiteWrapper crf = new CRFSuiteWrapper("resources"+File.separator+"model"+File.separator+"bio.crfsuite.model");
		VulcanOutputWriter output = null;
		boolean debug = false;
		boolean server = false;
		int port = 4444;
		ServerSocket serverSocket = null;
		Socket clientSocket = null;

		StanfordCore.init();
		LingPipeAuraNER.init();

		Iterator<VulcanInputInstance>  reader = null;
		if (args.length > 0 || debug) {
			String input = null;
			if (debug) {
				input = "bio-data/test.xml";
				input = "bio-data/dataset-one-train-sentences-nokb.xml";
			} else {
				if (args[0].startsWith("--server") || args[0].startsWith("-server")) {
					server = true;
					String[] splits = args[0].split(":");
					if (splits.length > 1) {
						port = Integer.parseInt(splits[1]);
					}
				} else {
					input = args[0];
				}
			}
			if (server) {
				try {
					serverSocket = new ServerSocket(port);
					logger.info(String.format("Server's up: localhost:%d (%s)", port, InetAddress.getLocalHost().toString()));
				} 
				catch (IOException e) {
					logger.severe("Could not listen on port: " + port);
					e.printStackTrace();
					System.exit(-1);
				}

				try {
					clientSocket = serverSocket.accept();
				} 
				catch (IOException e) {
					logger.severe("Accept failed: 4444");
					e.printStackTrace();
					System.exit(-1);
				}
				//reader = new VulcanInputReader(clientSocket.getInputStream());

			} else {
				reader = new VulcanInputReader(FileManager.getFileInputStream(FileManager.getFile(input)));
			}
		} else {
			reader = new VulcanInputReader(System.in);
		}


		if (args.length > 1 || debug) {
			String outputFilename;
			if (debug)
				outputFilename = "/tmp/output.xml";
			else {
				outputFilename = args[1];
				output = new VulcanOutputWriter(new FileOutputStream(outputFilename));
			}
		} else {
			if (server) {
				//output = new VulcanOutputWriter(clientSocket.getOutputStream());
			} else  {
				// stdout
				output = new VulcanOutputWriter(null);
			}
		}

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

		AbstractFeatureExtractor[] editExtractors = new AbstractFeatureExtractor[] {
				new EditFeature(),
				new NearestDistanceToAlignmentFeature(),
				//new WordnetQWordFeature(),
				//new WordnetFeature(),
				//new ModifierOfAlignmentFeature(),
				//new NearestDistanceTreeToAlignmentFeature(),
				//new OverlapFeature(),
		};

		String[][] features = new String[extractors.length + editExtractors.length][];

		// this is a hack, intended only for the server mode. If VulcanQA is run in 
		// non-server mode, the code will break the server_loop later.
		server_loop:
			while (true) {
				if (server) {
					// initialize a new input and output, otherwise xml parsing
					// doesn't work correctly.
					reader = new VulcanInputReader(clientSocket.getInputStream());
					output = new VulcanOutputWriter(clientSocket.getOutputStream());
					
					// the following is used to detect whether the client side has closed its connection.
					// unfortunatlly, none worked.
//					System.out.println(clientSocket.isBound());
//					System.out.println(clientSocket.isClosed());
//					System.out.println(clientSocket.isConnected());
//					System.out.println(clientSocket.isInputShutdown());
//					System.out.println(clientSocket.isOutputShutdown());
//					PrintWriter check_out = new PrintWriter(clientSocket.getOutputStream(), true);
//					check_out.println();
//					if (check_out.checkError())
//						break server_loop;
				}
				while (reader.hasNext()) {
					VulcanInputInstance inputIns = reader.next();
					DependencyTree qTree = MstReader.read(inputIns.qInstance, false);
					String qString = qTree.toCompactString("qRoot");

					LblTree qtree = LblTree.fromString(qString);
					qtree.setSentence(qTree.getSentence());

					StringBuilder sb = new StringBuilder();
					List<String> ref = new ArrayList<String>();

					for (VulcanInputInstance.AnalyzedInstance ansIns:inputIns.answerInstances) {
						// safety check
						if (ansIns.deps_line.equals("null")) continue;
						DependencyTree aTree = MstReader.read(ansIns, false);

						String aString = aTree.toCompactString("aRoot");
						LblTree atree = LblTree.fromString(aString);
						atree.setSentence(aTree.getSentence());
						EditDist dist;
						dist = new EditDist(true);
						dist.treeDist(atree, qtree);
						dist.printHumaneEditScript();


						String[] questionFeatures = QuestionFeature.extract(qTree);
						String[] wnFeatures = null;

						for (int j=0; j<extractors.length; j++) {
							AbstractFeatureExtractor ex = extractors[j];
							if (ex instanceof DepFeature)
								features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_TWO);
							//								else if (ex instanceof QuoteFeature)
							//									features[j] = ex.extract(dist, qTree, aTree, null, questionFeatures);
							else
								features[j] = ex.extract(dist, qTree, aTree, TemplateExpander.LEFT_RIGHT_BY_TWO, questionFeatures, wnFeatures);
						}

						for (int j=extractors.length; j<features.length; j++) {
							AbstractFeatureExtractor ex = editExtractors[j-extractors.length];
							//features[j] = ex.extract(dist, qTree, aTree, null, questionFeatures);
							features[j] = ex.extractSingleFeature(dist, qTree, aTree);
						}

						String[] words =aTree.getSentence().split("\\s+");
						//refSb.append(qWord+"\n");
						ref.add(qTree.getSentence());
						for (String w:words)
							ref.add(w);
						ref.add("");

						for (int k=0; k<aTree.getSize(); k++) {
							sb.append("O\t");
							for (int m=0; m<features.length; m++) {
								sb.append(features[m][k]);
								if (m != features.length-1)
									sb.append("\t");
							}
							//sb.append(questionMatchFeatures[k]);
							sb.append("\n");
						}
						sb.append("\n");
					}
					List<String> results = crf.classifyFeatures(sb.toString());
					//System.out.println(results);
					//System.out.println(ref);
					Pair<List<Pair<String, Double>>, HashMap<String, Set<Integer>>> ansPair = AnswerVoter.getVotes(results, ref, false, force);
					List<Pair<String, Double>> answers = ansPair.first();
					HashMap<String, Set<Integer>> ans2sentIdx = ansPair.second();
					//System.out.println(answers);
					output.writeInstance(inputIns, answers, ans2sentIdx, inputIns.answerInstances);
				}
				System.out.flush();
				output.closing();
				logger.info("Done answering all questions");
				if (server) {
					//output.close();
				} else {
					output.close();
					break server_loop;
				}
			}
			if (server) {
				output.close();
				serverSocket.close();
			}
	}

}
