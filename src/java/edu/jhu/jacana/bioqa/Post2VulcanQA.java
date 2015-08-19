/**
 * 
 */
package edu.jhu.jacana.bioqa;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 * A small test tool trying to send xml files to VulcanQA via socket
 * (default is localhost:4444).
 
   A typical way to programmatically access the VulcanQA server is:
   1. establish a socket connection with the server at localhost:4444
   2. send the raw XML to the server.
           a) Note it must be raw XML (no POST, no header, no nothing) and the line should start with something like:
           <?xml version="1.0" encoding="UTF-8"?>
           b) Nothing (even space, due to the strict streaming XML processing) is allowed before <?xml version...
   3. get the output (in XML format) from the server
   4. if the output line equals </question-set>, then the server is done answering questions.
   5. wait for another input (probably from the web input click) and repeat from 2.
  
   If the client (i.e. your code) is done with sending XML inputs and decides to quit, jacana-bioqa quits
   with the following error (due to a bug in Java that there's no easy way to detect whether
   the remote client has closed or not):
    WARNING: ParseError at [row,col]:[1,1]
    Message: Premature end of file.
    javax.xml.stream.XMLStreamException: ParseError at [row,col]:[1,1]
    Message: Premature end of file.
        at com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next(XMLStreamReaderImpl.java:594)
        at edu.jhu.jacana.bioqa.VulcanInputReader.hasNext(VulcanInputReader.java:176)
        at edu.jhu.jacana.bioqa.VulcanQA.main(VulcanQA.java:197)
   This is totally normal. Currently I don't disable this message, since this msg can also come
   from sending the wrong XML format, which is a real error.
 * @author Xuchen Yao
 *
 */
public class Post2VulcanQA {

	private static Logger logger = Logger.getLogger(Post2VulcanQA.class.getName());

	public static void printUsage() {
		System.out.println("======== Usage =========");
		System.out.println("Post2VulcanQA XML-files");
		System.out.println("Post2VulcanQA --url localhost:4444 XML-files");
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		if (args.length == 0 || args.length > 0 && (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help")
				|| args[0].equalsIgnoreCase("--help"))) {
			printUsage();
			System.exit(0);
		}

		String host = "localhost";
		int port = 4444;

		int fileStart = 0;

		if (args[0].equals("--url")) {
			String[] splits = args[1].split(":");
			host = splits[0]; port = Integer.parseInt(splits[1]);
			fileStart = 2;
		}
		Socket socket;
		socket = new Socket(host, port);
		OutputStream out = socket.getOutputStream();
		BufferedReader in = new BufferedReader(new InputStreamReader( socket.getInputStream()));
		String inputLine;

		for (int i=fileStart; i<args.length; i++) {

			String fileName = args[i];
			System.out.println(fileName);
			InputStream is = new FileInputStream(fileName);
			byte[] buf = new byte[1024];
			int read = 0;
			while ( (read = is.read(buf) ) >= 0) {
				if (null != out) out.write(buf, 0, read);
			}
			is.close();
			if (null != out) {
				out.flush();
			}

			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				System.out.flush(); 
				if (inputLine.equals("</question-set>"))
					break;
			}
		}
		in.close();
		out.close();
		socket.close();
	}

}
