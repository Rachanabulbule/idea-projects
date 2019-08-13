package com.knoldus.Calculator

  import org.scalatest.WordSpec


class TestCalculator extends WordSpec {
  val calculator = Calculator(12, 2)


  "Calculator" should {
    "perform addition of numbers" in {
      assert((calculator.addition) == 14)
    }

    "Perform subtraction of numbers" in {
      assert((calculator.subtraction) == 10)
    }


    "Perform multiplication of numbers" in {
      assert((calculator.multiplication) == 24)
    }

    "Perform division of numbers" in {
      assert((calculator.division) == 6)
    }

    "not accept num2 equal to 0" in {
      assert((calculator.num2) != 0)
    }
  }

}
