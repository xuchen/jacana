package edu.jhu.jacana.align.speedtest

//https://issues.scala-lang.org/browse/SI-4633

object ForWhile {

  def wr1 = {
    var Z = 0
    var a = 0
    while (a < 1073741824) {
      Z += a
      a += 1
    }
    Z
  }
  
  def wr2 = {
    var Z = 0
    var b = 0
    while (b < 1048576) {
      var a = 0
      while (a < 1024) {
        Z += a + 1024*b
        a += 1
      }
      b += 1
    }
    Z
  }
  
  def wa1024 = {
    val ar = Array.range(0,1024)
    var Z = 0
    var a = 0
    while (a < 1048576) {
      var b = 0
      while (b < ar.length) {
        Z += ar(b) + 1024*a
        b += 1
      }
      a += 1
    }
    Z
  }

  def fr1 = {
    var Z = 0
    for (a <- 0 until 1073741824) Z += a
    Z
  }
  
  def fr2 = {
    var Z = 0
    for (a <- 0 until 1048576; b <- 0 until 1024) Z += b + 1024*a
    Z
  }
  
  def fa1024 = {
    val ar = Array.range(0,1024)
    var Z = 0
    for (a <- 0 until 1048576; z <- ar) Z += z + 1024*a
    Z
  }
  
  def test(f: () => Int) = {
    val t0 = System.nanoTime
    val i = f()
    val dt = (System.nanoTime - t0)*1e-9
    printf("%10d took %6.3f s\n",i,dt)
  }
  
  def main(args: Array[String]) {
    val N = try { args(0).toInt } catch { case _: Throwable => 4 }
    val tests: List[(String,()=>Int)] = List(
      "while range 1" -> wr1 _,
      "while range 2" -> wr2 _,
      "while array 1024" -> wa1024 _,
      "for   range 1" -> fr1 _,
      "for   range 2" -> fr2 _,
      "for   array 1024" -> fa1024 _
    )
    var n = 0
    while (n < N) {
      tests.foreach { case (name, f) => printf("%-16s: ",name); test(f) }
      println
      n += 1
    }
  }
}