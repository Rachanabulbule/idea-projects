class Person(val name: String, val age: Int) extends Ordered[Person] {
  def compare(that: Person): Int = {

    if( this.name == that.name)

      if(this.age < that.age) 1 else 0

    else
      if(this.name.length < that.name.length) 1 else 0
  }
}

object Person extends App {

  {
    val personOne = new Person("Test123", 25)
    val personTwo = new Person("Test123", 25)
    println(personOne.compare(personTwo))
  }
}






