/**
 *
 */
package edu.jhu.jacana.align.util

import edu.jhu.jacana.nlp.OpenNLP

/**
 * @author Xuchen Yao
 *
 */
object AlignerParams {

    def initSettings() {
    	OpenNLP.setDomain(OpenNLP.DOMAIN.GENERAL)
    }

	var shallowProcess: Boolean = true
	
	var parallel: Boolean = true

	/** the max (source) phrase length for Semi-CRF.
	 *  in token-based alignment, its value defaults to 1
	 *  in phrase-based alignment, defaults to 4
	 **/
	var maxSourcePhraseLen = 1
	
	/**
	 * the max target phrase length for CRF or Semi-CRF.
	 *  in token-based alignment, its value defaults to 1
	 *  in phrase-based alignment, defaults to 4
	 */
	var maxTargetPhraseLen = 1
	
	// naive, semi-markov
	var modelGraph = "naive"
	    
	// true when semi-markov
	var phraseBased = false
	
	// weight that favors longer phrasal alignment
	// <= 1.0: mostly token alignment
	// >= 2.0: over phrasal-alignment
	var phraseWeight = 1.0
	/**
	 * supported: gaussian, exp, laplaceApprox
	 */
	var prior = "gaussian"
	
	/** initial value for all the lambda arrays */
	var initValue = 1.0
	
	var initValuesUseExisting = false
	/** penalty term for likelihood function is ||lambda||^2*invSigmaSquare/2
	set this to zero, if no penalty needed
	 */
	var invSigmaSquare = 0.01
	/** Maximum number of iterations over the training data during training */
	var maxIters = 300
	/** Convergence criteria for finding optimum lambda using BFGS */
	var epsForConvergence = 0.001
	/** The number of corrections used in the BFGS update. */
	var mForHessian = 7

	var trainerType = ""
	    
	var logProcessing  = false

	var inferenceType = "Viterbi"

	var beamSize = 1

	var debugLvl = 1 // controls amount of status information output

	var doScaling = true;

	var doRobustScale = false;

	var reuseM = false;
	
	var segmentViterbi = false

	/** This when set to true will only allow transitions
	 *  for which there is a corresponding edge feature
	 */
	var onlyFeatureBasedTransitions = false
	
	// when decoding, set to false, this will freeze features to be
	// those only extracted during training
	var train = true
	
	def setMaxSourcePhraseLen(max: Int) {maxSourcePhraseLen = max}
	
	def setMaxTargetPhraseLen(max: Int) {maxTargetPhraseLen = max}
	
	// Softmax-Margin CRFs: Training Log-Linear Models with Cost Functions
	// by Kevin Gimpel and Noah A. Smith, NAACL 2010
	var softmaxTraining = true
	
	var softmaxCostCoefficient = 1.0
	
	def parseParameters(opts: java.util.Properties) {
	    
		if (opts.getProperty("modelGraph") != null) {
			modelGraph = opts.getProperty("modelGraph");
			if (modelGraph == "semi-markov") {
			    phraseBased = true
			    maxSourcePhraseLen = 3
			    maxTargetPhraseLen = 3
			    phraseWeight = 1.5
			}
		}
		if (opts.getProperty("initValue") != null) {
			initValue = opts.getProperty("initValue").toDouble
		} 
		if (opts.getProperty("maxIters") != null) {
			maxIters = opts.getProperty("maxIters").toInt
		} 
		if (opts.getProperty("invSigmaSquare") != null) {
			invSigmaSquare = opts.getProperty("invSigmaSquare").toDouble
		} 
		if (opts.getProperty("debugLvl") != null) {
			debugLvl = opts.getProperty("debugLvl").toInt
		} 
		if (opts.getProperty("scale") != null) {
			doScaling = opts.getProperty("scale").equalsIgnoreCase("true");
		}
		if (opts.getProperty("robustScale") != null) {
			doRobustScale = opts.getProperty("robustScale").equalsIgnoreCase("true");
		}
		if (opts.getProperty("epsForConvergence") != null) {
			epsForConvergence = opts.getProperty("epsForConvergence").toDouble
		}
		if (opts.getProperty("mForHessian") != null) {
			mForHessian = opts.getProperty("mForHessian").toInt
		}
		if (opts.getProperty("trainer") != null) {
			trainerType = opts.getProperty("trainer");
			if (trainerType == "ll")
			    logProcessing = true
		}
		if (opts.getProperty("inferenceType") != null) {
			inferenceType = opts.getProperty("inferenceType");
			//System.out.println("InferenceType:" + inferenceType);
		}
		if (opts.getProperty("beamSize") != null) {
			beamSize = opts.getProperty("beamSize").toInt
		}
		if (opts.getProperty("maxSourcePhraseLen ") != null) {
			maxSourcePhraseLen = opts.getProperty("maxSourcePhraseLen").toInt
		}
		reuseM = opts.getProperty("reuseM","false").toBoolean
		onlyFeatureBasedTransitions = opts.getProperty("onlyFeatureTransitions","false").toBoolean
		if (opts.getProperty("segmentViterbi") != null) {
			segmentViterbi = opts.getProperty("segmentViterbi").toBoolean
		}
		if (opts.getProperty("softmaxTraining") != null) {
			softmaxTraining = opts.getProperty("softmaxTraining").toBoolean
		}
		if (opts.getProperty("softmaxCostCoefficient") != null) {
			softmaxCostCoefficient = opts.getProperty("softmaxCostCoefficient").toDouble
		}

		parallel = opts.getProperty("parallel","true").toBoolean
	}  
}