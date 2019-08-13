import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Failure

object future extends App {
  implicit val ec = scala.concurrent.ExecutionContext.global

  val f = Future {
    future.factorial(-1)
  }

  f onComplete {
    case result => println(f)
  }

  /*f onFailure {
    case result => println("Enter positive number")
  }*/

  def factorial(n: Int, fact: Int = 1): Int = n match {
    case _ if (n < 0) => throw new Exception
    case n if (n == 1) => fact
    case n => factorial(n - 1, n * fact)
  }
}
