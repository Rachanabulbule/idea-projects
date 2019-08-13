import java.io.File

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object FileSystem extends App {
  val result = Future {getFilesList("/home/knoldus/ScalaTest")}
  def getFilesList(path: String): List[File] = {
    def getFiles(toVisit: List[File], result: List[File]): List[File] =
      toVisit match {
        case Nil => result
        case h :: tail if h.isDirectory => getFiles(h.listFiles.toList ::: tail, result)
        case h :: tail if !h.isDirectory => getFiles(tail, h :: result)
      }
    getFiles(List(new File(path)), List())
  }
  result onComplete{
    case Success(value) => println("Success "+ value)
    case Failure(exception) => exception.getMessage
  }
}