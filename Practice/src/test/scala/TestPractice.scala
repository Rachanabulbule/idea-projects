import java.util.regex.Pattern

import org.scalatest.WordSpec

class TestPractice extends WordSpec {

  "UpperCase" should {
    "make all letters of string" in {
      val exercise1 = new Exercise1
      assert(exercise1.toUppercase("Rachana Bulbule 7767092611") == "RACHANA BULBULE 7767092611")
    }
  }
  "Exercise5" should{
    "perform addition" in{
      val excercise5=new Exercise5
      assert(excercise5.outerFun5(15,5)==15)
    }
  }

  "Factor" should{
    "list the factors" in{
      val factor= new Factor
      assert(factor.listFactor(12)==List(2,3,4,6,12))
    }
  }
  "PrimeFactor" should{
    "list prime factors" in{
      val primeFactor =new PrimeFactor
      assert(primeFactor.listPrimeFactor(12)==List(2,2,3))
    }
  }


  "Brackets" should{
    "perform match" in{
      val brackets=new Brackets
     assert(brackets.passString("{[}]R")==true)
    }
  }

  "Exercise8" should{
    "perform list operations" in{
      val exercise8=new Exercise8
      assert(exercise8.listOperation(List(1,2,List(3,1,5)),List(3,4,5))==false)
    }
  }

  "Pettern" should{
    "match pattern with expression" in{
      val pattern=new Pattern
      assert(pattern.matchPatternWithExpression("Re","[RS][aei]")==true)
    }
  }

  "RegExpression" should{
    "find expression and replace" in{
      val regExpression=new RegExpression
      assert(regExpression.findAndReplace("1Rachana2Bulbule","[0-9]")==" Rachana Bulbule")
    }
  }
}



