class FirstName(val name: String) extends AnyVal {
  def printName(name: String) = name
}

class LastName(val lastName: String) extends AnyVal {
  def printLastName(lastName: String) = lastName
}

class Age(val age: Int) extends AnyVal {
  def printAge(age: Int) = age
}

object ValueClass extends App {

  val firstname = new FirstName("Rachana")
  val lastname = new LastName("Bulbule")
  val age = new Age(22)


  def displayDetails(FirstName: String, LastName: String, Age: Int) = {

    s"${firstname.name}" + " " +
      s"${lastname.lastName}" + " " + "is of" + " " +
      age.age + " " + "years"

  }

  println(ValueClass.displayDetails(firstname.name, lastname.lastName, age.age))

}



