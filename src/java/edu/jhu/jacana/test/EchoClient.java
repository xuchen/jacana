/**
 * 
 */
package edu.jhu.jacana.test;

import java.io.*;
import java.net.*;
/**
 * http://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
 * curl http://localhost:4444 --data-binary @in.txt
 * telnet localhost 4444
 * @author Xuchen Yao
 *
 */
public class EchoClient {
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        int port = 4444;
        boolean keepAlive = true;
        
        while (keepAlive) {
	        try {
	            serverSocket = new ServerSocket(port);
	            System.out.println(String.format("Server's up: localhost:%d (%s)", port, InetAddress.getLocalHost().toString()));
	        } 
	        catch (IOException e) {
	            System.out.println("Could not listen on port: " + port);
	            e.printStackTrace();
	            System.exit(-1);
	        }
	        
	        try {
	            clientSocket = serverSocket.accept();
	        } 
	        catch (IOException e) {
	            System.out.println("Accept failed: 4444");
	            e.printStackTrace();
	            System.exit(-1);
	        }
	        
	        out = new PrintWriter(clientSocket.getOutputStream(), true);
	        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        String inputLine, outputLine;
	        
	        while ((inputLine = in.readLine()) != null) {   
	        	outputLine = inputLine;
	            out.println("===SOCKET===:\t" + outputLine);
	            System.err.println("===INPUT===:\t" + inputLine);
	            System.err.flush();
	            if (inputLine.equals("Bye."))
		            break;
	            if (inputLine.equals("Destroy.")) {
	            	keepAlive = false;
		            break;
	            }
	        }
	
	
			out.close();
			in.close();
			serverSocket.close();
        }
    }
}