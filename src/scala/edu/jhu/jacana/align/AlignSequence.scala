/**
 *
 */
package edu.jhu.jacana.align

/**
 * @author Xuchen Yao
 *
 */
trait AlignSequence {
    def length(): Int
    def y(i:Int): Int
    /** The type of x is never interpreted by the CRF package. This could be useful for your FeatureGenerator class */ 
    def x(i:Int): Object
    def set_y(i:Int, label:Int)
    // clears out all y's
    def zero_y()
    def getPair(): AlignPair
    def y_length(): Int
    def set_score(s:Double)
}

trait SegmentAlignSequence extends AlignSequence {
    /** get the end position of the segment starting at segmentStart */
    def getSegmentEnd(segmentStart: Int): Int 
    /** set segment boundary and label */
    def setSegment(segmentStart: Int, segmentEnd: Int, y: Int)
}


trait TrainAlignRecord extends SegmentAlignSequence {
    def numSegments(): Int // number of segments in the record
    def labels(): Array[Int] // labels of each segment
    def tokens(segmentNum: Int): Array[String] // array of tokens in this segment  
    def numSegments(label: Int): Int // number of segments of given label
    def tokens(label: Int, i: Int): Array[String] // i-th segment of given label
}

trait AlignIter {
    def startScan()
    def hasNext(): Boolean
    def next(): AlignSequence
}

trait TrainAlignData extends AlignIter {
    def size(): Int   // number of training records
    def startScan(); // start scanning the training data
    def hasMoreRecords(): Boolean 
    def nextRecord(): TrainAlignRecord
    def hasNext(): Boolean
    def next(): AlignSequence
}