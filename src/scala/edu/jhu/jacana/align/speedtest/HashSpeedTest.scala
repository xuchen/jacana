/**
 *
 */
package edu.jhu.jacana.align.speedtest

import cern.colt.map.OpenIntIntHashMap
import cern.colt.map.OpenIntObjectHashMap
import scala.collection.mutable.HashMap
import gnu.trove.map.hash.TIntObjectHashMap
/**
 * a speed test with mapping from Int to Array[Int]. Colt's
 * OpenIntObjectHashMap requires casting, in practice the overhead
 * is very small. Colt/Mahout is still 2-3 times faster in terms of gets/containsKey
 * than Trove
 * 
===== Scala's built-in HashMap =====

-- 10000000 puts(key, value) --
20.726784293
-90.50626373291016

-- 10000000 gets(key) --
10.226936651
-57.65959930419922

-- 10000000 containsKey(key) --
12.927389183
-87.76583862304688

===== Trove's TIntObjectHashMap =====

-- 10000000 puts(key, value) --
3.787923627
-253.70880126953125

-- 10000000 gets(key) --
0.26237162
-253.70880126953125

-- 10000000 containsKey(key) --
0.233023865
-253.70880126953125

===== Colt's OpenIntObjectHashMap =====

-- 10000000 puts(key, value) --
3.721191769
-249.74031829833984

-- 10000000 gets(key) --
0.103233798
-249.74031829833984

-- 10000000 containsKey(key) --
0.090009946
-249.74031829833984

===== Colt's OpenIntObjectHashMap =====

-- 10000000 puts(key, value) --
3.824094232
-222.95194244384766

-- 10000000 gets(key) --
0.099930151
-222.95194244384766

-- 10000000 containsKey(key) --
0.089707941
-222.95194244384766
 * 
 * @author Xuchen Yao
 *
 */
object HashSpeedTest {

    def main(args: Array[String]): Unit = {
		val n = 10000000;

		var startTime = System.nanoTime(); 
		var startHeapSize = Runtime.getRuntime().freeMemory();


		// BEGIN: benchmark for Scala's built-in hashmap
		System.out.println("\n===== Scala's built-in HashMap =====");
		val jIntIntMap = new HashMap[Int, Array[Float]]();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { jIntIntMap.put(i, Array[Float](0f,1f,2f,3f,4f)); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { jIntIntMap.get(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { jIntIntMap.contains(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END  


		// BEGIN: benchmark for Trove's TIntIntHashMap
		System.out.println("\n===== Trove's TIntObjectHashMap =====");
		val tIntIntMap = new TIntObjectHashMap[Array[Float]]();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { tIntIntMap.put(i, Array[Float](0f,1f,2f,3f,4f)); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { tIntIntMap.get(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { tIntIntMap.containsKey(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END     

		// BEGIN: benchmark for Colt's OpenIntIntHashMap
		System.out.println("\n===== Colt's OpenIntObjectHashMap =====");
		val cIntIntMap = new OpenIntObjectHashMap();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { cIntIntMap.put(i, Array[Float](0f,1f,2f,3f,4f)); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { cIntIntMap.get(i).asInstanceOf[Array[Float]]; }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { cIntIntMap.containsKey(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END    

		// BEGIN: benchmark for Mahout's OpenIntIntHashMap
		System.out.println("\n===== Mahout's OpenIntObjectHashMap =====");
		val mIntIntMap = new org.apache.mahout.math.map.OpenIntObjectHashMap[Array[Float]]();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { mIntIntMap.put(i, Array[Float](0f,1f,2f,3f,4f)); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { mIntIntMap.get(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { mIntIntMap.containsKey(i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END    

    }

}