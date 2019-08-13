
trait Queue {

  def enqueue(list: List[Int]): List[Int]

  def dequeue(list: List[Int]): List[Int] = {
    list.tail
  }

}

class DoubleQueue(num: Int) extends Queue {
  def enqueue(list: List[Int]): List[Int] = num * 2 :: List()


}

class SquareQueue(num: Int) extends Queue {

  def enqueue(list: List[Int]): List[Int] = num * num :: List()

}

object higherOrderFun extends App {
  val doublequeue = new DoubleQueue(2)
  val squareQueue = new SquareQueue(4)

  val list = doublequeue.enqueue(List()) ::: squareQueue.enqueue(List())
  println(list)
  val list3 = doublequeue.dequeue(list)
  println(list3)

}