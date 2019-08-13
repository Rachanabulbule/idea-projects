//Pack consecutive duplicates
import scala.annotation.tailrec

//UPPERCASE
class Exercise1 {
  def toUppercase(s: String): String = {
    s.toUpperCase
  }
}

//SUM OF ALL UNIQUE MULTIPLES OF GIVEN NUMBER
class Exercise5 {
  def outerFun5(limit: Int, i: Int): Int = {
    def innerFun5(n: Int, i: Int, sum: Int): Int = n match {
      case _ if n == limit => sum
      case n => if (n % i != 0) innerFun5(n + 1, i, sum)
      else innerFun5(n + 1, i, sum + n)
    }

    innerFun5(i, i, sum = 0)
  }
}

//FACTORS OF A GIVEN NUMBER
class Factor {
  def listFactor(n: Int): List[Int] = {
    def findFactor(n: Int, a: Int = 2, l: List[Int] = List()): List[Int] = a match {
      case _ if (a == n) => l :+ a
      case a => if (n % a == 0) findFactor(n, a + 1, l :+ a)
      else findFactor(n, a + 1, l)
    }

    findFactor(n)
  }
}

//PRIME FACTRORS OF A GIVEN NUMBER
class PrimeFactor {
  def listPrimeFactor(num: Int, list: List[Int] = List()): List[Int] = {
    for (n <- 2 to num if (num % n == 0)) {
      return listPrimeFactor(num / n, list :+ n)
    }
    list
  }
}

//COMPLETE EXPRESSION OF PARENTHESIS
class Brackets {
  def passString(s: String): Boolean = {
    def matchBrackets(m: Int, sum: Int = 0, count: Int = 0): Boolean = (s.charAt(m), sum, count) match {
      case (_, sum, _) if (((s.charAt(m) == '{' || s.charAt(m) == '[' || s.charAt(m) == '(')) && (m > 0)) =>
        matchBrackets(m - 1, sum + 1, count)
      case (_, _, count) if (((s.charAt(m) == '}' || s.contains(m) == ']' || s.charAt(m) == ')')) && (m > 0)) =>
        matchBrackets(m - 1, sum, count + 1)
      case (x, _, _) if (m > 0) => matchBrackets(m - 1, sum, count)
      case (_, sum, count) => if ((count == sum) && (m == 0)) true else false
      //case _ if(count!=sum)=> false

    }

    matchBrackets(s.length - 1)
  }
}


class Exercise8 {
  def listOperation(l1: List[Any], l2: List[Any]): Boolean = (l1, l2) match {
    case _ if (l1 == l2) => true
    case (_, l2) if (l1.contains(l2)) => true
    case (l1, _) if (l2.contains(l1)) => true
    case _ => false
  }

}


class Pattern {
  def matchPatternWithExpression(str: String, expression: String): Boolean = {
    if(str.matches(expression)) true else false
  }
}

class RegExpression{
  def findAndReplace(str:String,expression: String):String={
    str.replaceAll(expression," ")
  }
}

