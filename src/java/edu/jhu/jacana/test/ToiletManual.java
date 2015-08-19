package edu.jhu.jacana.test;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * There are always some moments every day when 
 * you have to be exactly THERE and do exactly THAT.
 * But how can a hard-core Java programmer endure 
 * such boredom?? So I wrote an instruction manual 
 * --- for my toilet.
 * 
 * @author Xuchen Yao
 * @version 12/21/2012 (now you know what I did 
 * on the day when the world was supposed to end...)
 */
public class ToiletManual {
	static final int OH_YEAH_DONE_WITH_TODAY = 0;
	static final int OH_SHOOT_SHOOT_HIT_THE_FAN = -1;

	public static void main(String[] args) {
		OutputStream trash = System.out;
		
		System.load("paper"); // code won't run unless you have "paper"
		PrintStream toilet = new PrintStream(new BufferedOutputStream(trash /*you know what's "buffered" here*/));
		
		toilet.print("whatever you manage to have come out");
		toilet.flush(); // Don't you dare to forget about this...
		
		if (toilet.checkError()) {
			System.getProperty("plumber.phoneNum"); // Oops
			System.exit(OH_SHOOT_SHOOT_HIT_THE_FAN);
		}
		
		// Good-mannered sitters and coders always do this after they stand up and flush
		toilet.close();  
		
		System.exit(OH_YEAH_DONE_WITH_TODAY);
	}
}