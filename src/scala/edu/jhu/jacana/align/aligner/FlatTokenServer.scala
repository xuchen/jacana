/**
 *
 */
package edu.jhu.jacana.align.aligner

import tiscaf._
import edu.jhu.jacana.align.AlignTestRecord

/**
 * @author Xuchen Yao
 *
 */
object FlatTokenServer extends HServer with App {
    

    println(System.getProperty("java.class.path"))
  var aligner:AbstractFlatAligner = new FlatTokenAligner()
  var modelFile = if (args.length > 0) args(0) else "/tmp/flatTokenAligner.model"
  aligner.readModel(modelFile)
  
  var port = if (args.length > 1) args(1).toInt else 8080
  
  def decode(sent1: String, sent2: String): String = {
      val record = new AlignTestRecord(sent1, sent2, labelAlphabet=aligner.getLabelAlphabet)
      aligner.decode(record)
      return addSuccessCode(record.toJSON())
  }
  
  def addSuccessCode(json: String): String = {
    // {"success":true,"countryInfo":{"code":"001","name":"USA","continent":"America","region":"North America","lifeExpectancy":80.0,"gnp":100.0}}
      return """{"success":true,"alignment":""" + json + "}"
  }
  
  def apps = Seq(AlignerApp, StaticAlignerApp)

  def ports = Set(port)

  // do not start the stop thread
  override protected def startStopListener { }

  start
  println("access it from http://localhost:"+port)

  println("press enter to stop...")
  Console.readLine

  stop
}

/** The application that serves the pages */
object AlignerApp extends HApp {

  def resolve(req: HReqData): Option[HLet] = {
      req.param("sentences") match {
    case Some(s) if (!s.isEmpty) => Some(AlignerLet)
    case _             => None
  }
  }

}

object AlignerLet extends HSimpleLet {

      
  def act(talk: HTalk) {
      
    val sentences = talk.req.param("sentences").get
    val Array(sent1, sent2) = sentences.split("""###""")
    val alignJson = FlatTokenServer.decode(sent1, sent2)
    
    //val myString = """{"success":true,"countryInfo":{"code":"001","name":"USA","continent":"America","region":"North America","lifeExpectancy":80.0,"gnp":100.0}}"""

    // simply return the current server time
    talk.setContentLength(alignJson.length)
      .write(alignJson)
  }

}

/** Simply servers the resource from the classpath */
object StaticAlignerApp extends HApp {

  override def buffered : Boolean  = true // ResourceLet needs buffered or chunked be set

  def resolve(req: HReqData) = Some(StaticAlignerLet) // generates 404 if resource not found
}

object StaticAlignerLet extends let.ResourceLet {
  protected def dirRoot          = ""
  override protected def uriRoot = ""
  override protected def indexes = List("AlignmentServer.html")
}