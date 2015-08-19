package edu.jhu.jacana.align.itg

import scala.collection.mutable.ArrayBuffer
import edu.jhu.jacana.align.itg.AType._


class Aligner {

    var allocated = new ArrayBuffer[ANode]()
    
    def createANode(f:ANode, b:ANode, t:AType):ANode = {
        val a = createANode()
        a.q = new Span(Math.min(f.q.fr, b.q.fr), Math.max(f.q.to, b.q.to))
        a.d = new Span(f.d.fr, b.d.to)
        a.front = f; a.back = b; a.atype = t
        return a
    }

    def createANode():ANode = {
        val a = new ANode()
        allocated.append(a)
        return a
    }
}