/**
 *
 */
package edu.jhu.jacana.align.evaluation

import edu.jhu.jacana.align.AlignTrainData
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import edu.jhu.jacana.align.IndexLabelAlphabet

/**
 * http://axon.cs.byu.edu/Dan/478/assignments/permutation_test.php
 * 
 * This test is only for ``token`` accuracy.
 * @author Xuchen Yao
 *
 */
object PairedPermutationTest {
    
    val K = 100
    
    def test(gold:AlignTrainData, test1:AlignTrainData, test2:AlignTrainData):Double = {
        val acc1 = getAccuracyListByMatrix(gold, test1)
        val acc2 = getAccuracyListByMatrix(gold, test2)
        
        val diff:Array[Int] = new Array(acc1.length)
        var mu = 0
        for (i <- 0 until acc1.length) {
            diff(i) = acc1(i) - acc2(i)
            mu += diff(i)
        }
        mu = Math.abs(mu)
        
        val random = new Random()
        var k = 0
        var n = 0
        while (k < K) {
            //if (k % 10 == 0)
            //    println(k)
	        var i = 0
	        var mu_new = 0
	        while (i<diff.length) {
	            if (random.nextBoolean())
	                mu_new += diff(i)
                else
	                mu_new -= diff(i)
	            i += 1
	        }
	        if (Math.abs(mu_new) >= mu)
	            n += 1
            k += 1
        }
        
        val p = n*1.0/K
        println()
        println(n)
        println(f"p value: $p%f")
        return p
    }
    
    /**
     * return a list consisting 1 if match or 0 if not
     */
    def getAccuracyListByMatrix(gold:AlignTrainData, test:AlignTrainData): Array[Int] = {
        val goldPairList = gold.getPairList
        val testPairList = test.getPairList
        val accuracyList = new ArrayBuffer[Int]()
        for ((p1,p2) <- goldPairList.zip(testPairList)) {
            
            var i = 0; var j = 0;
            while (i < p1.rows) {
                j = 0
                while (j < p1.columns) {
                    if (true /*p1.alignMatrix(i)(j) != 0*/) {
	                    if (p1.alignMatrix(i)(j) == p2.alignMatrix(i)(j))
	                        accuracyList.append(1)
	                    else
	                        accuracyList.append(0)
	                    }
                    j+=1
                }
                i+=1
            }
        } 
        return accuracyList.toArray[Int]
    }
    
    def getAccuracyList(gold:AlignTrainData, test:AlignTrainData): Array[Int] = {
        val goldPairList = gold.getTrainList
        val testPairList = test.getTrainList
        val accuracyList = new ArrayBuffer[Int]()
        for ((p1,p2) <- goldPairList.zip(testPairList)) {
            
            var i = 0 
            while (i < p1.getLabelsPerToken.length) {
                    if (p1.getLabelsPerToken()(i) == p2.getLabelsPerToken()(i))
                        accuracyList.append(1)
                    else
                        accuracyList.append(0)
                i+=1
            }
        } 
        return accuracyList.toArray[Int]
    }

    def main(args: Array[String]): Unit = {
        
    val alphabet = new IndexLabelAlphabet()
		val goldData = new AlignTrainData("alignment-data/msr/converted/RTE2_test_M.align.txt", labelAlphabet=alphabet)
		val t1 = new AlignTrainData("/tmp/RTE2_test_M.align.txt.t2s", true, labelAlphabet=alphabet)
		val t2 = new AlignTrainData("/tmp/RTE2_test_M.align.txt.union", labelAlphabet=alphabet)
		test(goldData, t1, t2)
    }

}