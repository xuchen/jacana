package edu.jhu.jacana.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * A collection of utilities for string processing.
 * 
 * @author Nico Schlaefer
 * @version 2007-05-05
 */
// TODO use Levenstein distance to identify similar tokens
public class StringUtils {
	/**
	 * Fraction of words that must occur in both strings for
	 * <code>equalsIntersect()</code> to be true.
	 */
	private static final float INTERSECT_THRESH = 0.33f;
	
	/**
	 * Checks if the first array of tokens is a subset if the second array.
	 * 
	 * @param tokens1 token array 1
	 * @param tokens2 token array 2
	 * 
	 * @return true, iff ss1 is a subset of ss2
	 */
	private static boolean isSubset(String[] tokens1, String[] tokens2) {
		boolean exists;
		for (String token1 : tokens1) {
			exists = false;
			for (String token2 : tokens2)
				if (token1.equals(token2)) {
					exists = true;
					break;
				}
			
			if (!exists) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the tokens in the first string form a subset of the tokens in
	 * the second string.
	 * 
	 * @param s1 string 1
	 * @param s2 string 2
	 * @return true, iff the tokens in s1 are a subset of the tokens in s2
	 */
	public static boolean isSubset(String s1, String s2) {
		if (s1 == null) return true;
		if (s2 == null) return false;
		
		String[] tokens1 = s1.split(" ");
		String[] tokens2 = s2.split(" ");
		
		return isSubset(tokens1, tokens2);
	}

	
	/**
	 * Concatenates an array of strings, using the given delimiter.
	 * 
	 * @param ss array of strings
	 * @param delim delimiter
	 * @return concatenated string
	 */
	public static String join(String[] ss, String delim) {
		StringBuilder sb = new StringBuilder();
		
		if (ss.length > 0) sb.append(ss[0]);
		for (int i = 1; i < ss.length; i++) sb.append(delim + ss[i]);
		
		return sb.toString();
	}

	public static String join(String[] ss, String delim, int start, int end) {
		StringBuilder sb = new StringBuilder();
		
		if (ss.length > start && end > start) sb.append(ss[start]);
		for (int i = start; i < Math.min(ss.length, end); i++) sb.append(delim + ss[i]);
		
		return sb.toString();
	}
	
	public static <T> String join ( T[] ss, String delim) {
		StringBuilder sb = new StringBuilder();
		
		if (ss.length > 0) sb.append(ss[0]);
		for (int i = 1; i < ss.length; i++) sb.append(delim + ss[i]);
		
		return sb.toString();
	}
	
	public static String join(List<String> ss, String delim) {
		StringBuilder sb = new StringBuilder();
		
		if (ss.size() > 0) sb.append(ss.get(0));
		for (int i = 1; i < ss.size(); i++) sb.append(delim + ss.get(i));
		
		return sb.toString();
	}

	public static String join(List<String> ss, String delim, int start, int end) {
		StringBuilder sb = new StringBuilder();
		
		if (ss.size() > start && end > start) sb.append(ss.get(start));
		for (int i = start; i < Math.min(ss.size(), end); i++) sb.append(delim + ss.get(i));
		
		return sb.toString();
	}
	
	/**
	 * Concatenates an array of strings, using whitespaces as delimiters.
	 * 
	 * @param ss array of strings
	 * @return concatenated string
	 */
	public static String joinWithSpaces(String[] ss) {

		return join(ss, " ");
	}
	
	/**
	 * Concatenates an array of strings, using tabs as delimiters.
	 * 
	 * @param ss array of strings
	 * @return concatenated string
	 */
	public static String joinWithTabs(String[] ss) {

		return join(ss, "\t");
	}
	
	/**
	 * Repeats string <code>s</code> <code>n</code> times.
	 * 
	 * @param s a string
	 * @param n number of repetitions
	 */
	public static String repeat(String s, int n) {
		String repeated = "";
		
		for (int i = 0; i < n; i++) repeated += s;
		
		return repeated;
	}
	
	
	/**
	 * Compares two strings. The strings are considered equal, iff one of the
	 * strings is a subset of the other string, i.e. iff all the tokens in the
	 * one string also occur in the other string.
	 * 
	 * @param s1 string 1
	 * @param s2 string 2
	 * @return true, iff the strings are equal in the sense defined above 
	 */
	public static boolean equalsSubset(String s1, String s2) {
		return isSubset(s1, s2) || isSubset(s2, s1);
	}
	
	/**
	 * Compares two strings. The strings are considered equal, iff the number of
	 * words that occur in both strings over the total number of words is at
	 * least <code>INTERSECT_FRAC</code>.
	 * 
	 * @param s1 string 1
	 * @param s2 string 2
	 * @return true, iff the strings are equal in the sense defined above
	 */
	public static boolean equalsIntersect(String s1, String s2) {
		// tokenize both strings
		String[] tokens1 = s1.split(" ");
		String[] tokens2 = s2.split(" ");
		
		// number of common tokens and total number of tokens
		// (note that duplicates are not handled properly)
		int commonTokens = 0;
		int totalTokens = tokens2.length;
		for (String token1 : tokens1)
			for (String token2 : tokens2)
				if (token1.equals(token2)) commonTokens++; else totalTokens++;
		
		return ((float) commonTokens) / totalTokens >= INTERSECT_THRESH;
	}
	
	
	/**
	 * <p>Sorts an array of strings by their length in ascending order.</p>
	 * 
	 * <p>This sort is guaranteed to be stable: strings of equal length are not
	 * reordered.</p>
	 * 
	 * @param ss array of strings
	 */
	public static void sortByLength(String[] ss) {
		Comparator<String> lengthC = new Comparator<String>() {
			public int compare(String s1, String s2) {
					return s1.length() - s2.length();
			}
		};
		
		Arrays.sort(ss, lengthC);
	}
	
	/**
	 * <p>Sorts an array of strings by their length in descending order.</p>
	 * 
	 * <p>This sort is guaranteed to be stable: strings of equal length are not
	 * reordered.</p>
	 * 
	 * @param ss array of strings
	 */
	public static void sortByLengthDesc(String[] ss) {
		Comparator<String> lengthC = new Comparator<String>() {
			public int compare(String s1, String s2) {
					return s2.length() - s1.length();
			}
		};
		
		Arrays.sort(ss, lengthC);
	}
	
	/**
	 * Capitalize the first letter of <code>input</code> and return the new string.
	 * 
	 */
	public static String capitalizeFirst(String input) {
		if (input == null) return null;
		
		return input.substring(0,1).toUpperCase() + input.substring(1);
	}
	
	/**
	 * Check whether all letters in a string are in upper case.
	 * 
	 */
	public static boolean isAllUppercase(String input) {
		if (input == null) return false;
		String cap = input.toUpperCase();
		if (cap.equals(input)) return true;
		else return false;
	}
	
	/**
	 * Make the first letter of <code>input</code> lower case and return the new string.
	 * 
	 */
	public static String lowercaseFirst(String input) {
		if (input == null) return null;
		
		return input.substring(0,1).toLowerCase() + input.substring(1);
	}
	
	/**
	 * replace XML-specific symbols:
	 * <           ->     &lt;
	 * &           ->     &amps;
	 * >           ->     &gt;
	 * "           ->     &quot;
	 * '           ->     &apos;
	 */
	public static String replaceXMLspecials (String input) {
		if (input == null) return null;
		return input.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
		.replaceAll("\"", "&quot;").replaceAll("\'", "&apos;");
	}
}
