/**
 * 
 */
package edu.jhu.jacana.test;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;

import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;

/**
 * Speed test code taken from:
 * http://b010.blogspot.com/2009/05/speed-comparison-of-1-javas-built-in.html
 * 
 * My own run shows that Trove is about 20-30 times faster than Java in gets(key) and contains(key)
 * while Colt is about 2-3 times faster than Trove
 * 
1st line: time used(s)
2nd line: heap memory used so far(MB)

===== Java's built-in HashMap =====

-- 10000000 puts(key, value) --
9.318541247
-96.87848663330078

-- 10000000 gets(key) --
6.615834282
-60.04905700683594

-- 10000000 containsKey(key) --
7.474065823
-90.540283203125

===== Trove's TIntIntHashMap =====

-- 10000000 puts(key, value) --
3.985574741
-291.6599807739258

-- 10000000 gets(key) --
0.276093337
-291.6599807739258

-- 10000000 containsKey(key) --
0.239907805
-291.6599807739258

===== Colt's OpenIntIntHashMap =====

-- 10000000 puts(key, value) --
1.920215603
-262.8419647216797

-- 10000000 gets(key) --
0.107288438
-262.8419647216797

-- 10000000 containsKey(key) --
0.084472067
-262.8419647216797
 * @author Xuchen Yao
 *
 */
public class HashSpeedTest {


	public static void main(String args[]){

		System.out.println("1st line: time used(s)\n2nd line: heap memory used so far(MB)");

		int n = 10000000;

		long startTime = System.nanoTime(); 
		long startHeapSize = Runtime.getRuntime().freeMemory();


		// BEGIN: benchmark for Java's built-in hashmap
		System.out.println("\n===== Java's built-in HashMap =====");
		HashMap jIntIntMap = new HashMap();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { jIntIntMap.put(i,new float[]{0f,1f,2f,3f,4f}); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { jIntIntMap.get(i); }
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
		TIntObjectHashMap tIntIntMap = new TIntObjectHashMap();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { tIntIntMap.put(i,new float[]{0f,1f,2f,3f,4f}); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { tIntIntMap.get(i); }
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
		for (int i = 0; i < n; i++) { cIntIntMap.put(i,new float[]{0f,1f,2f,3f,4f}); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (int i = 0; i < n; i++) { cIntIntMap.get(i); }
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