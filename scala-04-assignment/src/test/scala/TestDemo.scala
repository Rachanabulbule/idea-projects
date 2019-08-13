import higherOrderFun.{doublequeue, list, squareQueue}
import org.scalatest.WordSpec

class TestDemo extends WordSpec {
  val personOne = new Person("Test123", 22)
  val personTwo = new Person("Test123", 25)
  "person" should {
    "compare age and name" in {
      assert((personOne.compare(personTwo) == 1))
    }
  }

  val doublequeue = new DoubleQueue(2)
  val squareQueue = new SquareQueue(4)


  "Queue" should {
    "enqueue elements" in {

      assert(doublequeue.enqueue(List()) ::: squareQueue.enqueue(List()) == List(4, 16))
    }
      "and dequeue elements" in {
        assert(doublequeue.dequeue(List(1,2,3,16)) == List(2,3,16))

    }
  }

}