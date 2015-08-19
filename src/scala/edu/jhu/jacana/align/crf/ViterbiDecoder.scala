/**
 *
 */
package edu.jhu.jacana.align.crf

import edu.jhu.jacana.align.AlignTrainRecord
import edu.jhu.jacana.align.util.AlignerParams
import cern.colt.matrix.impl.DenseDoubleMatrix2D
import cern.colt.matrix.impl.DenseDoubleMatrix1D
import iitb.CRF.Entry
import edu.jhu.jacana.align.AlignTrainData
import edu.jhu.jacana.align.AlignSequence

/**
 * Viterbi Decoder for [[LinearChainCRF]].
 * Currently it only finds the best label sequence.  (TODO)
 * @author Xuchen Yao
 *
 */
class ViterbiDecoder(model:LinearChainCRF) {
	
    var beamsize = AlignerParams.beamSize
        
    var winningLabel: Array[Array[Entry]] = null
    var finalSoln:Entry = null
    protected var Mi:DenseDoubleMatrix2D = null
    protected var Ri:DenseDoubleMatrix1D = null
    // important: each test record has a possibly different number of Y's
    protected var numY = 0
    
    // for token-based decoder, it is equal to the source length
    // for phrase-based decoder, it is <= source length
    protected var numStatesFired: Int = 0

	//def decode(recordList: List[AlignTrainRecord]) {}
    
	def decode(record: AlignSequence): Double = {
        // very important: we want the length of target, not the source
	    numY = record.y_length + 1
        Mi = new DenseDoubleMatrix2D(numY,numY)
        Ri = new DenseDoubleMatrix1D(numY)
        winningLabel = new Array[Array[Entry]](numY)
        finalSoln = new Entry(beamsize,0,0)
        var score = bestLabelSequence(record)
        record.set_score(score/numStatesFired)
        return score
	}
	
	def bestLabelSequence(record: AlignSequence): Double = {
        var corrScore = viterbiSearch(record, false)
        if(AlignerParams.debugLvl > 1)
            System.out.println("Score of best sequence "+finalSoln.get(0).score + " corrScore " + corrScore)
        
        assignLabels(record)   
        return finalSoln.get(0).score
	}
	
	def assignLabels(dataSeq: AlignSequence) {
	    dataSeq.zero_y()
        var ybest = finalSoln.get(0);
        ybest = ybest.prevSoln;
        var pos = -1
        assert(ybest.pos == dataSeq.length()-1)
        while (ybest != null) {
            pos = ybest.pos
            //setSegment(dataSeq,ybest.prevPos(),ybest.pos, ybest.label)
            dataSeq.set_y(ybest.pos, ybest.label)
            ybest = ybest.prevSoln
            numStatesFired += 1
        }
        assert(pos>=0);
    }
 
    def viterbiSearch(record: AlignSequence, calcCorrectScore: Boolean):Double = {
        
        if ((winningLabel(0) == null) || (winningLabel(0).length < record.length())) {
            var yi = 0
            while (yi < winningLabel.length) {
                winningLabel(yi) = new Array[Entry](record.length())
                var l = 0
                while (l < record.length()) {
                    winningLabel(yi)(l) = new Entry(if (l==0) 1 else beamsize, yi, l)
                    l += 1
                }
                yi += 1
            }
        }
        
        var corrScore = fillArray(record, calcCorrectScore)
        
        finalSoln.clear()
        finalSoln.valid = true
        if (record.length() > 0) {
            var yi = 0
            while (yi < numY) {
                finalSoln.add(winningLabel(yi)(record.length()-1), 0)
                yi += 1
            }
        }
        return corrScore
    }   
    
    def fillArray(record: AlignSequence, calcScore: Boolean): Double = {
        val pair = record.getPair
        val featureVectorArray = model.getFeatureVectorArray(record)
        var corrScore = 0.0
        var i = 0
        while (i < record.length()) {
            // compute Mi.
            model.computeLogMi(featureVectorArray(i),model.lambda,Mi,Ri,false,-1)
            var yi = 0
            while (yi < numY) {
                winningLabel(yi)(i).clear()
                winningLabel(yi)(i).valid = true
                yi += 1
            }
            yi = 0
            while (yi < numY) {
                if (i > 0) {
                    var yp = 0
                    while (yp < numY) {
                        var value = Mi.getQuick(yp,yi)+Ri.getQuick(yi)
                        winningLabel(yi)(i).add(winningLabel(yp)(i-1), value.asInstanceOf[Float])
                        yp += 1
                    }
                } else {
                    winningLabel(yi)(i).add(Ri.getQuick(yi).asInstanceOf[Float])
                }
                yi += 1
            }
            if (calcScore)
                corrScore += (Ri.getQuick(record.y(i)) + (if (i > 0) Mi.getQuick(record.y(i-1),record.y(i)) else 0))
            i += 1
        }
        if (AlignerParams.debugLvl > 1) printWinningLabelMatrix()
        return corrScore
    }
    
    def printWinningLabelMatrix() {
        for (i <- winningLabel.length-1 to 0 by -1) {
                System.out.print(i)
            for (j <- 0 until winningLabel(i).length) {
                val soln = winningLabel(i)(j).get(0)
                val prevSoln = soln.prevSoln
                if (j > 0 && prevSoln != null)
                	System.out.print("\t%.2f(%d,%.2f)".format(soln.score, prevSoln.label, prevSoln.score))
                else
                	System.out.print("\t%.2f".format(soln.score))
            }
            println()
        }
    }


}