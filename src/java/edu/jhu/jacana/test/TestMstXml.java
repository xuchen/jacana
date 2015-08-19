/**
 * 
 */
package edu.jhu.jacana.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.reader.MstReader;
import edu.jhu.jacana.util.FileManager;


/**
 * This class reads in the pseudo-xml files and checks whether it's valid or not .
 * @author Xuchen Yao
 *
 */
public class TestMstXml {

	private enum TYPE {QUESTION, POSITIVE, NEGATIVE}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inFile = null;

		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("i", "input", true, "input pseudo-xml file");
		
		try {
		    CommandLine line = parser.parse( options, args );
		    
		    //inFile = line.getOptionValue("input", "tree-edit-data/answerSelectionExperiments/answer/dev-less-than-40.manual-edit.xml");
		    inFile = line.getOptionValue("input", "tree-edit-data/answerSelectionExperiments/answer/train2393.cleanup.xml.gz");
		}
		catch( ParseException exp ) {
		    System.out.println( "Unexpected exception:" + exp.getMessage() );
		}
		
		String id = null;
		// <QApairs id='32.1'>
		Pattern p = Pattern.compile("<QApairs id='(.*)'>");
		
		TYPE type = null;
		
		long time1 = (new Date()).getTime();
		int counter = 0, line_num=0;
		
		
		try {
			BufferedReader in = FileManager.getReader(inFile);

			String line;
			
			
			while ((line = in.readLine()) != null) {
				line_num += 1;
				if (line.startsWith("</QApairs")) {

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
					counter ++;
					if (counter % 1000 == 0)
						System.out.println(counter);
				    String pos_line = in.readLine(); line_num += 1;
				    String lab_line = in.readLine(); line_num += 1;
				    String deps_line = in.readLine(); line_num += 1;
				    String ner_line = in.readLine(); line_num += 1;
				    String ans_line = null;
				    if (type == TYPE.POSITIVE) {
				    	in.readLine(); line_num += 1;
				    	ans_line = in.readLine();  line_num += 1;
				    }
	
				    try {
					    DependencyTree tree = MstReader.read(line, pos_line, lab_line, deps_line, ner_line, ans_line, null, false, true);
				    } catch (Exception e) {
				    	e.printStackTrace();
				        System.err.println(String.format("line %d, QApair %s", line_num, id));
				        System.err.println(line);
				    }
				}
			}
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long time2 = (new Date()).getTime();
		double duration = (time2-time1)/1000.0;
		System.out.println(String.format("runtime: %.1fs, %d instances, %.1fms/instance", duration, counter, duration*1000/counter));


	}

}
