/**
 *
 */
package edu.jhu.jacana.align.crf

import java.util.Properties
import edu.jhu.jacana.align.AlignPair
import cern.colt.matrix.impl.DenseDoubleMatrix2D
import edu.jhu.jacana.align.IndexLabelAlphabet
import cern.colt.matrix.DoubleMatrix1D
import cern.colt.matrix.impl.DenseDoubleMatrix1D
import riso.numerical.LBFGS
import iitb.CRF.RobustMath
import cern.colt.function.DoubleDoubleFunction
import cern.colt.function.DoubleFunction
import edu.jhu.jacana.align.AlignFeatureVector
import edu.jhu.jacana.align.AlignTrainRecord
import cern.colt.matrix.DoubleMatrix2D
import edu.jhu.jacana.align.util.AlignerParams
import edu.jhu.jacana.align.Alphabet
import edu.jhu.jacana.align.AlignSequence
import edu.jhu.jacana.align.AlignTrainData
import edu.jhu.jacana.align.evaluation.AlignEvaluator
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.collection.mutable.HashSet
import edu.jhu.jacana.align.feature.AlignFeature
import edu.jhu.jacana.align.feature.AlignFeatureOrderOne
import scala.collection.mutable.ArrayBuffer
import java.io.PrintWriter
import java.io.File

/**
 * A 1st-order linear chain CRF that does L2-regularized training with LBFGS and viterbi decoding.
 * This is a special CRF tuned for using word indices as states in natural language alignment tasks. 
 * @author Xuchen Yao
 *
 */
@SerialVersionUID(1L)
class LinearChainCRF (
        val featureAlphabet: Alphabet, var FeatureExtractors: Array[AlignFeature],
        val labelAlphabet: IndexLabelAlphabet) extends Serializable {
    val numY: Int = labelAlphabet.totalStates
    val numF: Int = featureAlphabet.size()
    // feature weights
    val lambda = new Array[Double](numF)
    @transient val gradLogli = new Array[Double](numF)
    @transient val diag = new Array[Double](numF)
    @transient val ExpF = new Array[Double](numF)
    @transient var lZx = 0.0
    
    // numY holds for training. During test, numY might be larger, deal with it, this is a TODO
    @transient val Mi_YY = new DenseDoubleMatrix2D(numY, numY)
    @transient val Ri_Y = new DenseDoubleMatrix1D(numY)
    // val DoubleMatrix1D beta_Y[];
    @transient val tmp_Y = new DenseDoubleMatrix1D(numY)
    
    @transient var beta_Y: Array[DenseDoubleMatrix1D] = null
    @transient var alpha_Y:Array[DenseDoubleMatrix1D] = null
    @transient var scale: DenseDoubleMatrix1D = null
    
    var instanceWts: Array[Double] = null
    
    var initMDone = false
    @transient var icall = 0
 
    /**
     * train the model with an optional evaluation on the dev data. 
     * 
     * @param devData print F1 on devData every 10 iterations
     * @param devModelFname save model to devModelFname.iterXX every 10 iterations
     */
    def train(trainDataList: List[AlignTrainRecord], devData:AlignTrainData = null, 
            devModelFname:String = null, devOutputFname:String = null) {
        var f:Double = 0.0
        var xtol:Double = 1.0e-16; // machine precision
        var iprint = Array[Int](AlignerParams.debugLvl-2, AlignerParams.debugLvl-1)
        var iflag = Array[Int](0)
        var variables = lambda
        
        var positiveConstraint:Boolean = (AlignerParams.prior == "exp")
        if (positiveConstraint) {
            variables = new Array[Double](lambda.length)
        }
        
        setInitValue(variables)
        var bestF1 = 0.0
        var bestIter = 0
        
        do {
        	AlignerParams.train = true
            icall += 1
            if (positiveConstraint) {
                var i = 0
                while (i < variables.length) {
                    lambda(i) = Math.exp(variables(i))
                    i += 1
                }
                f = computeFunctionGradient(trainDataList, lambda,gradLogli); 
                i = 0
                while (i < gradLogli.length) {
                    gradLogli(i) *= Math.exp(variables(i))
                    i += 1
                }
            } else {
                f = computeFunctionGradient(trainDataList, lambda,gradLogli); 
            }
            f = -1*f; // since the routine below minimizes and we want to maximize logli
            var j = 0
            while (j < lambda.length) {
                gradLogli(j) *= -1
                j += 1
            }
            
            try	{
                LBFGS.lbfgs (numF, AlignerParams.mForHessian, variables, f, gradLogli, false, diag, iprint, AlignerParams.epsForConvergence, xtol, iflag)
            } catch {
                case e:LBFGS.ExceptionWithIflag => 
	                System.err.println( "CRF: lbfgs failed.\n"+e );
	                if (e.iflag == -1) {
	                    System.err.println("Possible reasons could be: \n \t 1. Bug in the feature generation or data handling code\n\t 2. Not enough features to make observed feature value==expected value\n");
	                }
	                return
            }
            if (devData != null && icall % 10 == 0) {
               decode(devData) 
               var (f1,_,_,_) = AlignEvaluator.evaluate(devData)
               f1 *= 100
               if (f1 > bestF1) {
                   bestF1 = f1
                   bestIter = icall
               }
               //this.printFeatureWeights()
               if (devModelFname != null) {
                   saveModel(devModelFname+f".iter_$icall%d.F1_$f1%.1f")
               }
               if (devOutputFname != null) {
                   saveJSON(devData, devOutputFname+f".iter_$icall%d.F1_$f1%.1f.json")
               }
            }
        } while (( iflag(0) != 0) && (icall < AlignerParams.maxIters))
        if (bestF1 != 0.0) {
            val maxIter = AlignerParams.maxIters
            println(f"Best F1 of $bestF1%.1f %% at iteration $bestIter%d (total iterations: $maxIter%d)")
        }
    }
    
    protected def computeFunctionGradient(dataList: List[AlignTrainRecord], lambda:Array[Double], grad:Array[Double], expFVals:Array[Double] = null):Double = {
        var logli = 0.0
        if (grad != null) {
            logli = addPrior(lambda,grad,logli);
        }
        initMDone = false
        var i = 0
        while (i < dataList.size) {
            logli += sumProduct(dataList(i), lambda,grad,expFVals, false, i )
            i += 1
        }
        if (AlignerParams.debugLvl > 0) {
	        if (grad != null) System.err.println("Iter " + icall + " loglikelihood "+logli + " gnorm " + norm(grad) + " xnorm "+ norm(lambda));
	    }
        return logli
    }
    
    def getFeatureVectorArray(record: AlignSequence):Array[ArrayBuffer[AlignFeatureVector]] = {
        val pair = record.getPair
        val featureVectorArray = new Array[ArrayBuffer[AlignFeatureVector]](pair.featureVectors.length)
        // first add all saved 0th-order features
        var ff = 0
        while (ff < featureVectorArray.length) {
            featureVectorArray(ff) = new ArrayBuffer[AlignFeatureVector]()
            featureVectorArray(ff).append(pair.featureVectors(ff)) 
            ff += 1
        }
        // then add all to-be-extracted 1st-order features
	    for (extractor <- FeatureExtractors) {
	        if (extractor.isInstanceOf[AlignFeatureOrderOne]) {
	            val fvs = extractor.extract(record, featureAlphabet, labelAlphabet)
	            var ff = 0
	            while (ff < featureVectorArray.length) {
	                featureVectorArray(ff).append(fvs(ff)) 
	                ff += 1
	            }
	        }
	    }
	    return featureVectorArray
    }
    
    protected def sumProduct(record: AlignTrainRecord, lambda: Array[Double], grad: Array[Double], 
            expFVals: Array[Double], onlyForwardPass: Boolean, numRecord: Int): Double = {
        
        if (AlignerParams.logProcessing) {
            return sumProductLL(record, lambda, grad, expFVals, onlyForwardPass, numRecord)
        }
        
        val pair = record.getPair
        var len = pair.srcLen
        var doScaling = AlignerParams.doScaling
        var f = 0
        while (f < ExpF.length) {ExpF(f) = 0; f+=1}
        var instanceWt = if (instanceWts != null) instanceWts(numRecord) else 1.0
        // allocate alpha/beta everytime because later we call beta_Y(i-1).zSum() to do scaling
        // if we play 'cheap' and wanted to save memory allocation by calling:
        // if ((alpha_Y == null) || (alpha_Y.length < len+1)) {	allocateAlphaBeta(len+1)}
        // then beta_Y_i-1).zSum() would be wrong when a new sentence's length is shorter than already allocated.
        allocateAlphaBeta(len+1)
        scale = new DenseDoubleMatrix1D(len+1)
        
        alpha_Y(0).assign(1)
        // compute beta values in a backward scan.
        // also scale beta-values to 1 to avoid numerical problems.
        scale.setQuick(len, if (doScaling) numY else 1.0)
        beta_Y(len).assign(1/scale.getQuick(len))
        var i = len

        val featureVectorArray = getFeatureVectorArray(record)
        while (i > 0) {
            // compute the Mi matrix
            initMDone = computeLogMi(featureVectorArray(i-1),lambda,Mi_YY,Ri_Y,true, record.y(i-1));
            tmp_Y.assign(beta_Y(i))
            tmp_Y.assign(Ri_Y,MultFunc)
            // TODO: optimize this function
            RobustMath.Mult(Mi_YY, tmp_Y, beta_Y(i-1),1,0,false,null)
            
            // need to scale the beta-s to avoid overflow
            scale.setQuick(i-1, if (doScaling) beta_Y(i-1).zSum() else 1.0)
            if ((scale.getQuick(i-1) < 1) && (scale.getQuick(i-1) > -1))
                scale.setQuick(i-1, 1)
            MultConst.multiplicator = 1.0/scale.getQuick(i-1)
            beta_Y(i-1).assign(MultConst);
            i -= 1
        }
        
        var thisSeqLogli = 0.0
        i = 0
        while (i < len) {
            initMDone = computeLogMi(featureVectorArray(i),lambda,Mi_YY,Ri_Y,true, record.y(i));
            if (i > 0) {
                tmp_Y.assign(alpha_Y(i))
                RobustMath.Mult(Mi_YY, tmp_Y, alpha_Y(i+1),1,0,true,null);
                alpha_Y(i+1).assign(Ri_Y,MultFunc); 
            } else {
                alpha_Y(i+1).assign(Ri_Y);     
            }
            if ((grad !=null) || (expFVals!=null)) {
            	// find features that fire at this position..
                var j = 0
                val ins = pair.featureVectors(i)
	            while (j < ins.size()) {
	                var f = ins.index(j)
	                var yp = ins.y(j)
	                var yprev = ins.yprev(j)
	                var value = ins.value(j)
	                if ((grad != null) && (record.y(i) == yp) && (((i-1 >= 0) && (yprev == record.y(i-1))) || (yprev < 0))) {
	                    grad(f) += instanceWt*value
	                    thisSeqLogli += value*lambda(f)
	                }
	                if (yprev < 0) {
	                    ExpF(f) += alpha_Y(i+1).getQuick(yp)*value*beta_Y(i+1).getQuick(yp)
	                } else {
	                    ExpF(f) += alpha_Y(i).getQuick(yprev)*Ri_Y.getQuick(yp)*Mi_YY.getQuick(yprev,yp)*value*beta_Y(i+1).getQuick(yp);
	                }
	                j += 1
	            }
            }
            
            // now scale the alpha-s to avoid overflow problems.
            MultConst.multiplicator = 1.0/scale.getQuick(i+1)
            alpha_Y(i+1).assign(MultConst)
            i += 1
        }
        var Zx = alpha_Y(len).zSum()
        thisSeqLogli -= Math.log(Zx)
        // correct for the fact that alpha-s were scaled.
        i = 1
        while (i < len+1) {
            thisSeqLogli -= Math.log(scale.getQuick(i))
            i += 1
        }
        // update grad.
        if (grad != null) {
            f = 0
	        while (f < grad.length) {
	            grad(f) -= instanceWt*ExpF(f)/Zx
	            f += 1
	        }
        }
        if (expFVals!=null) {
            f = 0
            while (f < lambda.length) {
                expFVals(f) += ExpF(f)/Zx
                f += 1
            }
        }
        return thisSeqLogli*instanceWt
        
    }
    
    
    protected def sumProductLL(record: AlignTrainRecord, lambda: Array[Double], grad: Array[Double], 
            expFVals: Array[Double], onlyForwardPass: Boolean, numRecord: Int): Double = {
        	
    	var instanceWt =  if (instanceWts!=null) instanceWts(numRecord) else 1.0
    	
    	var f = 0
        while (f < ExpF.length) {
            ExpF(f) = RobustMath.LOG0
            f += 1
    	}
        
        var gradThisInstance =grad
        if ((instanceWt != 1) && (grad != null)) {
        	gradThisInstance = new Array[Double](grad.length)
        }
        
        var thisSeqLogli = sumProductInner(record,lambda,gradThisInstance ,onlyForwardPass, numRecord) //, ((grad != null)||(expFVals!=null))?fgenForExpVals:null);
        
        thisSeqLogli -= lZx
        
        // update grad.
        if (grad != null) {
            f = 0
            while (f < grad.length) {
                grad(f) -= RobustMath.exp(ExpF(f)-lZx)*instanceWt
                if (gradThisInstance != grad) {
                	grad(f) += gradThisInstance(f)*instanceWt
                }
                f += 1
            }
        }
        if (expFVals!=null) {
            f = 0
            while (f < expFVals.length) {
                expFVals(f) += RobustMath.exp(ExpF(f)-lZx)*instanceWt
                f += 1
            }
        }
        if (AlignerParams.debugLvl > 2) {
            System.err.println("Sequence "  + thisSeqLogli  + " log(Zx) " + lZx + " Zx " + Math.exp(lZx));
        }
        return if (grad == null) -lZx else thisSeqLogli * instanceWt
    }
    
    protected def sumProductInner(record: AlignTrainRecord, lambda: Array[Double], grad: Array[Double], 
            onlyForwardPass: Boolean, numRecord: Int): Double = {
        val pair = record.getPair
        var len = pair.srcLen
        if ((beta_Y == null) || (beta_Y.length < len+1)) {
           allocateAlphaBeta(len+1)
        }
        val featureVectorArray = getFeatureVectorArray(record)
        // compute beta values in a backward scan.
        // also scale beta-values to 1 to avoid numerical problems.
        if (!onlyForwardPass) {
            beta_Y = computeBetaArray(featureVectorArray, record, lambda)
        }
        alpha_Y(0).assign(0);
        var thisSeqLogli = 0.0
        var i = 0
        while (i < len) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(featureVectorArray(i),lambda,Mi_YY,Ri_Y,false, record.y(i))
            
            if (i > 0) {
                tmp_Y.assign(alpha_Y(i));
                RobustMath.logMult(Mi_YY, tmp_Y, alpha_Y(i+1),1,0,true,null)
                alpha_Y(i+1).assign(Ri_Y,SumFunc); 
            } else {
                alpha_Y(i+1).assign(Ri_Y);
            }

            if (grad != null) {
            // find features that fire at this position..
                // fgenForExpVals.startScanFeaturesAt(dataSeq, i);
                var j = 0
                val ins = pair.featureVectors(i)

                while (j < ins.size()) {
  	                var f = ins.index(j)
	                var yp = ins.y(j)
	                var yprev = ins.yprev(j)
	                var value = ins.value(j)

                    if ((grad != null) && (record.y(i) == yp) && (((i-1 >= 0) && (yprev == record.y(i-1))) || (yprev < 0))) {
                        grad(f) += value
                        thisSeqLogli += value*lambda(f)
                    }
                    //if (Math.abs(value) < Double.MIN_VALUE) continue;
                    if (value < 0) {
                        System.err.println("ERROR: Cannot process negative feature values in log domains: " 
                                + "either disable the '-trainer=ll' flag or ensure feature values are not -ve")
                        System.exit(-1)
                    }
                    if (yprev < 0) {
                        ExpF(f) = RobustMath.logSumExp(ExpF(f), alpha_Y(i+1).get(yp) + RobustMath.log(value) + beta_Y(i+1).get(yp));
                    } else {
                        ExpF(f) = RobustMath.logSumExp(ExpF(f), alpha_Y(i).get(yprev)+Ri_Y.get(yp)+Mi_YY.get(yprev,yp)+RobustMath.log(value)+beta_Y(i+1).get(yp));
                    }
                    j += 1
                }
            }
            
            if (AlignerParams.debugLvl > 2) {
                System.out.println("Alpha-i " + alpha_Y(i).toString());
                System.out.println("Ri " + Ri_Y.toString());
                System.out.println("Mi " + Mi_YY.toString());
                System.out.println("Beta-i " + beta_Y(i).toString());
            }
            i += 1
        }
        lZx = RobustMath.logSumExp(alpha_Y(len));
        return thisSeqLogli;
    }
    
    def computeBetaArray(fvArray: Array[ArrayBuffer[AlignFeatureVector]], record: AlignTrainRecord, lambda: Array[Double]): Array[DenseDoubleMatrix1D] = {
        val pair = record.getPair
        var len = record.length
        beta_Y(len).assign(0)
        var i = len 
        while (i > 0) {
            // compute the Mi matrix
            initMDone = computeLogMiTrainMode(fvArray(i-1),lambda,Mi_YY,Ri_Y,false,record.y(i-1))
            if (i > 1) {
	            tmp_Y.assign(beta_Y(i))
	            tmp_Y.assign(Ri_Y,SumFunc)
	            RobustMath.logMult(Mi_YY, tmp_Y, beta_Y(i-1),1,0,false,null);
            } else {
	            beta_Y(0).assign(beta_Y(1))
	            beta_Y(0).assign(Ri_Y,SumFunc)
            }
            i -= 1
        }
        return beta_Y;
    }
    
    def allocateAlphaBeta(size: Int) {
        beta_Y = new Array[DenseDoubleMatrix1D](size)
        var i = 0
        while (i < beta_Y.length) {
            beta_Y(i) = new DenseDoubleMatrix1D(numY)
            i += 1
        }
        alpha_Y = new Array[DenseDoubleMatrix1D](size)
        i = 0
        while (i < alpha_Y.length) {
            alpha_Y(i) = new DenseDoubleMatrix1D(numY)
            i += 1
        }
    }
    
    def computeLogMiTrainMode(fvBuffer: ArrayBuffer[AlignFeatureVector], lambda: Array[Double], 
            Mi_YY: DoubleMatrix2D, Ri_Y: DoubleMatrix1D , takeExp: Boolean, trueLabel:Int): Boolean = {
        return  computeLogMi(fvBuffer, lambda, Mi_YY, Ri_Y, takeExp, trueLabel) || initMDone
    }
    
    /**
     * Compute the Transition matrix Mi_YY/Ri_Y given a feature vector <code>ins</code>.
     * 
     * <code>Mi_YY</code> is the transition matrix from state <code>s</code> to state <code>s'</code>,
     * for feature functions in the form of f(s, s', x).
     * <code>Ri_Y</code> is the "transition" matrix from ANY state to state <code>s'</code>,
     * for feature functions in the form of f(s', x).
     * 
     * @param takeExp if false, return the log values of Mi/Ri
     * 
     * NOTE:
     * The design of separating Mi_YY and Ri_Y from the original CRF package was flawed: 
     * Mi_YY and Ri_Y should've been merged into a single Mi_YY. This will make it easier to 
     * compute alpha-beta in forward-backward, save memory and increase speed.
     * 
     * Merging Mi_YY and Ri_YY would be one of the future improvements on top of the priority list.
     * (TODO TODO TODO TODO)
     */
    def computeLogMi(fvBuffer: ArrayBuffer[AlignFeatureVector], lambda: Array[Double], 
            Mi_YY: DoubleMatrix2D, Ri_Y: DoubleMatrix1D , takeExp: Boolean, trueLabel:Int): Boolean = {
        
        initMDone = false;
        Mi_YY.assign(0);
        Ri_Y.assign(0);
        initMDone = computeLogMiInitDone(fvBuffer,lambda,Mi_YY,Ri_Y,0, trueLabel)
        if (takeExp) {
            var r = Ri_Y.size()-1
            while(r >= 0) {
                Ri_Y.setQuick(r, expE(Ri_Y.getQuick(r)))
                var c = Mi_YY.columns()-1
                while(c >= 0) {
                    Mi_YY.setQuick(r,c,expE(Mi_YY.getQuick(r,c)))
                    c -= 1
                }
                r -= 1
            }
        }
        
        return initMDone
    }
    
    def computeLogMiInitDone(fvBuffer: ArrayBuffer[AlignFeatureVector], lambda: Array[Double], Mi_YY: DoubleMatrix2D, 
            Ri_Y: DoubleMatrix1D, DEFAULT_VALUE: Double, trueLabel:Int): Boolean = {
        
        var mSet = false
        // when trueLabel < 0, it is decoding during test
        var doSoftmax = AlignerParams.softmaxTraining && trueLabel >= 0
        var indices = new HashSet[Int]()
        var RiString:Array[String] = null
        var MiString:Array[Array[String]] = null
        if (true && AlignerParams.debugLvl > 1 && trueLabel < 0) {
            // print out feature vector sums during decoding
            RiString = Array.fill[String](Ri_Y.size())("")
            MiString = Array.fill[String](Ri_Y.size(), Ri_Y.size())("")
        }
        for (ins <- fvBuffer) {
            var i = 0
            while (i < ins.size) {
                var f = ins.index(i)
                var yp = ins.y(i)
                var yprev = ins.yprev(i)
                var value = ins.value(i)
                if (doSoftmax)
                    indices.add(yp)
                if (yprev == -1) {
                    // this is a single state feature.
                    
                    // if default value was a negative_infinity, need to
                    // reset to.
                    var oldVal = Ri_Y.get(yp)
                    if (oldVal == DEFAULT_VALUE)
                        oldVal = 0
                	Ri_Y.setQuick(yp,oldVal+lambda(f)*value)
    		        if (RiString != null) {
    		            RiString(yp) += " + %.3f (%s) x %.3f ".format(lambda(f), featureAlphabet.getString(f), value)
    		        }
                } else {
                    //if (Ri_Y.get(yp) == DEFAULT_VALUE)
                     //   Ri_Y.set(yp,0);
                    var oldVal = Mi_YY.get(yprev,yp)
                    if (oldVal == DEFAULT_VALUE) {
                        oldVal = 0
                    }
                    Mi_YY.setQuick(yprev,yp,oldVal+lambda(f)*value);
                    mSet = true;
    		        if (MiString != null) {
    		            MiString(yprev)(yp) += " + %.3f (%s) x %.3f ".format(lambda(f), featureAlphabet.getString(f), value)
    		        }
                }
                
                i += 1
          }
        }
        if (MiString != null) {
            for (yp <- 0 until RiString.length) {
                RiString(yp) += " = %.3f".format(Ri_Y.getQuick(yp))
            	for (yprev <- 0 until RiString.length) {
            		MiString(yprev)(yp) += " = %.3f".format(Mi_YY.getQuick(yprev, yp))
            	}
            }
            print1DsumFV(RiString)
            print2DsumFV(MiString)
		}
        
        if (doSoftmax) {
            for (yp <- indices) {
                Ri_Y.setQuick(yp, Ri_Y.getQuick(yp)+(if(trueLabel==yp) 0.0 else AlignerParams.softmaxCostCoefficient)) 
            }
        }
        
        return mSet
    }
    
    def print1DsumFV(RiString: Array[String]) {
        for (yp <- 0 until RiString.length) {
            println("%d: %s".format(yp, RiString(yp)))
        }
    }
    
    def print2DsumFV(MiString: Array[Array[String]]) {
       	for (yprev <- 0 until MiString.length) {
       		for (yp <- 0 until MiString(yprev).length) {
        	    println("%d -> %d: %s".format(yprev, yp, MiString(yprev)(yp)))
        	}
       	}
    }
    
    protected def addPrior(lambda: Array[Double], grad: Array[Double], logli: Double): Double = {
        var prior = logli
        if (AlignerParams.prior.equalsIgnoreCase("exp")) {
            var f = 0
            while (f < lambda.length) {
                grad(f) = -1*AlignerParams.invSigmaSquare
                prior -= lambda(f)*AlignerParams.invSigmaSquare
                f += 1
            }
        } else if (AlignerParams.prior.equalsIgnoreCase("laplaceApprox")) {
            var f = 0
            var approxL = 0.0
            while (f < lambda.length) {
                approxL = Math.sqrt(lambda(f)*lambda(f)+1e-3)
                grad(f) = -1*lambda(f)/approxL*AlignerParams.invSigmaSquare
                prior -= AlignerParams.invSigmaSquare*approxL
                f += 1
            }
        } else  {
            var f = 0
            while (f < lambda.length) {
                grad(f) = -1*lambda(f)*AlignerParams.invSigmaSquare
                prior -= ((lambda(f)*lambda(f))*AlignerParams.invSigmaSquare)/2;
                f += 1
            }
        }
        return prior;
    }
    
    protected def setInitValue(lambda: Array[Double]) {
        
        if (AlignerParams.initValuesUseExisting) {
            // use existing values of lambda from the model as starting point.
            return;
        } else {
	        for (j <- 0 until lambda.length) {
	            lambda(j) = AlignerParams.initValue
	        }
        }
    }
    
     def expE(value: Double):Double =  {
        val pr = RobustMath.exp(value);
        if (java.lang.Double.isNaN(pr) || java.lang.Double.isInfinite(pr)) {
            try {
                throw new Exception("Overflow error when taking exp of " + value + "\n Try running the CRF with the following option \"trainer ll\" to perform computations in the log-space.");
            } catch {
                case e:Exception => 
                System.out.println(e.getMessage());
                e.printStackTrace();
                return java.lang.Double.MAX_VALUE;
            }
        }
        return pr;
    }       
     
	def norm(ar:Array[Double]): Double = {
	    var v = 0.0
	    var f = 0
	    while (f < ar.length) {
	        v += ar(f)*ar(f);
	        f += 1
	    }
	    return Math.sqrt(v);
	}
		
    def printFeatureWeights(sorted: Boolean = true) {
        println("====== Features : Weights ========")
        if (sorted) {
            val weightFeatureList = for (i <- 0 until lambda.length) yield {(lambda(i), featureAlphabet.getString(i))}
            var i = 0
            for ((w,f) <- weightFeatureList.sorted.reverse) {
    			println("%d\t%s\t:\t%.4f".format(i, f, w))
    			i += 1
            }
        } else {
    		for (i <- 0 until lambda.length) {
    			println("%d\t%s\t:\t%.4f".format(i, featureAlphabet.getString(i), lambda(i)))
    		}
        }
    }
	   
    def saveModel(fileName: String) {
        val output = new ObjectOutputStream(new FileOutputStream(fileName))
        output.writeObject(this)
        output.close()
        println("model saved to " + fileName)
    }
 	   
    def saveJSON(devData:AlignTrainData, fileName: String) {
        
    	var writer: PrintWriter = new PrintWriter(new File(fileName))
        writer.print(devData.toJSON) 
        writer.close()
        println("json saved to " + fileName)
    }
    
	////////////////////////////// Viterbi /////////////////////////////////
    def decode(record: AlignSequence): Double = {
        AlignerParams.train = false
        var viterbi = new ViterbiDecoder(this)
        var score = viterbi.decode(record);
        return score;
    }
    
    def decode(devData: AlignTrainData) {
        AlignerParams.train = false
        devData.startScan
        while (devData.hasMoreRecords) {
            val devRecord = devData.nextRecord
            val goldLabels = devRecord.asInstanceOf[AlignTrainRecord].getGoldLabelsPerToken().toList

            decode(devRecord)
            
            val taggedLabels = devRecord.asInstanceOf[AlignTrainRecord].getLabelsPerToken().toList
            println(devRecord.getPair.id)
            println("gold label: " + goldLabels)
	        println("decoded labels: " + taggedLabels)
        }
    }
    
}

object MultFunc extends DoubleDoubleFunction {
    override def apply(a: Double, b: Double):Double = {return a*b;}
}
object SumFunc extends DoubleDoubleFunction {
    override def apply(a: Double, b: Double):Double = {return a+b;}
}
object MultConst extends DoubleFunction {
    var multiplicator:Double = 1.0
    override def apply(a: Double):Double = {return a*multiplicator;}
}
