/**
 *
 */
package edu.jhu.jacana.align.tuple.reverb

import edu.jhu.jacana.align.reader.ShallowJsonReader
import edu.jhu.jacana.align.AlignPair
import edu.jhu.jacana.align.reader.ShallowJson
import edu.washington.cs.knowitall.commonlib.Range
import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction
import scala.collection.mutable.ArrayBuffer


/**
 * converts the manually labeled MSR06/Edinburgh++ corpora
 * to aligned tuples.
 * @author Xuchen Yao
 *
 */
object ConvertToTupleAlign {
    implicit val formats = DefaultFormats
    
    var cPair = 0
    var cPosTuple = 0
    var cNegTuple = 0
    
    def convert(pair: AlignPair, sj:ShallowJson):AlignedTuples = {
       val srcExtrs = ReverbTupleExtractor.extract(pair.src) 
       val tgtExtrs = ReverbTupleExtractor.extract(pair.tgt) 
       val tuples = new ArrayBuffer[AlignedTuple]
       if (srcExtrs.size > 0 && srcExtrs(0).getSentence().getRange().getEnd()+1 == pair.srcLen &&
           tgtExtrs.size > 0 && tgtExtrs(0).getSentence().getRange().getEnd()+1 == pair.tgtLen) {
           // length matching
           // even though we used a whitespace tokenizer, for some weird reason,
           // ReVerb still can't tokenize some special punctuations such as (maybe)
           // '' (or -lrb-), so we add a special check here

    	   for (srcExtr <- srcExtrs) {
    	    val srcTuple =  extraction2tuple(srcExtr)
    	    
    	    for (tgtExtr <- tgtExtrs) {
    	    
    	        val tgtTuple = extraction2tuple(tgtExtr)
    	        if (rangeIsAligned(srcExtr.getRelation().getRange(), tgtExtr.getRelation().getRange(), pair) &&
    	            !(srcExtr.getRelation().toString() == tgtExtr.getRelation().toString() &&
    	              srcExtr.getArgument1().toString() == tgtExtr.getArgument1().toString() &&
    	              srcExtr.getArgument2().toString() == tgtExtr.getArgument2().toString())) {
    	              // relations are aligned, and they are not identical tuples
    	              if ((rangeIsAligned(srcExtr.getArgument1().getRange(), tgtExtr.getArgument1().getRange(), pair) &&
    	             	 rangeIsAligned(srcExtr.getArgument2().getRange(), tgtExtr.getArgument2().getRange(), pair) )) {
    	             	 val tuple = AlignedTuple(srcTuple, tgtTuple, false, true)
    	 		       	 tuples += tuple
    	 		       	 cPosTuple += 1
    	              } else if ((rangeIsAligned(srcExtr.getArgument1().getRange(), tgtExtr.getArgument2().getRange(), pair) &&
    	             	 rangeIsAligned(srcExtr.getArgument2().getRange(), tgtExtr.getArgument1().getRange(), pair) )) {
    	             	 val tuple = AlignedTuple(srcTuple, tgtTuple, true, true)
    	 		       	 tuples += tuple
    	 		       	 cPosTuple += 1
    	              } else {
    	             	 val tuple = AlignedTuple(srcTuple, tgtTuple, false, false)
    	 		       	 tuples += tuple
    	 		       	 cNegTuple += 1
    	              }
    	        }
    	    }
       }
       }
       return AlignedTuples(sj, tuples.toList)
    }
    
    def rangeIsAligned(r1:Range, r2:Range, pair: AlignPair):Boolean = {
        return AlignPair.sumRectAt(pair.alignMatrix, r1.getStart(), r1.getEnd(), r2.getStart(), r2.getEnd()) > 0
    }
    
    def extraction2tuple(extr:NormalizedBinaryExtraction): Tuple = {
    	Tuple(extr.getArgument1().toString(), 
              extr.getRelation().toString(),
              extr.getArgument2().toString())
    }

    def main(args: Array[String]): Unit = {
        
        /*
         * train:
         * total pairs: 2096
		 * total tuples: 1382
		 * positive: 616 (0.45)
		 * negative: 766 (0.55)
         * 
         * test:
         * total pairs: 524
		 * total tuples: 325
		 * positive: 151 (0.46)
		 * negative: 174 (0.54)
		 * 
		 * note: the above counting didn't include "identical" tuples
		 * (which are much more than the numbers you see above)
         */
        
        // no processing, no tokenization (== whitespace tokenization)
		val reader = new ShallowJsonReader("alignment-data/all/msr.edinburgh.train.json", false, false)
		//val reader = new ShallowJsonReader("alignment-data/edinburgh/t.json", true)
		val origJsons = reader.getShallowJsons()
		val jsons = new ArrayBuffer[AlignedTuples]()
		var i = 0
		for (alignedPair <- reader) {
			cPair += 1
		    println(alignedPair.id)
			val tupleJson = convert(alignedPair, origJsons(i))
			if (tupleJson.tuples.size > 0) {
			    jsons += tupleJson
			}
			i += 1
		}

        val totalTuples = cPosTuple + cNegTuple
		
		println(pretty(render(Extraction.decompose(jsons))))
		println("total pairs: " + cPair)
		println("total tuples: " + totalTuples)
		println(f"\tpositive: $cPosTuple (${cPosTuple*1.0/totalTuples}%.2f)")
		println(f"\tnegative: $cNegTuple (${cNegTuple*1.0/totalTuples}%.2f)")
    }

}

case class Tuple(var arg1:String, var rel:String, var arg2:String) {
    override def toString(): String = {
        return s"($arg1, $rel, $arg2)"
    }
}
case class AlignedTuple(src:Tuple, tgt:Tuple, var cross:Boolean, align:Boolean) {
    override def toString(): String = {
        return "align: "+align+"\t"+src+" <-> "+tgt
    }
}
case class AlignedTuples(orig:ShallowJson, tuples:List[AlignedTuple])