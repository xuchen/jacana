/**
 *
 */
package edu.jhu.jacana.align.crf

import cern.colt.matrix.impl.DenseDoubleMatrix1D
import iitb.CRF.RobustMath
import edu.jhu.jacana.align.util.AlignerParams
import scala.util.control.Breaks._
import edu.jhu.jacana.align.IndexLabelAlphabet
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignSequence
import edu.jhu.jacana.align.SegmentAlignSequence
import edu.jhu.jacana.align.feature.AlignFeature
import edu.jhu.jacana.align.AlignTrainRecord

/**
 * Nested(segment) CRF that is inherently a Semi-CRF.
 * 
 * This implementation contains lots of bug fixes of the original version.
 * 
 * @author Xuchen Yao
 *
 */
class NestedLinearChainCRF(featureAlphabet: Alphabet, FeatureExtractors: Array[AlignFeature],
        labelAlphabet:IndexLabelAlphabet) 
        extends LinearChainCRF(featureAlphabet, FeatureExtractors, labelAlphabet) {

     def sumProductInner(record: SegmentAlignSequence, lambda: Array[Double], grad: Array[Double], 
            onlyForwardPass: Boolean, numRecord: Int): Double = {
        
        val pair = record.getPair
        var len = record.length
        /*
         * forward-backward algorithm
         * 
         * suppose n = 4
         * size of alpha: n+1, size of beta: n+1
         * Initialization: alpha[0] = 1, beta[n] = 1
         * 
         * data index:						0			1			2			3
         * alpha:			alpha[0]=1	  alpha[1]	alpha[2]	alpha[3]	alpha[4]
         * beta:			beta[0]		  beta[1]	 beta[2]	 beta[3]	 beta[4]=1
         * M:							   M[1]		    M[2]		M[3]		M[4]
         * R:							   R[1]		    R[2]		R[3]		R[4]
         * 
         * Calculation (didn't take care of transpose in the following, check Sha&Pereira for details):
         * 					alpha[1] = R[1]
         * 			for i in 2 ... n
         * 					alpha[i] = alpha[i-1]*M[i].*R[i]
         * 					alpha[0]=1, alpha[1] = alpha[0]*M[1].*R[1], alpha[2] = alpha[1]*M[2].*R[2], ...
         * 
         * 					beta[0] = beta[1]+R[1]
         * 			for i in n-1 ... 1
         * 					beta[i] = (beta[i+1].*R[i+1])*M[i+1]
         * 					beta[4]=1, beta[3] = (beta[4].*R[4])*M[4], beta[2] = (beta[3].*R[4])*M[3], ...
         * 
         * Sanity check:
         * 
         * 			for i in 0 ... n:
         * 					alpha[i] * beta[i] == the same constant
         * 			e.g., 
         * 				alpha[0]*beta[0] = beta[0] = alpha[1]*beta[1] = ... = alpha[4]*beta[4] = alpha[4]
         * 
         * 				(in log domain, where log1 = 0)
         *				 alpha[0]+beta[0] = beta[0] = alpha[1]+beta[1] = ... = alpha[4]+beta[4] = alpha[4]
         */
        if ((alpha_Y == null) || (alpha_Y.length < len+1)) {
            // when, e.g., the 2nd sentence is shorter than the first sentence
            // we don't allocate new memory for alpha/beta. so don't be surprised
            // if the unused parts of alpha/beta are still filled with values
            // computed over the first sentence
        	allocateAlphaBeta(len+1)
        }
        val featureVectorArray = getFeatureVectorArray(record)
        // compute beta values in a backward scan.
        beta_Y(len).assign(0)
        var i = len - 1
        val maxMemory = AlignerParams.maxSourcePhraseLen
        while (i >= 0) {
            beta_Y(i).assign(RobustMath.LOG0)
            var ell = 1
           	var currPos = i+ell-1
            while ((ell <= maxMemory) && (currPos < len) && pair.featureVectors(currPos).hasCurrSpan(ell)) {
                // compute the Mi matrix
                pair.featureVectors(currPos).setCurrSpan(ell)
                initMDone = computeLogMi(featureVectorArray(currPos),lambda,Mi_YY,Ri_Y,false,record.y(currPos))
	                
//                tmp_Y.assign(beta_Y(currPos+1))
//                tmp_Y.assign(Ri_Y,SumFunc)
//	            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y(i),1,1,false,null)
	            
	            // this was the original implementation, which wasn't quite correct
	            // when computing beta(0), we should still add Mi_YY even though there's
	            // nothing to transit to index 0. This is because that taking exponential
	            // "makes nothing count as something": with Mi = 0, exp(0) = 1, if we don't
	            // add it, then the alphas and betas won't match by the sanity check
                if (i > 0) {
	                tmp_Y.assign(beta_Y(currPos+1))
	                tmp_Y.assign(Ri_Y,SumFunc)
	                RobustMath.logMult(Mi_YY, tmp_Y, beta_Y(i),1,1,false,null)
                } else {
	                tmp_Y.assign(beta_Y(currPos+1))
	                tmp_Y.assign(Ri_Y,SumFunc)
                    RobustMath.logSumExp(beta_Y(0),tmp_Y);
                }
                ell += 1
                currPos = i+ell-1
            }
            
            i -= 1
        }
        var thisSeqLogli = 0.0
        alpha_Y(0).assign(0);
        var segmentStart = 0;
        var segmentEnd = -1;
        var invalid = false;
        i = 0
        while (i < len && ! invalid) {
            if (segmentEnd < i) {
                segmentStart = i;
                segmentEnd = record.getSegmentEnd(i);
            }
            if (segmentEnd-segmentStart+1 > maxMemory) {
                // not possible to hit here since segment is specifically divided not to exceed maxMemory
                if (icall == 0) {
                    System.err.println("Ignoring record with segment length greater than maxMemory ");
                }
                if (AlignerParams.phraseBased) invalid = true
                //break
                //throw an exception
            }
            alpha_Y(i+1).assign(RobustMath.LOG0)
            var ell = 1
            while ((ell <= maxMemory) && (i-ell+1 >= 0) && pair.featureVectors(i).hasCurrSpan(ell)) {
                // compute the Mi matrix
                //featureGenNested.startScanFeaturesAt(dataSeq, i-ell,i);
                for (ins <- featureVectorArray(i))
                    ins.setCurrSpan(ell)
                initMDone = computeLogMi(featureVectorArray(i), lambda,Mi_YY,Ri_Y,false,record.y(i))
                                
	            if (i >= 0) {
	                RobustMath.logMult(Mi_YY, alpha_Y(i-ell+1),tmp_Y,1,0,true,null);
	                tmp_Y.assign(Ri_Y,SumFunc);
	                RobustMath.logSumExp(alpha_Y(i+1),tmp_Y);
	            } else {
                    RobustMath.logSumExp(alpha_Y(i+1),Ri_Y);
	            }

                var isSegment = ((i-ell+1==segmentStart) && (i == segmentEnd))
                for (ins <- featureVectorArray(i)) {
                    ins.setCurrSpan(ell)
                    var j = 0
                    while (j < ins.size()) {
    		            var f = ins.index(j)
    		            var yp = ins.y(j)
    		            var yprev = ins.yprev(j)
    		            var value = ins.value(j)
                        var allEllMatch = if (AlignerParams.phraseBased) isSegment && (record.y(i) == yp) else (record.y(i) == yp)
                        
                        if (allEllMatch && (((i-ell >= 0) && (yprev == record.y(i-ell))) || (yprev < 0))) {
                            // grad here is the empirical expectation
                            grad(f) += value
                            thisSeqLogli += value*lambda(f)
                        }
                        if ((yprev < 0) /*&& (i-ell >= 0)*/) {
                            // ExpF is the expectation under the model distribution
                            ExpF(f) = RobustMath.logSumExp(ExpF(f), tmp_Y.get(yp) + RobustMath.log(value) + beta_Y(i+1).get(yp))
                            // the line above is essentially the same as the 4 commented lines below (assuming that tmp_Y isn't changed from computing alph_Y
    //                    	yprev = 0
    //                    	while (yprev < Mi_YY.rows()) {
    //                    		ExpF(f) = RobustMath.logSumExp(ExpF(f), alpha_Y(i-ell+1).get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp) + RobustMath.log(value)+beta_Y(i+1).get(yp))
    //                    		yprev += 1
    //                    	}
                        } /*else if (i-ell < 0) {
                            ExpF(f) = RobustMath.logSumExp(ExpF(f), Ri_Y.get(yp)+RobustMath.log(value)+beta_Y(i+1).get(yp))
                        }*/ else {
                            ExpF(f) = RobustMath.logSumExp(ExpF(f), alpha_Y(i-ell+1).get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(value)+beta_Y(i+1).get(yp))
                        }
                        j += 1
                    }
                }
                
                /*
                if (true || i > 0 && i-ell+1 != 0) {
                    // IMPORTANT to check i-ell+1 != 0 here:
                    // when i-ell+1 == 0, it starts from the starting state (q0),
                    // then we should only add Ri_Y but not Mi_YY.
                    // if missed this, LBFGS would eventually break when epsilon is small (say 0.1)
                    RobustMath.logMult(Mi_YY, alpha_Y(i-ell+1),tmp_Y,1,0,true,null);
                    tmp_Y.assign(Ri_Y,SumFunc);
                    RobustMath.logSumExp(alpha_Y(i+1),tmp_Y);
                } else {
                    RobustMath.logSumExp(alpha_Y(i+1),Ri_Y);
                }
                */
                ell += 1
            }
            //if (i == 0) {
            //    pair.featureVectors(0).setCurrSpan(1)
            //    initMDone = computeLogMi(pair.featureVectors(0),lambda,Mi_YY,Ri_Y,false)
            //    RobustMath.logSumExp(alpha_Y(i+1),Ri_Y);
            //}
            if (AlignerParams.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y(i+1).toString());
                //System.out.println("Ri " + Ri_Y.toString());
                //System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y(i).toString());
            }
            if (AlignerParams.debugLvl > 1) {
                System.out.println(" pos "  + i + " " + thisSeqLogli);
            }
            i += 1
        }
        if (invalid)
            return 0;
        lZx = RobustMath.logSumExp(alpha_Y(len));
        return thisSeqLogli;        
     }
     
     
     	
	////////////////////////////// Viterbi /////////////////////////////////
    override def decode(record: AlignSequence): Double = {
        var viterbi = new NestedViterbiDecoder(this)
        var score = viterbi.decode(record);
        return score;
    }
}