/**
 * 
 */
package edu.jhu.jacana.bioqa;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.jhu.jacana.nlp.StanfordCore;
import edu.jhu.jacana.util.FileManager;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Xuchen Yao
 *
 */
public class VulcanInputEventReader implements /*Iterable<VulcanInputInstance>,*/ Iterator<VulcanInputInstance> {

	private static Logger logger = Logger.getLogger(VulcanInputEventReader.class.getName());

	private VulcanInputInstance nextInstance = null;
	private InputStream in;
	private XMLEventReader reader;

	// clean up the input string to make it easier to parse
	public static String cleanup (String str) {
		/*
(Figure 56.33a)
(see Chapter 53)
(Figure 56.33c, d)
(pp. 1244-1249)
(b)
(a)
(p. 265)
		 */
		// for now let's just remove all parenthesis and everything inside
		return str.replaceAll("\\(.*\\)", "");
	}

	public VulcanInputEventReader(InputStream in) throws XMLStreamException {
		this.in = in;
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		reader = inputFactory.createXMLEventReader(in, "UTF8");
	}

	// batch method
	public static List<VulcanInputInstance> readAll(InputStream in) throws XMLStreamException, IOException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader reader = inputFactory.createXMLEventReader(in);
		String content, id=null, rank=null, relevance=null, sentenceStr=null;
		VulcanInputInstance  inputInstance = null;
		List<VulcanInputInstance> insList = new ArrayList<VulcanInputInstance>();

		while (reader.hasNext()) {
			XMLEvent e = reader.nextEvent();
			if (e.isStartElement()) {
				StartElement start = e.asStartElement();
				String startName = start.getName().getLocalPart();  

				if (startName.equals("q-and-retrieved-sentences")) {
					//inputInstance = new VulcanInputInstance();
					content = e.asStartElement().getName().getLocalPart();
				} else if (startName.equals("qid")) {
					//e = reader.nextEvent();
					content = reader.getElementText();
					id = content;
				} else if (startName.equals("question")) {
					//e = reader.nextEvent();
					content = cleanup(reader.getElementText());
					List<CoreMap> sentences = StanfordCore.getSentences(StanfordCore.process(content));
					boolean foundAQuestion = false;
					if (sentences.size() > 1) {
						for (CoreMap sentence:sentences) {
							inputInstance = new VulcanInputInstance(sentence);
							if (inputInstance.qInstance.word_line.endsWith("?")) {
								foundAQuestion = true;
								break;
							}
						}
					} else if (sentences.size() == 1) {
						foundAQuestion = true;
						inputInstance = new VulcanInputInstance(sentences.get(0));
					}
					if (foundAQuestion) {
						inputInstance.qInstance.id = id;
					} else {
						logger.warning("Can't find a question from: "+content);
						logger.warning("Continue anyways...");
					}
				} else if (startName.equals("answer")) {
					//e = reader.nextEvent();
					content = cleanup(reader.getElementText());
					inputInstance.answerSet.add(content);
				} else if (startName.equals("sid")) {
					//e = reader.nextEvent();
					id = reader.getElementText();
				} else if (startName.equals("sentence-string")) {
					//e = reader.nextEvent();
					sentenceStr = cleanup(reader.getElementText());
				} else if (startName.equals("rank")) {
					//e = reader.nextEvent();
					rank = reader.getElementText();
				} else if (startName.equals("relevance")) {
					//e = reader.nextEvent();
					relevance = reader.getElementText();
				}
			} else if (e.isEndElement()) {
				EndElement end = e.asEndElement();
				String endName = end.getName().getLocalPart();  
				if (endName.equals("sentence")) {
					List<CoreMap> sentences = StanfordCore.getSentences(StanfordCore.process(sentenceStr));
					if (sentences.size() > 1) {
						logger.info("multiple sentences found in one line: " + sentenceStr);
					}
					for (CoreMap sentence:sentences) {
						VulcanInputInstance.AnalyzedInstance ansIns = inputInstance.new AnalyzedInstance(sentence);
						ansIns.id = id;
						ansIns.rank = rank;
						ansIns.relevance = relevance;
						inputInstance.answerInstances.add(ansIns);
					}
				} else if (endName.equals("q-and-retrieved-sentences")) {
					insList.add(inputInstance);
				}
			}
		}
		in.close();
		reader.close();
		return insList;
	}

	@Override
	public boolean hasNext() {

		String content, id=null, rank=null, relevance=null, sentenceStr=null;

		try {
			while (reader.hasNext()) {
				XMLEvent e;
				e = reader.nextEvent();
				if (e.isStartElement()) {
					StartElement start = e.asStartElement();
					String startName = start.getName().getLocalPart();  

					if (startName.equals("q-and-retrieved-sentences")) {
						//inputInstance = new VulcanInputInstance();
						content = e.asStartElement().getName().getLocalPart();
					} else if (startName.equals("qid")) {
						//e = reader.nextEvent();
						content = reader.getElementText();
						id = content;
					} else if (startName.equals("question")) {
						//e = reader.nextEvent();
						content = cleanup(reader.getElementText());
						logger.info("PARSING QUESTION: " + content);
						List<CoreMap> sentences = StanfordCore.getSentences(StanfordCore.process(content));
						boolean foundAQuestion = false;
						if (sentences.size() > 1) {
							for (CoreMap sentence:sentences) {
								nextInstance = new VulcanInputInstance(sentence);
								if (nextInstance.qInstance.word_line.endsWith("?")) {
									foundAQuestion = true;
									break;
								}
							}
						} else if (sentences.size() == 1) {
							foundAQuestion = true;
							nextInstance = new VulcanInputInstance(sentences.get(0));
						}
						if (foundAQuestion) {
							nextInstance.qInstance.id = id;
						} else {
							logger.warning("Can't find a question from: "+content);
							logger.warning("Continue anyways...");
						}
					} else if (startName.equals("answer")) {
						//e = reader.nextEvent();
						content = cleanup(reader.getElementText());
						nextInstance.answerSet.add(content);
					} else if (startName.equals("sid")) {
						//e = reader.nextEvent();
						id = reader.getElementText();
					} else if (startName.equals("sentence-string")) {
						//e = reader.nextEvent();
						sentenceStr = cleanup(reader.getElementText());
					} else if (startName.equals("rank")) {
						//e = reader.nextEvent();
						rank = reader.getElementText();
					} else if (startName.equals("relevance")) {
						//e = reader.nextEvent();
						relevance = reader.getElementText();
					}
				} else if (e.isEndElement()) {
					EndElement end = e.asEndElement();
					String endName = end.getName().getLocalPart();  
					if (endName.equals("sentence")) {
						logger.info("PARSING CANDIDATE: " + sentenceStr);
						List<CoreMap> sentences = StanfordCore.getSentences(StanfordCore.process(sentenceStr));
						if (sentences.size() > 1) {
							logger.info("multiple sentences found in one line: " + sentenceStr);
						}
						for (CoreMap sentence:sentences) {
							VulcanInputInstance.AnalyzedInstance ansIns = nextInstance.new AnalyzedInstance(sentence);
							ansIns.id = id;
							ansIns.rank = rank;
							ansIns.relevance = relevance;
							nextInstance.answerInstances.add(ansIns);
						}
					} else if (endName.equals("retrieved-sentences-set")) {
						nextInstance = null;
					} else if (endName.equals("q-and-retrieved-sentences")) {
						break;
					}
				}
			}
		} catch (XMLStreamException e1) {
			nextInstance = null;
		}
		if (nextInstance == null) {
			try {
				if (System.in != in) {
					in.close();
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}
			return false;
		} else
			return true;
	}

	@Override
	public VulcanInputInstance next() {
		return nextInstance;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/*
	@Override
	public Iterator<VulcanInputInstance> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	 */

	/**
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws XMLStreamException, IOException {
		//String input = "bio-data/dataset-one-train-sentences.xml";
		String input = "bio-data/test.xml";
		//readAll(FileManager.getFileInputStream(FileManager.getFile(input)));
		Iterator<VulcanInputInstance> reader = new VulcanInputEventReader(FileManager.getFileInputStream(FileManager.getFile(input)));
		while (reader.hasNext()) {
			VulcanInputInstance ins = reader.next();
			System.out.println(ins);
		}
	}

}
