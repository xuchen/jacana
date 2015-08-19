/**
 * 
 */
package edu.jhu.jacana.bioqa;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import edu.jhu.jacana.bioqa.VulcanInputInstance.AnalyzedInstance;
import edu.stanford.nlp.util.Pair;

/**
 * http://www.halotestframework.net:8008/tester
 * @author Xuchen Yao
 *
 */
public class VulcanOutputWriter {

	private static Logger logger = Logger.getLogger(VulcanOutputWriter.class.getName());
	private XMLStreamWriter xtw = null;
	boolean headerWritten = false;

	public void closing() throws XMLStreamException {
		if (xtw != null) {
			xtw.writeEndElement();
			xtw.writeEndDocument();
			xtw.flush();
		}
	}
	public void close() throws XMLStreamException {
		if (xtw != null) {
			xtw.close();
		}
	}
		
	public VulcanOutputWriter(OutputStream outputStream) throws XMLStreamException, IOException {
		XMLOutputFactory xof =  XMLOutputFactory.newInstance();
		if (outputStream!= null)
			// must specify encoding here otherwise Mac complains (default encoding is MacRoman, on Linux it's UTF8)
			xtw = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(outputStream, "UTF-8"));
		else
			xtw = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(System.out, "UTF-8"));

	}
	
	private void writeHeader() throws XMLStreamException {
		// defer writing header until writeInstance so we know what VulcanInputReader.datasetName is by then
		if (headerWritten) return;
		xtw.writeStartDocument("UTF-8","1.0");
		/*
<question-set>
  <name>Dataset one</name>
  <date>10/25/2012 16:56.16</date>
		 */
		xtw.writeStartElement("question-set");
		xtw.writeStartElement("name");
		xtw.writeCharacters(VulcanInputReader.datasetName);
		xtw.writeEndElement();
		xtw.writeStartElement("date");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		xtw.writeCharacters(dateFormat.format(date));
		xtw.writeEndElement();
		xtw.flush();
		headerWritten = true;
	}
	
	public void writeInstance(VulcanInputInstance inputIns, List<Pair<String, Double>> answers, HashMap<String, Set<Integer>> ans2sentIdx, List<AnalyzedInstance> answerInstances) throws XMLStreamException {
		writeHeader();
		/*
  <qa-pair>
    <qid>1</qid>
    <question>What checkpoint of the cell cycle does p53 work at?</question>
    <answers>
      <answer>
        <answer-phrase>G1/S</answer-phrase>
        <confidence>1.0</confidence>
      </answer>
      <answer>
        <answer-phrase>G2</answer-phrase>
        <confidence>1.0</confidence>
      </answer>
    </answers>
  </qa-pair>
		 */
		xtw.writeStartElement("qa-pair");
		xtw.writeStartElement("qid");
		xtw.writeCharacters(inputIns.qInstance.id);
		xtw.writeEndElement();
		xtw.writeStartElement("question");
		xtw.writeCharacters(inputIns.qInstance.word_line);
		xtw.writeEndElement();
		xtw.writeStartElement("answers");
		for (Pair<String,Double> ansVote:answers) {
			xtw.writeStartElement("answer");
			xtw.writeStartElement("answer-phrase");
			xtw.writeCharacters(ansVote.first());
			xtw.writeEndElement();
			xtw.writeStartElement("confidence");
			xtw.writeCharacters(ansVote.second().toString());
			xtw.writeEndElement();
			xtw.writeStartElement("sources");
			for (Integer sentIdx:ans2sentIdx.get(ansVote.first())) {
				xtw.writeStartElement("source");
				xtw.writeStartElement("sid");
				xtw.writeCharacters(answerInstances.get(sentIdx).id);
				xtw.writeEndElement();
				xtw.writeStartElement("rank");
				xtw.writeCharacters(answerInstances.get(sentIdx).rank);
				xtw.writeEndElement();
				xtw.writeStartElement("relevance");
				xtw.writeCharacters(answerInstances.get(sentIdx).relevance);
				xtw.writeEndElement();
				xtw.writeEndElement();
			}
			xtw.writeEndElement();
			xtw.writeEndElement();
		}
		xtw.writeEndElement();
		xtw.writeEndElement();
		xtw.flush();
	}
}
