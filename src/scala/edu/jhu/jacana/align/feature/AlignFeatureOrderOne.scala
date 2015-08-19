package edu.jhu.jacana.align.feature

import edu.jhu.jacana.align.AlignPair

/**
 * 1st order Markovian features. In phrase-based models, Markovian features take
 * a lot of space (up to 150GB when phrase length is 3). We make a compromise here
 * by not saving the feature values, but extract them on the fly (Markovian features
 * are cheap to extract anyways; they need not lexical resources).
 */
abstract class AlignFeatureOrderOne extends AlignFeature {

    // the implementation is actually embedded in AlignFeature.extract()
}