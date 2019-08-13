import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object forComprension extends App{
  val f1= Future (1 / 0) recover { case e: ArithmeticException => 0 } // result: 0
  val f2 = Future(2)
  //val f1 = Future(1 / 0) recoverWith { case e: ArithmeticException => f2} // result: Int.MaxValue
  val f3 = f1.map(_+1)

   val result=for{
     r1 <-f1
     r2 <- f2
     r3 <- f3
   }yield (r1+r2+r3)

  //result.onFailure{case x=>x.printStackTrace}
  //result.onSuccess{case x=>println(x)}

   result onComplete{
   case Success(x)=>println(x)
   case Failure(e)=>e.printStackTrace
  }
}