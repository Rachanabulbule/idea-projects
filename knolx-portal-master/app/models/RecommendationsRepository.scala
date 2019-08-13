package models

import javax.inject.Inject

import models.RecommendationsJsonFormats._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.{QueryOpts, ReadPreference}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection
import utilities.DateTimeUtility

import scala.annotation.switch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

// this is not an unused import contrary to what intellij suggests, do not optimize
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.play.json.BSONFormats.BSONDateTimeFormat

case class RecommendationInfo(email: Option[String],
                              name: String,
                              topic: String,
                              description: String,
                              submissionDate: BSONDateTime,
                              updateDate: BSONDateTime,
                              approved: Boolean = false,
                              declined: Boolean = false,
                              pending: Boolean = true,
                              done: Boolean = false,
                              book: Boolean = false,
                              upVotes: Int = 0,
                              downVotes: Int = 0,
                              _id: BSONObjectID = BSONObjectID.generate())

object RecommendationsJsonFormats {

  import play.api.libs.json.Json

  implicit val recommendationsFormat = Json.format[RecommendationInfo]
}

class RecommendationsRepository @Inject()(reactiveMongoApi: ReactiveMongoApi, dateTimeUtility: DateTimeUtility) {

  import play.modules.reactivemongo.json._

  protected def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("recommendations"))

  def insert(recommendationInfo: RecommendationInfo)(implicit ex: ExecutionContext): Future[WriteResult] =
    collection
      .flatMap(jsonCollection =>
        jsonCollection
          .insert(recommendationInfo))

  def approveRecommendation(id: String)(implicit ex: ExecutionContext): Future[WriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))
    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "approved" -> true,
        "declined" -> false,
        "pending"  -> true,
        "updateDate" -> BSONDateTime(dateTimeUtility.nowMillis)
      ))

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def getRecommendationById(id: String)(implicit ex: ExecutionContext): Future[Option[RecommendationInfo]] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))

    collection
      .flatMap(jsonCollection =>
        jsonCollection
          .find(selector)
          .cursor[RecommendationInfo](ReadPreference.Primary)
          .headOption)
  }

  def declineRecommendation(id: String)(implicit ex: ExecutionContext): Future[WriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))
    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "approved" -> false,
        "declined" -> true,
        "pending" -> false,
        "book" -> false,
        "done" -> false,
        "updateDate" -> BSONDateTime(dateTimeUtility.nowMillis)
      ))

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def paginate(pageNumber: Int,
               filter: String = "all",
               viewBy: String = "latest",
               pageSize: Int = 8)(implicit ex: ExecutionContext): Future[List[RecommendationInfo]] = {
    val total = pageNumber * pageSize
    val queryOptions = new QueryOpts(skipN = 0, batchSizeN = total, flagsN = 0)

    val condition = filter match {
      case "all"      => Json.obj()
      case "approved" => Json.obj("approved" -> true)
      case "decline"  => Json.obj("declined" -> true)
      case "pending"  => Json.obj("pending" -> true)
      case "book"     => Json.obj("book" -> true)
      case "done"     => Json.obj("done" -> true)
      case _          => Json.obj()
    }
    val sortBy = viewBy match {
      case "latest" => Json.obj("submissionDate" -> -1)
      case "recent" => Json.obj("updateDate" -> -1)
    }

    collection
      .flatMap(jsonCollection =>
        jsonCollection
          .find(condition)
          .sort(sortBy)
          .options(queryOptions)
          .cursor[RecommendationInfo](ReadPreference.Primary)
          .collect[List](total, FailOnError[List[RecommendationInfo]]()))
  }

  def upVote(id: String, alreadyVoted: Boolean)(implicit ex: ExecutionContext): Future[UpdateWriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))

    val modifier =
      (alreadyVoted: @switch) match {
        case true  => BSONDocument("$inc" -> BSONDocument("upVotes" -> 1, "downVotes" -> -1))
        case false => BSONDocument("$inc" -> BSONDocument("upVotes" -> 1))
      }

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def downVote(id: String, alreadyVoted: Boolean)(implicit ex: ExecutionContext): Future[UpdateWriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))

    val modifier =
      (alreadyVoted: @switch) match {
        case true  => BSONDocument("$inc" -> BSONDocument("upVotes" -> -1, "downVotes" -> 1))
        case false => BSONDocument("$inc" -> BSONDocument("downVotes" -> 1))
      }

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def pendingRecommendation(id: String)(implicit ex: ExecutionContext): Future[UpdateWriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "pending" -> true,
        "book" -> false,
        "done" -> false,
        "updateDate" -> BSONDateTime(dateTimeUtility.nowMillis)))

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def doneRecommendation(id: String)(implicit ex: ExecutionContext): Future[UpdateWriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id))

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "pending" -> false,
        "done" -> true,
        "updateDate" -> BSONDateTime(dateTimeUtility.nowMillis)))

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def bookRecommendation(id: String)(implicit ex: ExecutionContext): Future[UpdateWriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id), "approved" -> true)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "pending" -> false,
        "book" -> true,
        "updateDate" -> BSONDateTime(dateTimeUtility.nowMillis)))

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def cancelBookedRecommendation(id: String)(implicit ex: ExecutionContext): Future[UpdateWriteResult] = {
    val selector = BSONDocument("_id" -> BSONDocument("$oid" -> id), "approved" -> true)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "pending" -> true,
        "book" -> false,
        "done" -> false,
        "updateDate" -> BSONDateTime(dateTimeUtility.nowMillis)))

    collection
      .flatMap(jsonCollection =>
        jsonCollection.update(selector, modifier))
  }

  def allPendingRecommendations(implicit ex: ExecutionContext): Future[Int] = {
    val selector = Some(Json.obj("approved" -> false, "declined" -> false))

    collection
      .flatMap(_.count(selector))
  }

}
