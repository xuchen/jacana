package edu.jhu.jacana.align.itg

class Span (var fr:Int = 0, var to:Int = 0) {}

object AType extends Enumeration {
    type AType = Value
    val ATOMIC, STRAIGHT, INVERTED = Value
}


class ANode {
    import AType._
    
    var q:Span = null
    var d:Span = null
    var score = 0.0
    var atype:AType = ATOMIC
    var straight_only = false
    var front:ANode = null
    var back:ANode = null
    
    override def toString():String = {return ""}
    
    def recursiveToString(q_tokens:List[String],  d_tokens:List[String]):String = {
        return ""
    }

}

