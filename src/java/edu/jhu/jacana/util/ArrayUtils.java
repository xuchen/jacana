package edu.jhu.jacana.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * A collection of array transformation utilities.
 * 
 * @author Nico Schlaefer
 * @version 2007-05-03
 */
public class ArrayUtils {
	/**
	 * Gets all subsets of the given array of objects.
	 * 
	 * @param objects array of objects
	 * @return all subsets
	 */
	public static Object[][] getAllSubsets(Object[] objects) {
		ArrayList<Object[]> subsets = new ArrayList<Object[]>();
		
		getAllSubsetsRec(subsets, new ArrayList<Object>(), objects, 0);
		
		return subsets.toArray(new Object[subsets.size()][]);
	}
	
	// recursive implementation of getAllSubsets(Object)
	private static void getAllSubsetsRec(ArrayList<Object[]> subsets,
			ArrayList<Object> subset, Object[] objects, int i) {
		if (i == objects.length) {
			subsets.add(subset.toArray(new Object[subset.size()]));
		} else {
			ArrayList<Object> subset1 = new ArrayList<Object>();
			for (Object o : subset) subset1.add(o);
			
			ArrayList<Object> subset2 = new ArrayList<Object>();
			for (Object o : subset) subset2.add(o);
			subset2.add(objects[i]);
			
			i++;
			getAllSubsetsRec(subsets, subset1, objects, i);
			getAllSubsetsRec(subsets, subset2, objects, i);
		}
	}
	
	/**
	 * Gets all non-empty subsets of the given array of objects.
	 * 
	 * @param objects array of objects
	 * @return all non-empty subsets
	 */
	public static Object[][] getNonemptySubsets(Object[] objects) {
		Object[][] subsets = getAllSubsets(objects);
		
		Object[][] nonempty = new Object[subsets.length - 1][];
		for (int i = 0; i < nonempty.length; i++)
			nonempty[i] = subsets[i + 1];
		
		return nonempty;
	}
	
	public static <T> ArrayList<T> reverseArrayList(ArrayList<T> list) {
		ArrayList<T> reverseList = new ArrayList<T>();
		if (list == null)
			return null;
		else {
			for (int i=list.size()-1; i>=0; i--) {
				reverseList.add(list.get(i));
			}
		}
		return reverseList;
	}
	
	/**
	 * Given a sequence [a, b, c, d] with a window of <code>upTo</code>=3,
	 * this returns a set {a, b, c, d, a b, b c, c d, a b c, b c d}
	 * with any member not in <code>vocab</code> removed.
	 * @param splits a string sequence
	 * @param upTo window of up to size <code>upTo</code>
	 * @param vocab make sure the slice is in the vocab
	 * @return a list of slices
	 */
	public static HashSet<String> getWindowedSlices(String[] splits, int upTo, HashSet<String> vocab) {
		HashSet<String> set = new HashSet<String>();
		/*
		 *         for window in range(1,min(up_to,l)):
			            for start in range(l-window+1):
			                ngrams[window].add(" ".join(splits[start:start+window]))
		 */
		int l = splits.length;
		for (int window=1; window<Math.min(upTo, l); window++) {
			for (int start=0; start<l-window+1; start++) {
				String slice = StringUtils.joinWithSpaces(Arrays.asList(splits).subList(start, start+window).toArray(new String[window]));
				if (vocab != null) {
					if (vocab.contains(slice))
						set.add(slice);
				} else
					set.add(slice);
			}
		}
		
		return set;
	}
	
	/**
	 * Like the above function, but returns a list of indices, each row contains 2 dims: [start (include), end (exclude.)]
	 * @param splits
	 * @param upTo
	 * @param vocab
	 * @return
	 */
	public static int[][] getWindowedSlicesIndex (String[] splits, int upTo, HashSet<String> vocab) {
		HashSet<String> set = new HashSet<String>();
		ArrayList<int[]> list = new ArrayList<int[]>(); 
		/*
		 *         for window in range(1,min(up_to,l)):
			            for start in range(l-window+1):
			                ngrams[window].add(" ".join(splits[start:start+window]))
		 */
		int l = splits.length;
		for (int window=1; window<Math.min(upTo, l); window++) {
			for (int start=0; start<l-window+1; start++) {
				String slice = StringUtils.joinWithSpaces(Arrays.asList(splits).subList(start, start+window).toArray(new String[window]));
				if (vocab != null) {
					if (vocab.contains(slice))
						list.add(new int[]{start, start+window});
				} else
					list.add(new int[]{start, start+window});
			}
		}
		
		return list.toArray(new int[list.size()][]);
	}
	
	public static <T> T[] concat(T[] first, T[] second) {
		  T[] result = Arrays.copyOf(first, first.length + second.length);
		  System.arraycopy(second, 0, result, first.length, second.length);
		  return result;
		}

	
}
