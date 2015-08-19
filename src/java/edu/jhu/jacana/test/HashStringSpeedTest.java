/**
 * 
 */
package edu.jhu.jacana.test;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;

import cern.colt.map.OpenIntObjectHashMap;

/**
 * 
 * code adapted from HashSpeedTest to test Int->String mapping
 * similar conclusion holds
 * 
 * @author Xuchen Yao
 *
 */
public class HashStringSpeedTest {


	public static void main(String args[]){

		System.out.println("1st line: time used(s)\n2nd line: heap memory used so far(MB)");

		int n = 10000000;

		long startTime = System.nanoTime(); 
		long startHeapSize = Runtime.getRuntime().freeMemory();
		String tmp;


		// BEGIN: benchmark for Java's built-in hashmap
		System.out.println("\n===== Java's built-in HashMap =====");
		HashMap<Integer, String> jIntIntMap = new HashMap<Integer, String>();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { jIntIntMap.put(i,"abc"); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		
		for (int i = 0; i < n; i++) { tmp = jIntIntMap.get(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { jIntIntMap.containsKey(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END  


		// BEGIN: benchmark for Trove's TIntIntHashMap
		System.out.println("\n===== Trove's TIntIntHashMap =====");
		TIntObjectHashMap<String> tIntIntMap = new TIntObjectHashMap<String>();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { tIntIntMap.put(i,"abc"); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { tmp = tIntIntMap.get(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { tIntIntMap.containsKey(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END     

		// BEGIN: benchmark for Colt's OpenIntIntHashMap
		System.out.println("\n===== Colt's OpenIntIntHashMap =====");
		OpenIntObjectHashMap cIntIntMap = new OpenIntObjectHashMap();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { cIntIntMap.put(i, "abc"); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { tmp = (String)cIntIntMap.get(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { cIntIntMap.containsKey(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END    


	}

}