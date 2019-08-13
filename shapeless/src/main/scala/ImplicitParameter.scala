abstract class Implicit[A]{
  def add(x:A,y:A): A
  def unit: A
}
object ImplicitParameter extends App{
  implicit val addString:Implicit[String]= new Implicit[String] {
    def add(x: String, y: String): String = x concat y
    def unit:String=""
  }

  implicit val addInt:Implicit[Int]=new Implicit[Int] {
    def add(x: Int, y: Int):Int = x + y
    def unit: Int = 0
  }

  def sum[A] (l:List[A])(implicit x:Implicit[A]):A={
    if(l.isEmpty) x.unit
    else x.add(l.head, sum(l.tail))
  }
  println(sum(List(1,2,3)))
  println(sum(List("a","b","c")))
}