package models

import java.text.SimpleDateFormat

import actors.SessionsScheduler.{Notification, Reminder}
import controllers.{FilterUserSessionInformation, UpdateSessionInformation}
import models.SessionJsonFormats.{ExpiringNext, ExpiringNextNotReminded, SchedulingNext, SchedulingNextUnNotified}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.json.{JsBoolean, JsObject}
import play.api.test.PlaySpecification
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONDateTime, BSONObjectID}
import utilities.DateTimeUtility

import scala.concurrent.ExecutionContext.Implicits.global

class SessionsRepositorySpec extends PlaySpecification with Mockito {

  private val sessionId = BSONObjectID.generate
  private val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  private val currentDateString = "2017-07-12T14:30:00"
  private val startDateString = "2017-07-10T14:30:00"
  private val startDate = formatter.parse(startDateString).getTime
  private val endDateString = "2017-07-13T23:59:00"
  private val endDate = formatter.parse(endDateString).getTime
  private val currentDate = formatter.parse(currentDateString)
  private val currentMillis = currentDate.getTime
  private val endOfDayDateString = "2017-07-12T23:59:59"
  private val endOfDayDate = formatter.parse(endOfDayDateString)
  private val endOfDayMillis = endOfDayDate.getTime
  private val yearMonthFormatDB = new SimpleDateFormat("yyyy-MM")
  private val yearMonthFormat = new SimpleDateFormat("yyyy-MMMM")
  private val utilDate = yearMonthFormatDB.parse(currentDateString)
  private val yearMonth = yearMonthFormat.format(utilDate)

  val sessionInfo = SessionInfo("testId1", "test@example.com", BSONDateTime(currentMillis), "session1", "category",
    "subCategory", "feedbackFormId", "topic1", 1, meetup = true, "", 0.00, cancelled = false, active = true,
    BSONDateTime(currentMillis + 24 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
    temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

  trait TestScope extends Scope {
    val dateTimeUtility: DateTimeUtility = mock[DateTimeUtility]

    val sessionsRepository = new SessionsRepository(TestDb.reactiveMongoApi, dateTimeUtility)
  }

  "Session repository" should {

    "insert session" in new TestScope {
      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))

      created must beEqualTo(true)
    }

    "get sessions" in new TestScope {
      val sessions: List[SessionInfo] = await(sessionsRepository.sessions)

      sessions must beEqualTo(List(sessionInfo))
    }

    "get session by id" in new TestScope {
      val maybeSession: Option[SessionInfo] = await(sessionsRepository.getById(sessionId.stringify))

      maybeSession contains sessionInfo
    }

    "get sessions scheduled next" in new TestScope {
      dateTimeUtility.nowMillis returns currentMillis
      dateTimeUtility.endOfDayMillis returns endOfDayMillis

      val sessions: List[SessionInfo] = await(sessionsRepository.sessionsForToday(SchedulingNext))

      sessions must beEqualTo(List(sessionInfo))
    }

    "get sessions expiring next not reminded" in new TestScope {
      dateTimeUtility.nowMillis returns currentMillis
      dateTimeUtility.endOfDayMillis returns endOfDayMillis

      val sessions: List[SessionInfo] = await(sessionsRepository.sessionsForToday(ExpiringNextNotReminded))

      sessions must beEqualTo(Nil)
    }

    "get sessions scheduled next unnotified" in new TestScope {
      dateTimeUtility.nowMillis returns currentMillis
      dateTimeUtility.endOfDayMillis returns endOfDayMillis

      val sessions: List[SessionInfo] = await(sessionsRepository.sessionsForToday(SchedulingNextUnNotified))

      sessions must beEqualTo(List(sessionInfo))
    }

    "get sessions expiring next" in new TestScope {
      dateTimeUtility.startOfDayMillis returns currentMillis
      dateTimeUtility.endOfDayMillis returns endOfDayMillis

      val sessions: List[SessionInfo] = await(sessionsRepository.sessionsForToday(ExpiringNext))

      sessions must beEqualTo(Nil)
    }


    "getByEmail session" in new TestScope {
      val updatedSession = UpdateSessionInfo(UpdateSessionInformation(sessionId.stringify, currentDate,
        "updatedSession", "category", "subCategory", "feedbackFormId", "updatedTopic", 1, Some("youtubeURL"),
        Some("slideShareURL"), cancelled = false), BSONDateTime(currentMillis + 24 * 60 * 60 * 1000))

      val updated: Boolean = await(sessionsRepository.update(updatedSession).map(_.ok))

      updated must beEqualTo(true)
    }

    "get active session by id" in new TestScope {
      val response = await(sessionsRepository.getActiveById(sessionId.stringify))

      response contains sessionInfo
    }

    "get paginated sessions when searched with empty string" in new TestScope {
      val paginatedSessions: List[SessionInfo] = await(sessionsRepository.paginate(1, filter="all"))

      paginatedSessions must beEqualTo(List(sessionInfo.copy(session = "updatedSession", topic = "updatedTopic", meetup = false)))
    }

    "get paginated sessions when searched with some string" in new TestScope {
      val paginatedSessions: List[SessionInfo] = await(sessionsRepository.paginate(1, Some("test"), filter="all"))

      paginatedSessions must beEqualTo(List(sessionInfo.copy(session = "updatedSession", topic = "updatedTopic", meetup = false)))
    }

    "get active sessions count when searched with empty string" in new TestScope {
      val count: Int = await(sessionsRepository.activeCount(None, "all"))

      count must beEqualTo(1)
    }

    "get active and uncancelled sessions count when serched with empty string" in new TestScope {
      dateTimeUtility.nowMillis returns currentMillis
      val count: Int = await(sessionsRepository.activeUncancelledCount(None))

      count must beEqualTo(0)
    }

    "get users session till now for a particular user" in new TestScope {
      val response = await(sessionsRepository.userSessionsTillNow(Some("test@example.com"), 1))

      response contains sessionInfo
    }

    "get users session till now for all users" in new TestScope {
      val response = await(sessionsRepository.userSessionsTillNow(None, 1))

      response contains sessionInfo
    }

    "get active sessions count when searched with some string" in new TestScope {
      val count: Int = await(sessionsRepository.activeCount(Some("test"), "all"))

      count must beEqualTo(1)
    }

    "get active and cancelled sessions count when serched with some string" in new TestScope {
      dateTimeUtility.nowMillis returns currentMillis
      val count: Int = await(sessionsRepository.activeUncancelledCount(Some("test")))

      count must beEqualTo(0)
    }

    "delete session" in new TestScope {
      val deletedSession: Option[JsObject] = await(sessionsRepository.delete(sessionId.stringify))

      val deletedSessionJsObject: JsObject = deletedSession.getOrElse(JsObject(Seq.empty))

      (deletedSessionJsObject \ "active").getOrElse(JsBoolean(true)) must beEqualTo(JsBoolean(false))
    }

    "get active sessions" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0, cancelled = false, active = true,
        BSONDateTime(currentMillis + 24 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val greaterThanSessionMillis: Long = currentMillis + 2 * 60 * 60 * 1000

      dateTimeUtility.nowMillis returns greaterThanSessionMillis

      val activeSessions: List[SessionInfo] = await(sessionsRepository.activeSessions())

      activeSessions must beEqualTo(List(sessionInfo))
    }

    "get active sessions by email" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 24 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val greaterThanSessionMillis: Long = currentMillis + 2 * 60 * 60 * 1000

      dateTimeUtility.nowMillis returns greaterThanSessionMillis

      val activeSessions: List[SessionInfo] = await(sessionsRepository.activeSessions(Some("test@example.com")))

      activeSessions must contain(List(sessionInfo).head)
    }

    "get immediate previous expired sessions when there is no session" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 24 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val greaterThanSessionExpirationMillis: Long = currentMillis + (23 * 60 * 60 * 1000)

      dateTimeUtility.nowMillis returns greaterThanSessionExpirationMillis

      val expiredSessions: List[SessionInfo] = await(sessionsRepository.immediatePreviousExpiredSessions)

      expiredSessions must beEqualTo(Nil)
    }

    "get immediate previous expired sessions" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val greaterThanSessionExpirationMillis: Long = currentMillis + 24 * 60 * 60 * 1000

      dateTimeUtility.nowMillis returns greaterThanSessionExpirationMillis

      val expiredSessions: List[SessionInfo] = await(sessionsRepository.immediatePreviousExpiredSessions)

      expiredSessions must beEqualTo(List(sessionInfo))
    }

    "update rating for a given session ID when score is greater than or equal to 60" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result: UpdateWriteResult = await(sessionsRepository.updateRating(sessionId.stringify, List(90.00)))

      result.ok must beEqualTo(true)
    }

    "update rating for a given session ID when score is greater than 30 but less than 60" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 38.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result: UpdateWriteResult = await(sessionsRepository.updateRating(sessionId.stringify, List(40.00)))

      result.ok must beEqualTo(true)
    }

    "update rating for a given session ID when score is less than 30" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 28.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result: UpdateWriteResult = await(sessionsRepository.updateRating(sessionId.stringify, List(20.00)))

      result.ok must beEqualTo(true)
    }

    "update rating for a given session ID when user did not attend" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtubeURL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result: UpdateWriteResult = await(sessionsRepository.updateRating(sessionId.stringify, List()))

      result.ok must beEqualTo(true)
    }

    "update sub category on change" in new TestScope {
      val deleteSubCategory = await(sessionsRepository.updateSubCategoryOnChange("subCategory", ""))

      deleteSubCategory.ok must beEqualTo(true)
    }

    "update category on delete" in new TestScope {
      val deleteCategory = await(sessionsRepository.updateCategoryOnChange("category", ""))

      deleteCategory.ok must beEqualTo(true)
    }

    "get sessions by category" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category", "subCategory",
        "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true, BSONDateTime(currentMillis + 23 * 60 * 60 * 1000),
        Some("youtubeURL"), Some("slideShareURL"), temporaryYoutubeURL = Some("temporaryYoutubeURL"), reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val session = await(sessionsRepository.getSessionByCategory("category", "subCategory"))
      session must beEqualTo(List(sessionInfo))
    }

    "return session in Time Range when email is specified" in new TestScope {

      val result: List[SessionInfo] = await(sessionsRepository.sessionsInTimeRange(FilterUserSessionInformation(
        Some("test@example.com"), startDate, endDate)))

      result.size must beEqualTo(8)
    }

    "return session in Time Range when email is not specified" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate

      val result: List[SessionInfo] = await(sessionsRepository.sessionsInTimeRange(FilterUserSessionInformation(None, startDate, endDate)))

      result.size must beEqualTo(8)
    }

    "return session monthly Info" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate

      val result: List[(String, Int)] = await(sessionsRepository.getMonthlyInfoSessions(FilterUserSessionInformation(None, startDate, endDate)))

      result.head._1 must beEqualTo("2017-07")
    }

    "get video URL for a session" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category", "subCategory", "feedbackFormId", "topic2",
        1, meetup = true, "", 0.00, cancelled = false, active = true, BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtube/embed/URL"), Some("slideShareURL"), temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result: List[String] = await(sessionsRepository.getVideoURL(sessionId.stringify))

      result.head must beEqualTo("youtube/embed/URL")
    }

    "update video URL for a session" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category", "subCategory", "feedbackFormId", "topic2",
        1, meetup = true, "", 0.00, cancelled = false, active = true, BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtube/embed/URL"), Some("slideShareURL"), temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result = await(sessionsRepository.updateVideoURL(sessionId.stringify, "youtube/embed/URL"))

      result.ok must beEqualTo(true)
    }

    "store temporary video URL for a session" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtube/embed/URL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val result = await(sessionsRepository.storeTemporaryVideoURL(sessionId.stringify, "youtube/embed/URL"))

      result.ok must beEqualTo(true)
    }

    "get temporary video URL for a session" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate
      val sessionInfo = SessionInfo("testId2", "test@example.com", BSONDateTime(currentMillis), "session2", "category",
        "subCategory", "feedbackFormId", "topic2", 1, meetup = true, "", 0.00, cancelled = false, active = true,
        BSONDateTime(currentMillis + 23 * 60 * 60 * 1000), Some("youtube/embed/URL"), Some("slideShareURL"),
        temporaryYoutubeURL = None, reminder = false, notification = false, sessionId)

      val created: Boolean = await(sessionsRepository.insert(sessionInfo).map(_.ok))
      created must beEqualTo(true)

      val insertTemporaryUrl = await(sessionsRepository.storeTemporaryVideoURL(sessionId.stringify, "youtube/embed/URL"))
      insertTemporaryUrl.ok must beEqualTo(true)

      val result: List[String] = await(sessionsRepository.getTemporaryVideoURL(sessionId.stringify))

      result.head must beEqualTo("youtube/embed/URL")
    }

    "fetch particular user's sessions List" in new TestScope {
      val sessionId: BSONObjectID = BSONObjectID.generate

      val result: List[SessionInfo] = await(sessionsRepository.userSession("test@example.com"))

      result must beEqualTo(Nil)
    }

    "upsert record for email session notification reminder" in new TestScope {

      val result = await(sessionsRepository.upsertRecord(sessionInfo, Notification))

      result.ok must beEqualTo(true)
    }

    "upsert record for email feedback reminder" in new TestScope {

      val result = await(sessionsRepository.upsertRecord(sessionInfo, Reminder))

      result.ok must beEqualTo(true)
    }

  }

}
