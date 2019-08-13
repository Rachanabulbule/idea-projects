import shapeless._
import syntax.zipper._

object Shapeless extends App {
  case class Red()
  case class Green()
  case class Blue()
  type Colors = Red :+: Green :+: Blue :+: CNil
  val blue = Coproduct[Colors](Blue())
  val green = Coproduct[Colors](Green())
  printColor1(green)
  printColor2(blue)

  def printColor1(c: Colors) : Unit = {
    //(c.select[Red], c.select[Green], c.select[Blue]) match {
      (c.select[Red], c.select[Green], c.select[Blue]) match {
      case (_, None, None) => println("Color is red")
      case (None, _, None) => println("Color is green")
      case (None, None, _) => println("Color is blue")
      case _ => println("unknown Color")
    }
  }
  def printColor2(c: Colors) : Unit = {
    c match {
      case Inl(Red()) => println("color is red")
      case Inr(Inl(Green())) => println("color is green")
      case Inr(Inr(Inl(Blue()))) => println("color is blue")
      case _ => println("unknown color")
    }
  }
}
