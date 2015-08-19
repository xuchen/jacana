/**
 *
 */
package edu.jhu.jacana.align.speedtest

import scala.collection.mutable.HashMap
import org.apache.mahout.math.map.OpenObjectIntHashMap
import gnu.trove.map.hash.TObjectIntHashMap
/**
 * a speed test with mapping from String to Int. Colt doesn't have an object
 * called OpenObjectIntHashMap, but Mahout (based on Colt) does.
 * Mahout is slower in gets/containsKey than Trove, but faster in puts.


===== Scala's built-in HashMap =====

-- 10000000 puts(key, value) --
22.14396408
-327.71356201171875

-- 10000000 gets(key) --
11.165029111
-499.9617614746094

-- 10000000 containsKey(key) --
6.263850502
-503.602294921875

===== Trove's TObjectIntHashMap =====

-- 10000000 puts(key, value) --
6.684251572
-622.0805816650391

-- 10000000 gets(key) --
2.253210976
-539.674560546875

-- 10000000 containsKey(key) --
1.492583954
-543.8705673217773

===== Mahout's OpenObjectIntHashMap =====

-- 10000000 puts(key, value) --
5.130357978
-181.51670837402344

-- 10000000 gets(key) --
4.245474484
-749.4973983764648

-- 10000000 containsKey(key) --
1.724255418
-755.1273574829102
* 

 * @author Xuchen Yao
 *
 */
object HashStringIntSpeedTest {

    def main(args: Array[String]): Unit = {
		val n = 10000000;

		var startTime = System.nanoTime(); 
		var startHeapSize = Runtime.getRuntime().freeMemory();


		// BEGIN: benchmark for Scala's built-in hashmap
		System.out.println("\n===== Scala's built-in HashMap =====");
		val jIntIntMap = new HashMap[String, Integer]();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { jIntIntMap.put(i.toString, i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { jIntIntMap.get(i.toString); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { jIntIntMap.contains(i.toString); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END  


		// BEGIN: benchmark for Trove's TIntIntHashMap
		System.out.println("\n===== Trove's TObjectIntHashMap =====");
		val tIntIntMap = new TObjectIntHashMap[String]();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { tIntIntMap.put(i.toString, i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { tIntIntMap.get(i.toString); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { tIntIntMap.containsKey(i.toString); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END     

		// BEGIN: benchmark for Mahout's OpenIntIntHashMap
		System.out.println("\n===== Mahout's OpenObjectIntHashMap =====");
		val cIntIntMap = new OpenObjectIntHashMap[String]();

		System.out.println("\n-- " + n + " puts(key, value) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { cIntIntMap.put(i.toString, i); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " gets(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { cIntIntMap.get(i.toString); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  

		System.out.println("\n-- " + n + " containsKey(key) --");
		startTime = System.nanoTime(); 
		for (i <- 0 until n) { cIntIntMap.containsKey(i.toString); }
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  
		// END    

    }

}