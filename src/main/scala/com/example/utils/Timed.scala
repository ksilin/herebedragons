package com.example.utils

trait Timed {
  trait Timed {
    def timed[A](name: String = "")(f: => A): A = {
      val s   = System.nanoTime
      val ret = f
      println(s"time for $name " + (System.nanoTime - s) / 1e6 + "ms")
      ret
    }
  }
}
