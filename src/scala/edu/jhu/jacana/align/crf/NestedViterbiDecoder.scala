/**
 *
 */
package edu.jhu.jacana.align.crf

import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.SegmentAlignSequence
import edu.jhu.jacana.align.AlignSequence
import edu.jhu.jacana.align.IndexLabelAlphabet
import iitb.CRF.Entry

/**
 * Decoder for Semi-CRF.
 * 
 * @author Xuchen Yao
 *
 */
class NestedViterbiDecoder(model: NestedLinearChainCRF) extends ViterbiDecoder(model) {
    
    override def fillArray(record: AlignSequence, calcScore: Boolean): Double = {
        var corrScore = 0.0
        val maxLen = AlignerParams.maxSourcePhraseLen
        val pair = record.getPair
        val featureVectorArray = model.getFeatureVectorArray(record)
        var i = 0
        while (i < record.length()) {
            var yi = 0
            while (yi < numY) {
                winningLabel(yi)(i).clear()
                winningLabel(yi)(i).valid = true
                yi += 1
            }
            var ell = 1
            while ((ell <= maxLen) && (i-ell+1 >= 0)) {
                for (ins <- featureVectorArray(i))
                    ins.setCurrSpan(ell)
                
                if (AlignerParams.debugLvl > 1) {
                    println("src: %d, span: %d, look backward: %s".format(i, ell, record.getPair.srcTokens.slice(i-ell+1, i+1).mkString(" ")))
                }
                model.computeLogMi(featureVectorArray(i), model.lambda,Mi,Ri,false,-1)
                var yi = 0
                while (yi < numY) {
                    if (i-ell < 0) {
                    	winningLabel(yi)(i).add(Ri.getQuick(yi).asInstanceOf[Float])
                    } else {
	                    var yp = 0
	                    //if (i == 2 && ell == 3 && yi == 46)
	                    //    println("here")
	                    while (yp < numY) {
	                        var value = Mi.getQuick(yp,yi)+Ri.getQuick(yi)
	                        winningLabel(yi)(i).add(winningLabel(yp)(i-ell), value.asInstanceOf[Float])
	                        yp += 1
	                    }
                    }
			        if (AlignerParams.debugLvl > 1) {
			            println("pos: %d label: %d span: %d score: %s ".format(i, yi, ell, winningLabel(yi)(i).toString()))
			        }
                    yi += 1
                }
                ell += 1
            }
	        if (AlignerParams.debugLvl > 1) {
	        	val bestSoln = new Entry(beamsize,0,0)
	        	bestSoln.valid = true
	            var yi = 0
	            while (yi < numY) {
	                bestSoln.add(winningLabel(yi)(i), 0)
	                yi += 1
	            }
	        	if (i == record.length - 1)
	        		println(record.getPair.id + " Final Best: " + bestSoln.get(0).toString())
        		else
	        		println(record.getPair.id + " Best: " + bestSoln.get(0).toString())
	        }
            if (calcScore)
                corrScore += (Ri.getQuick(record.y(i)) + (if (i > 0) Mi.getQuick(record.y(i-1),record.y(i)) else 0))
            i += 1
        }
        if (AlignerParams.debugLvl > 1) printWinningLabelMatrix()
        return 0;
    }

    override def bestLabelSequence(record: AlignSequence):Double = {
        record.zero_y()
        viterbiSearch(record, false);
        var ybest = finalSoln.get(0);
        ybest = ybest.prevSoln;
        while (ybest != null) {
            // here the label can beyond target sentence length (for phrases)
            // later in AlignEvaluator it is mapped back to target position and spans
            var srcSpan = ybest.pos - ybest.prevPos()
           	record.asInstanceOf[SegmentAlignSequence].setSegment(ybest.prevPos()+1,ybest.pos, ybest.label)
            val (pos, tgtSpan) = IndexLabelAlphabet.getPosAndSpanByStateIdx(ybest.label, record.getPair.tgtLen) 
            val srcPhrase = record.getPair.srcTokens.slice(ybest.prevPos()+1, ybest.prevPos()+1 + srcSpan).mkString(" ")
            val tgtPhrase = record.getPair.tgtTokens.slice(pos-1, pos-1 + tgtSpan).mkString(" ")
            if (srcSpan > 1 || tgtSpan > 1)
                println(f"${record.getPair.id}-source-span-$srcSpan(${ybest.prevPos()}-${ybest.pos})-target-span-$tgtSpan(${ybest.label}): $srcPhrase ::: $tgtPhrase")
            //else if (srcSpan > 1)
            //    println(f"${record.getPair.id}-source-span-$srcSpan: $srcPhrase ::: $tgtPhrase")
//            for (c <- pos until pos+span) {
//               	record.asInstanceOf[SegmentAlignSequence].setSegment(ybest.prevPos()+1,ybest.pos, c);
//	          }
            ybest = ybest.prevSoln;
            numStatesFired += 1
        }
        return finalSoln.get(0).score;
    }
}