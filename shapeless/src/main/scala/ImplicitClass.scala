import scala.language.implicitConversions

object ImplicitClass extends App{

  implicit class BetterString(val s: String) {
    def hello: String = s"Hello, ${s.capitalize}"
    def plusOne = s.toInt + 1

  }
  println("hi".hello)
  println("4".plusOne)
}