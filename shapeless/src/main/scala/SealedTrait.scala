sealed trait Shape

final case class Rectangle(width: Double, height: Double) extends Shape

final case class Circle(radius: Double) extends Shape

object Main extends App{
  val rect: Shape = Rectangle(3.0, 4.0)
  val circ: Shape = Circle(1.0)
  println(area(rect))
  def area(shape: Shape): Double =
    shape match {
      case Rectangle(w, h) => w * h
      case Circle(r) => math.Pi * r * r
    }
}