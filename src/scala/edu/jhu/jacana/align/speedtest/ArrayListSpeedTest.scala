/**
 *
 */
package edu.jhu.jacana.align.speedtest

import cern.colt.map.OpenIntIntHashMap
import cern.colt.map.OpenIntObjectHashMap
import scala.collection.mutable.HashMap
import org.apache.mahout.math.list.IntArrayList
import org.apache.mahout.math.list.DoubleArrayList
import org.apache.mahout.math.list.ObjectArrayList
import gnu.trove.list.array.TIntArrayList
import gnu.trove.list.array.TDoubleArrayList
import scala.Tuple4
/**
 * a speed test with having 4 array lists vs. 1 array list of tuple4.
 * 
 * Turns out the latter is extremely slow.
 * Also, Trove is about 30% faster than Mahout
 * 
===== Trove Four ArrayList =====
0.695166415
26.88378143310547

===== Mahout Four ArrayList =====
0.976812049
-177.25894165039062

===== Mahout One ArrayList =====
didn't finish...
 * 
 * 
 * @author Xuchen Yao
 *
 */
object ArrayListSpeedTest {

    def main(args: Array[String]): Unit = {
		val n = 10000000;
		var startTime = System.nanoTime(); 
		var startHeapSize = Runtime.getRuntime().freeMemory();

		System.out.println("\n===== Trove Four ArrayList =====");
		startTime = System.nanoTime(); 
		startHeapSize = Runtime.getRuntime().freeMemory();
		val tList1 = new TIntArrayList()
		val tList2 = new TIntArrayList()
		val tList3 = new TIntArrayList()
		val tList4 = new TDoubleArrayList()
		for (i <- 0 until n) {
		    tList1.add(i); tList2.add(i); tList3.add(i); tList4.add(i);
		}
		
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  


		System.out.println("\n===== Mahout Four ArrayList =====");
		startTime = System.nanoTime(); 
		startHeapSize = Runtime.getRuntime().freeMemory();
		val mList1 = new IntArrayList()
		val mList2 = new IntArrayList()
		val mList3 = new IntArrayList()
		val mList4 = new DoubleArrayList()
		for (i <- 0 until n) {
		    mList1.add(i); mList2.add(i); mList3.add(i); mList4.add(i);
		}
		
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  


		System.out.println("\n===== Mahout One ArrayList =====");
		startTime = System.nanoTime(); 
		startHeapSize = Runtime.getRuntime().freeMemory();
		val mList = new ObjectArrayList[Tuple4[Int,Int,Int,Double]]()
		for (i <- 0 until n) {
		    mList.add((i,i,i,i.toDouble))
		}
		
		System.out.println( (System.nanoTime() - startTime) / 1000000000.0 );
		System.out.println( (startHeapSize - Runtime.getRuntime().freeMemory()) /1048576.0  );  


    }

}