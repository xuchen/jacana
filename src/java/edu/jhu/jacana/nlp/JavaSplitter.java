/**
 * 
 */
package edu.jhu.jacana.nlp;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Word tokenizer and sentence splitter provided in Java 1.6
 * http://docs.oracle.com/javase/6/docs/api/java/text/BreakIterator.html
 * @author Xuchen Yao
 *
 */
public class JavaSplitter {
	
	 public static void printEachForward(BreakIterator boundary, String source) {
	     int start = boundary.first();
	     for (int end = boundary.next();
	          end != BreakIterator.DONE;
	          start = end, end = boundary.next()) {
	          System.out.println(source.substring(start,end));
	     }
	 }
	 
	 public static void printEachBackward(BreakIterator boundary, String source) {
	     int end = boundary.last();
	     for (int start = boundary.previous();
	          start != BreakIterator.DONE;
	          end = start, start = boundary.previous()) {
	         System.out.println(source.substring(start,end));
	     }
	 }
	 
	 public static void printFirst(BreakIterator boundary, String source) {
	     int start = boundary.first();
	     int end = boundary.next();
	     System.out.println(source.substring(start,end));
	 }
	 
	 public static void printLast(BreakIterator boundary, String source) {
	     int end = boundary.last();
	     int start = boundary.previous();
	     System.out.println(source.substring(start,end));
	 }
	 
	 
	 public static void printAt(BreakIterator boundary, int pos, String source) {
	     int end = boundary.following(pos);
	     int start = boundary.previous();
	     System.out.println(source.substring(start,end));
	 }
	 
	 
	 public static int nextWordStartAfter(int pos, String text) {
	     BreakIterator wb = BreakIterator.getWordInstance();
	     wb.setText(text);
	     int last = wb.following(pos);
	     int current = wb.next();
	     while (current != BreakIterator.DONE) {
	         for (int p = last; p < current; p++) {
	             if (Character.isLetter(text.codePointAt(p)))
	                 return last;
	         }
	         last = current;
	         current = wb.next();
	     }
	     return BreakIterator.DONE;
	 }
	 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        String stringToExamine = "What does the plasma membrane do? Why is it necessary? 7.1 Cellular membranes are fluid mosaics of lipids and proteins (pp. 125-131) The Davson-Danielli sandwich model of the membrane has been replaced by the fluid mosaic model, in which amphipathic proteins are embedded in the phospholipid bilayer.";
        //print each word in order
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(stringToExamine);
        printEachForward(boundary, stringToExamine);
        //print each sentence in reverse order
        boundary = BreakIterator.getSentenceInstance(Locale.US);
        boundary.setText(stringToExamine);
        printEachBackward(boundary, stringToExamine);
        //printFirst(boundary, stringToExamine);
        //printLast(boundary, stringToExamine);

	}

}
