/**
 *
 */

import tiscaf._
import edu.jhu.jacana.align.AlignTestRecord
import edu.jhu.jacana.align.aligner.FlatTokenAligner
import edu.jhu.jacana.align.aligner.AbstractFlatAligner
import edu.jhu.jacana.align.aligner.FlatAligner
import java.io.File
import java.io.PrintWriter
import org.apache.commons.lang3.StringEscapeUtils
import edu.jhu.jacana.util.WebUtils

/**
 *
 */
object Demo extends HServer with App {
    
  var aligner = new FlatAligner()
  var modelFile = if (args.length > 0) args(0) else "/tmp/flatTokenAligner.model"
  aligner.initParams()
  aligner.readModel(modelFile)
  
  var port = if (args.length > 1) args(1).toInt else 8080
  var logFolder:String = "."
  
  // create log folder
  if (args.length > 2)
      logFolder = args(2)
  else if (args.length == 1 && !args(1).matches("\\d+"))
      logFolder = args(1)
  val f = new File(logFolder)
  if (!f.exists())
      f.mkdirs()
  var counter = 0
  var logWriter: PrintWriter = null
  
  
  def decode(sent1: String, sent2: String): String = {
      val record = new AlignTestRecord(sent1, sent2, labelAlphabet=aligner.getLabelAlphabet)
      aligner.decode(record)
      counter += 1
      val json = record.toJSON(counter.toString)
      writeLog(json)
      return addSuccessCode(json)
  }
  
  def getLogFilenameByDate():String = {
      // outputs sth like 2013-10-24.11-54-04
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd.HH-mm-ss")
      return logFolder + "/" + format.format(new java.util.Date()) + ".json"
  }
  
  def writeLog(json:String) {
      def startLog() {
    	  var logFile = getLogFilenameByDate()
    	  logWriter = new PrintWriter(new File(logFile), "UTF-8")
    	  logWriter.write("[\n")
      }
      if (logWriter == null) {
          startLog()
      }
      
      if (counter % 1000 != 0) {
    	  if (counter % 1000 != 1) logWriter.print("\t,\n")
    	  logWriter.print(json)
      } else {
    	  logWriter.print(json)
		  logWriter.write("]\n")
	      logWriter.close()
          startLog()
      }
      logWriter.flush()
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
  if (logWriter != null) {
	  logWriter.write("\n]\n")
	  logWriter.close()
  }

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
      
    talk.setCharacterEncoding("UTF-8")
    //talk.setContentType("application/x-www-form-urlencoded; charset=UTF-8")
    talk.setContentType("application/json; charset=UTF-8")
    val sentences = talk.req.param("sentences").get
    val Array(sent1, sent2) = sentences.split("""###""")
    // val alignJson = WebUtils.encodeURIComponent(Demo.decode(sent1, sent2))
    val alignJson = Demo.decode(sent1, sent2)

    talk.setContentLength(alignJson.getBytes("UTF-8").length).write(alignJson)
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