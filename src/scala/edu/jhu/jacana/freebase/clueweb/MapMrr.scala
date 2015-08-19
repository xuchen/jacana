/**
 *
 */
package edu.jhu.jacana.freebase.clueweb

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

/**
 * Compute Mean Average Precision, Mean Reciprocal Rank.
 * 
 * relevantPoints are sectiosn of interest to see how many relevant points fall
 * in between these sections. For instance, an array of [1,3,10] counts:
 * how many results there are that have at least one relevant point that are:
 * in the top 1, top 2-3, top 4-10, and beyond top 10 sections
 * @author Xuchen Yao
 *
 */
class MapMrr(relevantPoints:Array[Int] = Array(1,3,5,10,20,50,100,200,300)) {

    var n = 0
    val rankList = new ArrayBuffer[Int]()
    val precList = new ArrayBuffer[Double]()
    
    val relevantCounts = new Array[Int](relevantPoints.size+1)
    // holds a sample of "StringForPrinting" (see below) so we really see the data
    val relevantTextSet = new Array[HashMap[String, Int]](relevantPoints.size+1)
    for (i <- 0 to relevantPoints.size) relevantTextSet(i) = new HashMap[String, Int]()
    /**
     * Add a result for one instance in a tuple form of 
     * (score, isRelevant, StringForPrinting)
     */
    def addResult(tupleList:List[(Double, Boolean, String)], print:Boolean=false) {
        // var l = Array((1.0, true),(1.0, false),(1.0, true), (0.0,true),(0.0,false))
        // l.sortBy(t =>(-t._1, t._2))
    	// Array[(Double, Boolean)] = Array((1.0,false), (1.0,true), (1.0,true), (0.0,false), (0.0,true))
        // descending sort on score first, then boolean (false comes in front of true since
        // sorting on boolean is ascending). this puts the relevant answer last
        val sorted = tupleList.sortBy(t => (-t._1, t._2))
        var rank = 1
        var found = false
        var aveP = 0.0
        var relevantSoFar = 0
        var relevantIdx = 0
        var added = false
        for (((s, relevant, text),i) <- sorted.zipWithIndex) {
            if (relevantIdx != relevantCounts.size-1 && i+1 > relevantPoints(relevantIdx)) {
                relevantIdx += 1 
                // added = false
            }
            if (relevant == true) {
                found = true
                relevantSoFar += 1
                aveP += relevantSoFar*1.0/(i+1)
                if (!added) {
                	relevantCounts(relevantIdx) += 1
                	if (!relevantTextSet(relevantIdx).contains(text))
                		relevantTextSet(relevantIdx) += text -> 0
              		relevantTextSet(relevantIdx)(text) += 1
                	added = true
                }
            } else if (!found) {
                rank += 1
            }
        }
        aveP /= relevantSoFar // divided by total relevants
        rankList += rank
        precList += aveP
        n += 1
        if (print)
            println(sorted.mkString("\n"))
    }
    
    def getMRR():Double = {
        // println(rankList)
        return rankList.foldLeft(0.0)((r,c) => r + 1.0/c)*1.0/n
    }
    
    def getMeanRank():Double = {
        return rankList.foldLeft(0.0)((r,c) => r + c)*1.0/n
    }

    def getMedianRank():Int = {
        val s = rankList.sortWith(_ < _)
        return s(s.size/2)
    }
    
    def getMAP():Double = {
        // http://en.wikipedia.org/wiki/Information_retrieval#Mean_average_precision
        // println(precList)
        return precList.foldLeft(0.0)((r,c) => r + c)*1.0/n
    }
    
    def printRevelantCounts() {
        // println(relevantPoints.toList)
        // println(relevantCounts.toList)
        // println(relevantCounts.map(x => x*1.0/n).toList)
        var previousP = 0
        for ((p,i) <- relevantPoints.zipWithIndex) {
            println(f"points in the range ($previousP, $p]: ${relevantCounts(i)} (${relevantCounts(i)*100.0/n}%.2f%%)")
            println("10 samples: "+ getTextSampleInString(i))
            previousP = p
        }
        val lastI = relevantPoints.size
        println(f"points beyond ($previousP, ...]: ${relevantCounts(lastI)} (${relevantCounts(lastI)*100.0/n}%.2f%%)")
        println("10 samples: "+ getTextSampleInString(lastI))
    }
    
    def getTextSampleInString(idx:Int, count:Int = 10):String = {
        var end = math.min(count, relevantTextSet(idx).size)
        // return the top 10 samples
        return relevantTextSet(idx).toList.sortBy(t => -t._2).slice(0, end).mkString(" | ")
    }
}

object MapMrr {

    def main(args: Array[String]): Unit = {
        val ranker = new MapMrr(Array(1,2,3))
        ranker.addResult(List((2.0, false, ""),(1.0, true, ""),(3.0, false, "")))
        ranker.addResult(List((3.0, false, ""),(2.0, true, ""),(1.0, false, "")))
        ranker.addResult(List((3.0, true, ""),(2.0, false, ""),(1.0, false, "")))
        println(ranker.getMRR()) // should get 0.61111..
        println(ranker.getMAP()) // should get 0.61111..
        ranker.printRevelantCounts
    }
}