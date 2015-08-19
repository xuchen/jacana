/**
 *
 */
package edu.jhu.jacana.align.evaluation

import edu.jhu.jacana.align.AlignTrainData
import edu.jhu.jacana.align.util.Loggable
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.IndexLabelAlphabet
import scala.collection.mutable.HashSet
import edu.jhu.jacana.align.AlignPair

/**
 * Compute Macro-F1 and Micro-F1.
 * 
 * Macro-F1: averaging P/R/F1 from each pair (i.e., each pair has equal weight)
 * Micro-F1: count TP/FP/TN/FN from all pairs, then compute P/R/F1 
 * 
 * @author Xuchen Yao
 *
 */
object AlignEvaluator extends Loggable {
    
    /**
     * Given a AlignTrainRecord from '''test''', construct an align matrix
     * from it's labels per token assigned by the Viterbi decoding. The record's
     * own align matrix might still be the gold align matrix so we can't use it.
     * 
     * TODO: add support for phrase-based aligner
     */
    def getAlignMatrixFromLabel(record: AlignTrainRecord): Array[Array[Int]] = {
        val rows = record.getPair.rows
        val columns = record.getPair.columns
        val matrix = Array.ofDim[Int](rows, columns)
        
        val labelsPerToken = record.getLabelsPerToken
        // println("getAlignMatrixFromLabel id: %s (rows: %d, columns: %d)".format(record.getPair.id, rows, columns)) 
        // println("decoded labels: " + labelsPerToken.toList)
        var row = 0; var column = 0
        while (row < rows) {
            // tagged labels start from 1
            if (!AlignerParams.phraseBased) {
	            column = labelsPerToken(row) - 1
	            if (column != -1)
	            	matrix(row)(column) = 1
            } else {
                val (pos, span) = IndexLabelAlphabet.getPosAndSpanByStateIdx(labelsPerToken(row), record.getPair.tgtLen) 
                // val tgtPhrase = record.getPair.tgtTokens.slice(pos-1, pos-1+span).mkString(" ")
                //if (span > 1)
                //    println(f"${record.getPair.id}-target-span-$span: $tgtPhrase")
                var c = pos
	            while (c < pos+span) {
	                if (c != 0)
		                matrix(row)(c-1) = 1
		            c += 1
	            }
            }
            row += 1
        }
        return matrix
    }
    

    // return a set of tuples containing the upper left and bottom right corner indices
    def getPhraseBoundaries(a: Array[Array[Int]]): HashSet[(Int,Int,Int,Int)] = {
        val boundaries = new HashSet[(Int,Int,Int,Int)]()
        var i = 0
        var maxI = 0
        val srcLen = a.length
        val tgtLen = a(0).length
        while (i < srcLen) {
            maxI = i+1
            var j = 0
            var maxJ = 0
            var allZeros = true
            while (j < tgtLen && allZeros) {
                maxJ = j+1
                if (a(i)(j) == 1) {
                    allZeros = false
                    var expandI = true; var expandJ = true;
                    while (expandI || expandJ) {
                        if (expandI) {
                            if (maxI == srcLen ) {
                                expandI = false
                            } else {
                            	if ((maxI-i+1) * (maxJ-j) == AlignPair.sumRectAt(a, i, maxI+1, j, maxJ)) {
                            	    maxI += 1
                            	} else
                            	    expandI = false
                            }
                        }
                        if (expandJ) {
                            if (maxJ == tgtLen) {
                                expandJ = false
                            } else {
                            	if ((maxI-i) * (maxJ-j+1) == AlignPair.sumRectAt(a, i, maxI, j, maxJ+1)) {
                            	    maxJ += 1
                            	} else
                            	    expandJ = false
                            }
                        }
                    }
                } else {
                    j = maxJ
                }
            }
            if (allZeros) {
            } else {
                boundaries.add((i, j, maxI, maxJ))
            }
            i = maxI
        }       
        return boundaries
    }
    
    /**
     * Given two files in MSR alignment format, print Macro/Micro F1
     */
    def evaluateFile(goldFileName: String, testFileName: String, transpose: Boolean = false) {
    val alphabet = new IndexLabelAlphabet()
		val goldData = new AlignTrainData(goldFileName, labelAlphabet=alphabet)
    alphabet.freeze
		val testData = new AlignTrainData(testFileName, transpose, labelAlphabet=alphabet)
		evaluate(goldData, testData)
		evaluateByTokenAndPhrase(goldData, testData)
    }
    
     def evaluateByTokenAndPhrase(goldData: AlignTrainData, testData: AlignTrainData) {
        val goldAll = goldData.getTrainData
        val testAll = (if (testData == null) goldAll else testData.getTrainData)
        require(goldAll.size == testAll.size, s"Gold list size $goldAll.size != Test list size $testAll.size")
        
        // divided by token (t) and phrase (p)
        var tp_t = 0.0; var tp_p = 0.0;
        var fp_t = 0.0; var fp_p = 0.0;
        var fn_t = 0.0; var fn_p = 0.0;
        var (precision_t, recall_t, f1_t) = (0.0, 0.0, 0.0)
        var (precision_p, recall_p, f1_p) = (0.0, 0.0, 0.0)
	        
        var counter = 0
        var total_p = 0; var total_t = 0
        while (counter < goldAll.length) {
            val gold = goldAll(counter)
            val test = testAll(counter)
            counter += 1
            var goldMatrix = gold.getPair.alignMatrix
            var testMatrix = if (testData == null) getAlignMatrixFromLabel(test) else test.getPair.alignMatrix
        
	        val goldSet = getPhraseBoundaries(goldMatrix)
	        val testSet = getPhraseBoundaries(testMatrix)
	        
	        var has_token_align = false; var has_phrase_align = false;
	        
	        for (testTuple <- testSet) {
	            if (testTuple._3-testTuple._1 == 1 && testTuple._4-testTuple._2 == 1) {
	                // token
	                if (goldSet.contains(testTuple)) {
	                    tp_t += 1
	                } else {
	                    fp_t += 1
	                }
	            } else {
	                // phrase
	                if (goldSet.contains(testTuple)) {
	                    tp_p += 1
	                } else {
	                    fp_p += 1
	                }
	            }
	        }
	        
	        for (goldTuple <- goldSet) {
	            // only find out false negative
	            if (goldTuple._3-goldTuple._1 == 1 && goldTuple._4-goldTuple._2 == 1) {
	                has_token_align = true
	                if (!testSet.contains(goldTuple)) {
	                   fn_t += 1 
	                }
	            } else {
	                has_phrase_align = true
	                if (!testSet.contains(goldTuple)) {
	                   fn_p += 1 
	                }
	            }
	        }
	        if (has_token_align) total_t += 1
	        if (has_phrase_align) total_p += 1
	        precision_t += (if (tp_t+fp_t!=0) tp_t / (tp_t + fp_t) else 0.0)
	        recall_t += (if (tp_t+fn_t!=0) tp_t / (tp_t + fn_t) else 0.0)
	        
	        precision_p += (if (tp_p+fp_p!=0) tp_p / (tp_p + fp_p) else 0.0)
	        recall_p += (if (tp_p+fn_p!=0) tp_p / (tp_p + fn_p) else 0.0)
        }
         // macro statistics
        precision_t /= total_t; precision_p /= total_p;
        recall_t /= total_t; recall_p /= total_p; 
        f1_t = 2*precision_t*recall_t/(precision_t+recall_t)
        f1_p = 2*precision_p*recall_p/(precision_p+recall_p)
        
	    val sp = new StringBuilder();
	    
	    sp.append("Macro-averaged (reported in paper) :\n")
	    sp.append("                    (token/phrase):\n")
	    sp.append("%-20s      (%.1f/%.1f)\n".format( "  Precision", 100.0 * precision_t, 100.0 * precision_p))
	    sp.append("%-20s      (%.1f/%.1f)\n".format( "  Recall", 100.0 * recall_t, 100.0 * recall_p))
	    sp.append("%-20s      (%.1f/%.1f)\n".format( "  F1", 100.0 * f1_t, 100.0 * f1_p))
	    
	    println(sp.toString())       
    }
       
    
    /**
     * when <code>test</code> is not given, this method evaluates gold.alignMatrix (gold-standard) with
     * gold.labelsPerToken (decoded). This is used when we already know the gold alignments of test data.
     * 
     */
    def evaluate(goldData: AlignTrainData, testData: AlignTrainData = null, printMicroAveraged: Boolean = false): (Double,Double,Double,Double) = {
        val goldAll = goldData.getTrainData
        val testAll = (if (testData == null) goldAll else testData.getTrainData)
        require(goldAll.size == testAll.size, s"Gold list size ${goldAll.size} != Test list size ${testAll.size}")
        
        // for macro-averaged statistics
        var (accuracy, precision, recall, f1) = (0.0, 0.0, 0.0, 0.0)
        var exact = 0.0; val total = goldAll.length * 1.0
        var total_i = 0; var total_n = 0;
        // for micro-averaged statistics
        var tp = 0.0               // true positives: guess = true, gold = true
        var fp = 0.0               // false positives: guess = true, gold = false
        var fn = 0.0               // false negatives: guess = false, gold = true
        var tn = 0.0               // true negatives: guess = false, gold = false
        
        // divided by identical (i) and non-identical (n)
        var tp_i = 0.0; var tp_n = 0.0;
        var fp_i = 0.0; var fp_n = 0.0;
        var fn_i = 0.0; var fn_n = 0.0;
        var tn_i = 0.0; var tn_n = 0.0;
        var (precision_i, recall_i, f1_i) = (0.0, 0.0, 0.0)
        var (precision_n, recall_n, f1_n) = (0.0, 0.0, 0.0)
        
        var (tpTotal, fpTotal, fnTotal, tnTotal) = (0.0, 0.0, 0.0, 0.0)
        
        // count how much percentage of alignments are identical alignments
        var (gold_same, gold_diff, test_same, test_diff) = (0, 0, 0, 0)
        
        var (gold_same_test_correct, gold_diff_test_correct) = (0, 0)
        
        var counter = 0
        //goldAll.zip(testAll) foreach { case (gold, test) => 
        while (counter < goldAll.length) {
            val gold = goldAll(counter)
            val test = testAll(counter)
            counter += 1
            var goldMatrix = gold.getPair.alignMatrix
            var testMatrix = if (testData == null) getAlignMatrixFromLabel(test) else test.getPair.alignMatrix
            var rows = goldMatrix.length; var columns = goldMatrix(0).length;
            var allMatch = true
            tp = 0.0; fp = 0.0; fn = 0.0; tn = 0.0;
            
            var srcTokens = gold.getPair.srcTokens
            var tgtTokens = gold.getPair.tgtTokens
            
            var has_identical = false; var has_nonidentical = false;
            var i = 0; var j = 0;
            while (i < rows) {
                j = 0
                while (j < columns) {
                    (goldMatrix(i)(j), testMatrix(i)(j)) match {
                        case (1,1) => tp += 1; if (srcTokens(i) == tgtTokens(j)) tp_i += 1 else tp_n += 1;
                        case (1,0) => fn += 1; allMatch = false; if (srcTokens(i) == tgtTokens(j)) fn_i += 1 else fn_n += 1;
                        case (0,1) => fp += 1; allMatch = false; if (srcTokens(i) == tgtTokens(j)) fp_i += 1 else fp_n += 1;
                        case (0,0) => tn += 1; if (srcTokens(i) == tgtTokens(j)) tn_i += 1 else tn_n += 1;
                    }
                    if (!has_identical && srcTokens(i) == tgtTokens(j))
                        has_identical = true
                    if (!has_nonidentical && srcTokens(i) != tgtTokens(j))
                        has_nonidentical = true
                    if (goldMatrix(i)(j) == 1) {
                        if (srcTokens(i) == tgtTokens(j)) {
                            gold_same += 1
                            if (testMatrix(i)(j) == 1) gold_same_test_correct += 1
                        }
                        else {
                            gold_diff += 1
                            if (testMatrix(i)(j) == 1) gold_diff_test_correct += 1
                        }
                    }
                    if (testMatrix(i)(j) == 1) {
                        if (srcTokens(i) == tgtTokens(j))
                            test_same += 1
                        else
                            test_diff += 1
                    }
                    j += 1
                }
                i += 1
            }
            if (has_identical) total_i += 1
            if (has_nonidentical) total_n += 1
            if (allMatch) exact += 1
            precision += (if (tp+fp!=0) tp / (tp + fp) else 0.0)
            recall += (if (tp+fn!=0) tp / (tp + fn) else 0.0)
            accuracy += (tp + tn) / (rows * columns)
            tpTotal += tp; fpTotal += fp; fnTotal += fn; tnTotal += tn;
            
            
            precision_i += (if (tp_i+fp_i!=0) tp_i / (tp_i + fp_i) else 0.0)
            recall_i += (if (tp_i+fn_i!=0) tp_i / (tp_i + fn_i) else 0.0)
            
            precision_n += (if (tp_n+fp_n!=0) tp_n / (tp_n + fp_n) else 0.0)
            recall_n += (if (tp_n+fn_n!=0) tp_n / (tp_n + fn_n) else 0.0)
        }
        
        // macro statistics
        precision /= total; precision_i /= total_i; precision_n /= total_n;
        recall /= total; recall_i /= total_i; recall_n /= total_n; 
        accuracy /= total
        f1 = 2*precision*recall/(precision+recall)
        f1_i = 2*precision_i*recall_i/(precision_i+recall_i)
        f1_n = 2*precision_n*recall_n/(precision_n+recall_n)
        
	    val sp = new StringBuilder();
	    sp.append("%-20s %.1f   (%.0f of %.0f)\n".format(
	              "Exact match", 100.0 * exact / total, exact, total))
	    // don't print accuracy here since most 0s match so the number is usually very high
	    //sp.append("%-20s %.1f\n".format( "Accuracy", 100.0 * accuracy))
	    
	    sp.append("Macro-averaged (reported in paper) :\n")
	    sp.append("                    average (identical/non-identical):\n")
	    sp.append("%-20s %.1f (%.1f/%.1f)\n".format( "  Precision", 100.0 * precision, 100.0 * precision_i, 100.0 * precision_n))
	    sp.append("%-20s %.1f (%.1f/%.1f)\n".format( "  Recall", 100.0 * recall, 100.0 * recall_i, 100.0 * recall_n))
	    sp.append("%-20s %.1f (%.1f/%.1f)\n".format( "  F1", 100.0 * f1, 100.0 * f1_i, 100.0 * f1_n))
	    val retValues = (f1, precision, recall, exact)
	    
	    // micro statistics
	    if (printMicroAveraged) {
	        precision = tpTotal / (tpTotal + fpTotal)
	        recall = tpTotal / (tpTotal + fnTotal)
	        accuracy = (tpTotal + tnTotal) / (tpTotal + tnTotal + fpTotal + fnTotal)
	        f1 = 2*precision*recall/(precision+recall)
		    sp.append("Micro-averaged:\n")
		    sp.append("%-20s %.1f\n".format( "  Precision", 100.0 * precision))
		    sp.append("%-20s %.1f\n".format( "  Recall", 100.0 * recall))
		    sp.append("%-20s %.1f\n".format( "  F1", 100.0 * f1))
	    }
	    sp.append("percentage of identical alignments in gold: %.3f(%d/%d)\n".format(gold_same*1.0/(gold_same+gold_diff), gold_same, gold_same + gold_diff))
	    sp.append("percentage of identical alignments in test: %.3f(%d/%d)\n".format(test_same*1.0/(test_same+test_diff), test_same, test_same + test_diff))
	    
	    //sp.append("recall of identical alignments in gold: %.3f(%d/%d)\n".format(gold_same_test_correct*1.0/(gold_same), gold_same_test_correct, gold_same))
	    //sp.append("recall of non-identical alignments in gold: %.3f(%d/%d)\n".format(gold_diff_test_correct*1.0/(gold_diff), gold_diff_test_correct, gold_diff))
	    
	    println(sp.toString())
	    return retValues
    }

    def main(args: Array[String]): Unit = {
        if (args.length == 3)
            evaluateFile(args(0), args(1), true)
        else if (args.length == 2)
            evaluateFile(args(0), args(1))
        else {
        	evaluateFile("alignment-data/edinburgh/synthetic-phrases/gold.synthetic-phrases.test.sure.json", "/tmp/edinburgh.aligned.maxacc.json", false)
        	//evaluateFile("alignment-data/edinburgh/gold.test.sure.json", "/tmp/edinburgh.aligned.maxacc.json", false)
        	//evaluateFile("alignment-data/msr/converted/RTE2_test_M.align.json", "/tmp/msr.aligned.maxacc.json", false)
        	//evaluateFile("alignment-data/msr/converted/synthetic-phrases/RTE2_test_M.synthetic-phrases.json", "/tmp/msr.aligned.maxacc.json", false)
        	//evaluateFile("alignment-data/msr/converted/RTE2_test_M.align.txt", "alignment-data/msr/baselines/giza/RTE2_test_M.align.giza.grow-diag-final.txt")
        	//evaluateFile("alignment-data/msr/converted/RTE2_test_M.align.txt", "alignment-data/msr/baselines/giza/RTE2_test_M.align.giza.intersect.txt")
        	//evaluateFile("alignment-data/msr/converted/RTE2_test_M.align.txt", "alignment-data/msr/baselines/berkeley/RTE2_test_M.align.berkeley-unsupervised.txt")
        	//evaluateFile("alignment-data/msr/converted/RTE2_test_M.align.txt", "alignment-data/msr/baselines/ted/RTE2_test_M.align.ted.txt")
        }
    }
}