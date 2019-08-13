import com.knoldus.Calculator.Calculator

object Main extends App{
  val calculator = Calculator(12, 2)
  println(calculator.addition)
  println(calculator.subtraction)
  println(calculator.multiplication)
  println(calculator.division)

}
