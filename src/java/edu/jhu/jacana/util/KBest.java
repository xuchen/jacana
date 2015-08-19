// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 20 Feb 2011

package edu.jhu.jacana.util;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Random;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
   @author Benjamin Van Durme

   A wrapper over {@code java.util.PriorityQueue} that allows for finding the
   k-best entries in a series of length n, in time O(n lg k), by doing a single
   pass over the series and performing {@code insert()}.
 */
public class KBest<T> {
	public PriorityQueue<SimpleImmutableEntry<T,Double>> heap;
	int k;
	boolean max;
	// If false, we need to check for duplicates before an Insert
	public boolean allowDuplicates = true;

	public KBest (int k, boolean max, boolean allowDuplicates) {
		this.allowDuplicates = false;
		if (max)
			heap = new PriorityQueue<SimpleImmutableEntry<T,Double>>(k,Greater);
		else
			heap = new PriorityQueue<SimpleImmutableEntry<T,Double>>(k,Lesser);

		this.k = k;
		this.max = max;
	}

	public KBest (int k, boolean max) {
		if (max)
			heap = new PriorityQueue<SimpleImmutableEntry<T,Double>>(k,Greater);
		else
			heap = new PriorityQueue<SimpleImmutableEntry<T,Double>>(k,Lesser);

		this.k = k;
		this.max = max;
	}

	final Comparator<SimpleImmutableEntry<T,Double>> Lesser =
			new Comparator<SimpleImmutableEntry<T,Double>>() {
		public int compare(SimpleImmutableEntry<T,Double> x,
				SimpleImmutableEntry<T,Double> y) {
			if (x.getValue() < y.getValue())
				return 1;
			else if (x.getValue() > y.getValue())
				return -1;
			else
				return 0;
		}
	};

	final Comparator<SimpleImmutableEntry<T,Double>> Greater =
			new Comparator<SimpleImmutableEntry<T,Double>>() {
		public int compare(SimpleImmutableEntry<T,Double> x,
				SimpleImmutableEntry<T,Double> y) {
			if (x.getValue() > y.getValue())
				return 1;
			else if (x.getValue() < y.getValue())
				return -1;
			else
				return 0;
		}
	};

	public void insert (T key, double value) {
		if (heap.size() < k) {
			if (allowDuplicates)
				heap.add(new SimpleImmutableEntry<T,Double>(key, value));
			else {
				SimpleImmutableEntry<T,Double> pair = new SimpleImmutableEntry<T,Double>(key, value);
				if (! heap.contains(pair))
					heap.add(pair);
			}
		} else if ((max && (heap.peek().getValue() < value)) ||
				((!max) && (heap.peek().getValue() > value))) {
			if (allowDuplicates) {
				heap.remove(heap.peek());
				heap.add(new SimpleImmutableEntry<T,Double>(key, value));
			} else {
				SimpleImmutableEntry<T,Double> pair = new SimpleImmutableEntry<T,Double>(key, value);
				if (! heap.contains(pair)) {
					heap.remove(heap.peek());
					heap.add(pair);
				}
			}
		}
	}

	/**
     Returns k-best entries, sorted conditioned on this.max == true or false
	 */
	 public SimpleImmutableEntry<T,Double>[] toArray () {
		SimpleImmutableEntry<T,Double>[] results = new SimpleImmutableEntry[Math.min(k,heap.size())];
		results = heap.toArray(results);
		if (results != null) {
			if (!max)
				Arrays.sort(results,Greater);
			else
				Arrays.sort(results,Lesser);
		}
		return results;
	 }

	 public static void main (String[] args) {
		 KBest<String> kbest = new KBest(5,false);
		 Random r = new Random();
		 int v;

		 for (int i = 0; i < 10; i++) {
			 v = r.nextInt(100);
			 System.out.println(v);
			 kbest.insert(""+i,(double)v);
		 }

		 SimpleImmutableEntry<String,Double>[] results = kbest.toArray();
		 System.out.println("---------------------");
		 for (int i = 0; i < results.length; i++)
			 System.out.println(results[i].getKey() + "\t" + results[i].getValue());
	 }
}


