/**
 *
 */
package edu.jhu.jacana.align

//import org.apache.mahout.math.map.OpenIntObjectHashMap
//import org.apache.mahout.math.map.OpenObjectIntHashMap
//import org.apache.mahout.math.map.OpenIntDoubleHashMap
import edu.jhu.jacana.align.util.Loggable
import gnu.trove.list.array.TIntArrayList
import gnu.trove.list.array.TDoubleArrayList
import gnu.trove.set.hash.TIntHashSet
import edu.jhu.jacana.align.util.AlignerParams
import gnu.trove.map.hash.TIntObjectHashMap


/**
 * @author Xuchen Yao
 *
 */
class AlignFeatureVector extends Loggable {
    
    val span2featureIds = new TIntObjectHashMap[TIntArrayList]()
    val span2prevStates = new TIntObjectHashMap[TIntArrayList]()
    val span2currStates = new TIntObjectHashMap[TIntArrayList]()
    val span2featureValues = new TIntObjectHashMap[TDoubleArrayList]()
    
    span2featureIds.put(1, new TIntArrayList())
    span2prevStates.put(1, new TIntArrayList())
    span2currStates.put(1, new TIntArrayList())
    span2featureValues.put(1, new TDoubleArrayList())
    
    def initBySpan(span: Int) {
        span2featureIds.put(span, new TIntArrayList())
        span2prevStates.put(span, new TIntArrayList())
        span2currStates.put(span, new TIntArrayList())
        span2featureValues.put(span, new TDoubleArrayList())
    }
    
    val featureIds1 = span2featureIds.get(1)
    val prevStates1 = span2prevStates.get(1)
    val currStates1 = span2currStates.get(1)
    val featureValues1 = span2featureValues.get(1)
    val stateSet = new TIntHashSet()
    
    var currSpan = 1
    
    def setCurrSpan(span: Int) {currSpan = span}
    def hasCurrSpan(span: Int):Boolean = {return span2featureIds.containsKey(span)}
    
    /*
     * the span of this feature on the source side.
     * In token-based alignment, defaults to 1.
     * In phrase-based alignment, max is set by observed max span in training data 
     */
    //val sourceSpan = new TIntArrayList()
    
//    def addFeature(featureName:String, prevState: Int, currState: Int, value: Double, featureAlphabet: Alphabet) {
//        if (AlignerParams.train || (!AlignerParams.train && featureAlphabet.contains(featureName))) {
//            // if train, or if test, and we've seen this feature in train
//	        featureIds1.add(featureAlphabet.get(featureName))
//	        prevStates1.add(prevState)
//	        currStates1.add(currState)
//	        stateSet.add(currState)
//	        featureValues1.add(value)
//        }
//    }
 
    def addFeature(featureName:String, prevState: Int, currState: Int, value: Double, span: Int = 1, featureAlphabet: Alphabet) {
        if (AlignerParams.train || (!AlignerParams.train && featureAlphabet.contains(featureName))) {
	            // if train, or if test, and we've seen this feature in train
	        if (!span2featureIds.containsKey(span))
	            initBySpan(span)
	        span2featureIds.get(span).add(featureAlphabet.get(featureName))
	        span2prevStates.get(span).add(prevState)
	        span2currStates.get(span).add(currState)
	        stateSet.add(currState)
	        span2featureValues.get(span).add(value)
        }
    }
    
    def numStates() = stateSet.size()
    
    def size(): Int = {
        val fv = span2featureIds.get(currSpan)
        return if (fv == null) 0 else fv.size()
    }
    
    def index(i:Int) = span2featureIds.get(currSpan).getQuick(i)
    
    def y(i:Int) = span2currStates.get(currSpan).getQuick(i)
    
    def yprev(i:Int) = span2prevStates.get(currSpan).getQuick(i)
    
    def value(i:Int) = span2featureValues.get(currSpan).getQuick(i)
    
    //def srcSpan(i: Int) = sourceSpan.getQuick(i)
    
    //TODO: need to write a freeze function to convert all ArrayList to Array
    //thus guaranteeing constant time in get()
//    val featureVector = new OpenIntObjectHashMap[Array[Int]]()
//    val featureValues = new OpenIntDoubleHashMap()
//    
//    def addFeature(featureName:String, prevY: Int, currY: Int, value: Double) {
//        val featureId = FeatureAlphabet.get(featureName)
//        if (featureValues.containsKey(featureId))
//            log.warn("%s (%d, %d) is already added to feature vector".format(featureName, prevY, currY))
//        featureVector.put(featureId, Array[Int](prevY, currY))
//        featureValues.put(featureId, value)
//    }
    
}

