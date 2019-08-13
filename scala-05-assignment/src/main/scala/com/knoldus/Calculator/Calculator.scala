package com.knoldus.Calculator

case class Calculator(num1: Double, num2: Double) {
  def addition = num1 + num2

  def subtraction = num1 - num2

  def multiplication = num1 * num2

  def division = {
    require(num2 != 0, "num2 can not be 0")

    num1 / num2

  }
}
