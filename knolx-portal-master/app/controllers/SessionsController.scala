package controllers

import java.text.SimpleDateFormat
import java.time._
import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import actors.SessionsScheduler._
import actors.{EmailActor, YouTubeDetailsActor}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.api.services.youtube.model.VideoCategory
import controllers.EmailHelper._
import models._
import play.api.{Configuration, Logger}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, Result}
import reactivemongo.bson.BSONDateTime
import utilities.DateTimeUtility

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

// this is not an unused import contrary to what intellij suggests, do not optimize
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.play.json.BSONFormats.BSONDateTimeFormat

case class CreateSessionInformation(email: String,
                                    date: Date,
                                    session: String,
                                    category: String,
                                    subCategory: String,
                                    feedbackFormId: String,
                                    topic: String,
                                    feedbackExpirationDays: Int,
                                    meetup: Boolean)

case class UpdateSessionInformation(id: String,
                                    date: Date,
                                    session: String,
                                    category: String,
                                    subCategory: String,
                                    feedbackFormId: String,
                                    topic: String,
                                    feedbackExpirationDays: Int,
                                    youtubeURL: Option[String],
                                    slideShareURL: Option[String],
                                    cancelled: Boolean,
                                    meetup: Boolean = false)

case class KnolxSession(id: String,
                        userId: String,
                        date: Date,
                        session: String,
                        topic: String,
                        email: String,
                        meetup: Boolean,
                        cancelled: Boolean,
                        rating: String,
                        feedbackFormScheduled: Boolean = false,
                        dateString: String = "",
                        completed: Boolean = false,
                        expired: Boolean = false,
                        contentAvailable: Boolean)

case class SessionEmailInformation(email: Option[String], page: Int, filter: String, pageSize: Int)

case class SessionSearchResult(sessions: List[KnolxSession],
                               pages: Int,
                               page: Int,
                               keyword: String,
                               totalSessions: Int)

case class VideoCategories(id: String, name: String)

object SessionValues {
  val Sessions: IndexedSeq[(String, String)] = 1 to 5 map (number => (s"session $number", s"Session $number"))
}

@Singleton
class SessionsController @Inject()(messagesApi: MessagesApi,
                                   usersRepository: UsersRepository,
                                   sessionsRepository: SessionsRepository,
                                   feedbackFormsRepository: FeedbackFormsRepository,
                                   sessionRequestRepository: SessionRequestRepository,
                                   recommendationsRepository: RecommendationsRepository,
                                   dateTimeUtility: DateTimeUtility,
                                   configuration: Configuration,
                                   controllerComponents: KnolxControllerComponents,
                                   @Named("EmailManager") emailManager: ActorRef,
                                   @Named("SessionsScheduler") sessionsScheduler: ActorRef,
                                   @Named("UsersBanScheduler") usersBanScheduler: ActorRef,
                                   @Named("YouTubeManager") youtubeManager: ActorRef
                                  ) extends KnolxAbstractController(controllerComponents) with I18nSupport {

  implicit val timeout = Timeout(10.seconds)

  implicit val knolxSessionInfoFormat: OFormat[KnolxSession] = Json.format[KnolxSession]
  implicit val sessionSearchResultInfoFormat: OFormat[SessionSearchResult] = Json.format[SessionSearchResult]
  lazy val fromEmail: String = configuration.getOptional[String]("play.mailer.user").getOrElse("support@knoldus.com")

  val sessionSearchForm = Form(
    mapping(
      "email" -> optional(nonEmptyText),
      "page" -> number.verifying("Invalid Page Number", number => number >= 0),
      "filter" -> nonEmptyText.verifying("Invalid filter",
        filter => filter == "all" || filter == "completed" || filter == "upcoming"),
      "pageSize" -> number.verifying("Invalid Page size", number => number >= 10)
    )(SessionEmailInformation.apply)(SessionEmailInformation.unapply)
  )

  val createSessionForm = Form(
    mapping(
      "email" -> email.verifying("Invalid Email", email => isValidEmail(email)),
      "date" -> date("yyyy-MM-dd'T'HH:mm", dateTimeUtility.ISTTimeZone)
        .verifying("Invalid date selected!", date => date.after(new Date(dateTimeUtility.startOfDayMillis))),
      "session" -> text.verifying("Wrong session type specified!",
        session => !session.isEmpty && SessionValues.Sessions.map { case (value, _) => value }.contains(session)),
      "category" -> text.verifying("Please attach a category", !_.isEmpty),
      "subCategory" -> text.verifying("Please attach a sub-category", !_.isEmpty),
      "feedbackFormId" -> text.verifying("Please attach a feedback form template", !_.isEmpty),
      "topic" -> nonEmptyText,
      "feedbackExpirationDays" -> number.verifying("Invalid feedback form expiration days selected", number => number >= 0 && number <= 31),
      "meetup" -> boolean
    )(CreateSessionInformation.apply)(CreateSessionInformation.unapply)
  )
  val updateSessionForm = Form(
    mapping(
      "sessionId" -> nonEmptyText,
      "date" -> date("yyyy-MM-dd'T'HH:mm", dateTimeUtility.ISTTimeZone),
      "session" -> nonEmptyText.verifying("Wrong session type specified!",
        session => SessionValues.Sessions.map { case (value, _) => value }.contains(session)),
      "category" -> text.verifying("Please attach a category", !_.isEmpty),
      "subCategory" -> text.verifying("Please attach a sub-category", !_.isEmpty),
      "feedbackFormId" -> text.verifying("Please attach a feedback form template", !_.isEmpty),
      "topic" -> nonEmptyText,
      "feedbackExpirationDays" -> number.verifying("Invalid feedback form expiration days selected, " +
        "must be in range 1 to 31", number => number >= 0 && number <= 31),
      "youtubeURL" -> optional(nonEmptyText),
      "slideShareURL" -> optional(nonEmptyText),
      "cancelled" -> boolean,
      "meetup" -> boolean
    )(UpdateSessionInformation.apply)(UpdateSessionInformation.unapply)
  )


  def sessions: Action[AnyContent] = action.async { implicit request =>
    Future.successful(Ok(views.html.sessions.sessions()))
  }

  def searchSessions: Action[AnyContent] = action.async { implicit request =>
    sessionSearchForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.error(s"Received a bad request for user manage ==> $formWithErrors")
        Future.successful(BadRequest(" OOps! Invalid value encountered !"))
      },
      sessionInformation => {
        sessionsRepository
          .paginate(sessionInformation.page, sessionInformation.email, sessionInformation.filter, sessionInformation.pageSize)
          .flatMap { sessionInfo =>
            val knolxSessions = sessionInfo map { session =>
              val contentAvailable = session.youtubeURL.isDefined || session.slideShareURL.isDefined
              KnolxSession(session._id.stringify,
                session.userId,
                new Date(session.date.value),
                session.session,
                session.topic,
                session.email,
                session.meetup,
                session.cancelled,
                "",
                dateString = new Date(session.date.value).toString,
                completed = new Date(session.date.value).before(new java.util.Date(dateTimeUtility.nowMillis)),
                expired = new Date(session.expirationDate.value)
                  .before(new java.util.Date(dateTimeUtility.nowMillis)),
                contentAvailable = contentAvailable)
            }
            sessionsRepository
              .activeCount(sessionInformation.email, sessionInformation.filter)
              .map { count =>
                val pages = Math.ceil(count.toDouble / sessionInformation.pageSize).toInt
                Ok(Json.toJson(SessionSearchResult(knolxSessions, pages, sessionInformation.page, sessionInformation.email.getOrElse(""), count)).toString)
              }
          }
      })
  }

  def manageSessions: Action[AnyContent] = adminAction.async { implicit request =>
    Future.successful(Ok(views.html.sessions.managesessions()))
  }

  def searchManageSession: Action[AnyContent] = adminAction.async { implicit request =>
    sessionSearchForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.error(s"Received a bad request for user manage ==> $formWithErrors")
        Future.successful(BadRequest(" OOps! Invalid value encountered !"))
      },
      sessionInformation => {
        sessionsRepository
          .paginate(sessionInformation.page, sessionInformation.email, sessionInformation.filter, sessionInformation.pageSize)
          .flatMap { sessionInfo =>
            val knolxSessions = sessionInfo map { sessionInfo =>
              val contentAvailable = sessionInfo.youtubeURL.isDefined || sessionInfo.slideShareURL.isDefined
              KnolxSession(sessionInfo._id.stringify,
                sessionInfo.userId,
                new Date(sessionInfo.date.value),
                sessionInfo.session,
                sessionInfo.topic,
                sessionInfo.email,
                sessionInfo.meetup,
                sessionInfo.cancelled,
                sessionInfo.rating,
                dateString = new Date(sessionInfo.date.value).toString,
                completed = new Date(sessionInfo.date.value).before(new java.util.Date(dateTimeUtility.nowMillis)),
                expired = new Date(sessionInfo.expirationDate.value)
                  .before(new java.util.Date(dateTimeUtility.nowMillis)),
                contentAvailable = contentAvailable)
            }

            val eventualScheduledFeedbackForms =
              (sessionsScheduler ? GetScheduledSessions) (5.seconds).mapTo[ScheduledSessions]

            val eventualKnolxSessions = eventualScheduledFeedbackForms map { scheduledFeedbackForms =>
              knolxSessions map { session =>
                val scheduled = scheduledFeedbackForms.sessionIds.contains(session.id)
                session.copy(feedbackFormScheduled = scheduled)
              }
            }
            eventualKnolxSessions flatMap { sessions =>
              sessionsRepository
                .activeCount(sessionInformation.email, sessionInformation.filter)
                .map { count =>
                  val pages = Math.ceil(count.toDouble / sessionInformation.pageSize).toInt
                  Ok(Json.toJson(SessionSearchResult(sessions, pages, sessionInformation.page, sessionInformation.email.getOrElse(""), count)).toString)
                }
            }
          }
      })
  }

  def create: Action[AnyContent] = adminAction.async { implicit request =>
    feedbackFormsRepository
      .getAll
      .map { feedbackForms =>
        val formIds = feedbackForms.map(form => (form._id.stringify, form.name))
        Ok(views.html.sessions.createsession(createSessionForm, formIds))
      }
  }

  def createSession: Action[AnyContent] = adminAction.async { implicit request =>
    feedbackFormsRepository
      .getAll
      .flatMap { feedbackForms =>
        val formIds = feedbackForms.map(form => (form._id.stringify, form.name))
        createSessionForm.bindFromRequest.fold(
          formWithErrors => {
            Logger.error(s"Received a bad request for create session $formWithErrors")
            Future.successful(BadRequest(views.html.sessions.createsession(formWithErrors, formIds)))
          },
          createSessionInfo => {
            val presenterEmail = createSessionInfo.email.toLowerCase
            usersRepository
              .getByEmail(presenterEmail)
              .flatMap(_.fold {
                Future.successful(
                  BadRequest(views.html.sessions.createsession(createSessionForm.fill(createSessionInfo).withGlobalError("Email not valid!"), formIds)))
              } { userJson =>
                val expirationDateMillis = sessionExpirationMillis(createSessionInfo.date, createSessionInfo.feedbackExpirationDays)
                val session = models.SessionInfo(userJson._id.stringify, createSessionInfo.email.toLowerCase,
                  BSONDateTime(createSessionInfo.date.getTime), createSessionInfo.session, createSessionInfo.category,
                  createSessionInfo.subCategory, createSessionInfo.feedbackFormId,
                  createSessionInfo.topic, createSessionInfo.feedbackExpirationDays, createSessionInfo.meetup, rating = "",
                  0, cancelled = false, active = true, BSONDateTime(expirationDateMillis), None, None)
                sessionsRepository.insert(session) flatMap { result =>
                  if (result.ok) {
                    Logger.info(s"Session for user ${createSessionInfo.email} successfully created")
                    sessionsScheduler ! RefreshSessionsSchedulers
                    Future.successful(Redirect(routes.SessionsController.manageSessions()).flashing("message" -> "Session successfully created!"))
                  } else {
                    Logger.error(s"Something went wrong when creating a new Knolx session for user ${createSessionInfo.email}")
                    Future.successful(InternalServerError("Something went wrong!"))
                  }
                }
              })
          })
      }
  }

  def sendEmailToPresenter(sessionId: String): Action[AnyContent] = adminAction.async { implicit request =>
    sessionsRepository
      .getById(sessionId)
      .flatMap(_.fold {
        Logger.error(s"Failed to send email to the presenter with id $sessionId")
        Future.successful(InternalServerError("Something went wrong!"))
      } { sessionInfo =>
        val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
        emailManager ! EmailActor.SendEmail(
          List(sessionInfo.email), fromEmail, "Session Scheduled!",
          views.html.emails.presenternotification(sessionInfo.topic,
            formatter.parse(dateTimeUtility.toLocalDateTime(sessionInfo.date.value).toString)).toString)
        Logger.error(s"Email has been successfully sent to the presenter ${sessionInfo.email}")
        Future.successful(Redirect(routes.SessionsController.manageSessions()).flashing("message" -> "Email has been sent to the presenter"))
      }
      )
  }

  private def sessionExpirationMillis(date: Date, customDays: Int): Long =
    if (customDays > 0) {
      customSessionExpirationMillis(date, customDays)
    } else {
      defaultSessionExpirationMillis(date)
    }

  private def defaultSessionExpirationMillis(date: Date): Long = {
    val scheduledDate = dateTimeUtility.toLocalDateTimeEndOfDay(date)

    val expirationDate = scheduledDate.getDayOfWeek match {
      case DayOfWeek.FRIDAY   => scheduledDate.plusDays(4)
      case DayOfWeek.SATURDAY => scheduledDate.plusDays(3)
      case _: DayOfWeek       => scheduledDate.plusDays(1)
    }

    dateTimeUtility.toMillis(expirationDate)
  }

  private def customSessionExpirationMillis(date: Date, days: Int): Long = {
    val scheduledDate = dateTimeUtility.toLocalDateTimeEndOfDay(date)
    val expirationDate = scheduledDate.plusDays(days)

    dateTimeUtility.toMillis(expirationDate)
  }

  def deleteSession(id: String, pageNumber: Int): Action[AnyContent] = adminAction.async { implicit request =>
    sessionsRepository
      .delete(id)
      .flatMap(_.fold {
        Logger.error(s"Failed to delete Knolx session with id $id")
        Future.successful(InternalServerError("Something went wrong!"))
      } { _ =>
        Logger.info(s"Knolx session $id successfully deleted")
        sessionsScheduler ! RefreshSessionsSchedulers
        Future.successful(Redirect(routes.SessionsController.manageSessions()).flashing("message" -> "Session successfully Deleted!"))
      })
  }

  def update(id: String): Action[AnyContent] = adminAction.async { implicit request =>
    sessionsRepository
      .getById(id)
      .flatMap {
        case Some(sessionInformation) =>
          feedbackFormsRepository
            .getAll
            .flatMap { feedbackForms =>
              val formIds = feedbackForms.map(form => (form._id.stringify, form.name))
              val filledForm = updateSessionForm.fill(UpdateSessionInformation(sessionInformation._id.stringify,
                new Date(sessionInformation.date.value), sessionInformation.session, sessionInformation.category, sessionInformation.subCategory,
                sessionInformation.feedbackFormId, sessionInformation.topic, sessionInformation.feedbackExpirationDays,
                sessionInformation.youtubeURL, sessionInformation.slideShareURL, sessionInformation.cancelled, sessionInformation.meetup))
              val eventualYoutubeCategories =
                (youtubeManager ? YouTubeDetailsActor.GetCategories)
                  .mapTo[List[VideoCategory]]
                  .map(_.map(videoCategory => VideoCategories(videoCategory.getId, videoCategory.getSnippet.getTitle)))

              eventualYoutubeCategories.flatMap { youtubeCategories =>
                sessionInformation.youtubeURL.fold {
                  Future.successful(Ok(views.html.sessions.updatesession(filledForm, formIds, youtubeCategories, None)))
                } { videoURL =>
                  val videoId = videoURL.split("/")(2)
                  (youtubeManager ? YouTubeDetailsActor.GetDetails(videoId)).mapTo[Option[UpdateVideoDetails]]
                    .map { videoDetails =>
                      Ok(views.html.sessions.updatesession(filledForm, formIds, youtubeCategories, videoDetails))
                    }
                }
              }
            }
        case None                     =>
          Future.successful(Redirect(routes.SessionsController.manageSessions()).flashing("message" -> "Something went wrong!"))
      }
  }

  def updateSession(): Action[AnyContent] = adminAction.async { implicit request =>
    feedbackFormsRepository
      .getAll
      .flatMap { feedbackForms =>
        val formIds = feedbackForms.map(form => (form._id.stringify, form.name))
        updateSessionForm.bindFromRequest.fold(
          formWithErrors => {
            val eventualYoutubeCategories =
              (youtubeManager ? YouTubeDetailsActor.GetCategories)
                .mapTo[List[VideoCategory]]
                .map(_.map(videoCategory => VideoCategories(videoCategory.getId, videoCategory.getSnippet.getTitle)))

            Logger.error(s"Received a bad request for getByEmail session $formWithErrors")

            eventualYoutubeCategories.flatMap { youtubeCategories =>
              val youtubeURL = formWithErrors.value.fold[Option[String]](None)(_.youtubeURL)

              youtubeURL.fold {
                Future.successful(BadRequest(views.html.sessions.updatesession(formWithErrors, formIds, youtubeCategories, None)))
              } { url =>
                val videoId = url.split("/")(2)
                (youtubeManager ? YouTubeDetailsActor.GetDetails(videoId))
                  .mapTo[Option[UpdateVideoDetails]]
                  .map { videoDetails =>
                    Ok(views.html.sessions.updatesession(formWithErrors, formIds, youtubeCategories, videoDetails))
                  }
              }
            }
          },
          updateSessionInfo => {
            val expirationMillis = sessionExpirationMillis(updateSessionInfo.date, updateSessionInfo.feedbackExpirationDays)
            val updatedSession = UpdateSessionInfo(updateSessionInfo, BSONDateTime(expirationMillis))
            sessionsRepository
              .update(updatedSession)
              .flatMap { result =>
                if (result.ok) {
                  Logger.info(s"Successfully updated session ${updateSessionInfo.id}")
                  sessionsScheduler ! RefreshSessionsSchedulers
                  Logger.error(s"Cannot refresh feedback form actors while updating session ${updateSessionInfo.id}")
                  Future.successful(Redirect(routes.SessionsController.manageSessions()).flashing("message" -> "Session successfully updated"))
                } else {
                  Logger.error(s"Something went wrong when updating a new Knolx session for user  ${updateSessionInfo.id}")
                  Future.successful(InternalServerError("Something went wrong!"))
                }
              }
          })
      }
  }

  def cancelScheduledSession(sessionId: String): Action[AnyContent] = adminAction.async { implicit request =>
    (sessionsScheduler ? CancelScheduledSession(sessionId)) (5.seconds).mapTo[Boolean] map {
      case true  =>
        Redirect(routes.SessionsController.manageSessions())
          .flashing("message" -> "Scheduled feedback form successfully cancelled!")
      case false =>
        Redirect(routes.SessionsController.manageSessions())
          .flashing("message" -> "Either feedback form was already sent or Something went wrong while removing scheduled feedback form!")
    }
  }

  def scheduleSession(sessionId: String): Action[AnyContent] = adminAction.async { implicit request =>
    sessionsScheduler ! ScheduleSession(sessionId)
    Future.successful(Redirect(routes.SessionsController.manageSessions())
      .flashing("message" -> "Feedback form schedule initiated"))
  }

  def shareContent(id: String): Action[AnyContent] = action.async { implicit request =>
    if (id.length != 24) {
      Future.successful(Redirect(routes.SessionsController.sessions()).flashing("message" -> "Session Not Found"))
    } else {
      val eventualMaybeSession: Future[Option[SessionInfo]] = sessionsRepository.getById(id)
      eventualMaybeSession.flatMap(maybeSession =>
        maybeSession.fold(Future.successful(Redirect(routes.SessionsController.sessions()).flashing("message" -> "Session Not Found")))
        (session => Future.successful(Ok(views.html.sessions.sessioncontent(session)))))
    }
  }

  def renderScheduleSessionByAdmin(sessionId: String): Action[AnyContent] = adminAction.async { implicit request =>
    feedbackFormsRepository
      .getAll
      .flatMap { feedbackForms =>
        val formIds = feedbackForms.map(form => (form._id.stringify, form.name))

        sessionRequestRepository.getSession(sessionId).map { maybeSession =>
          maybeSession.fold {
            Redirect(routes.CalendarController.renderCalendarPage()).flashing("message" -> "The selected session does not exist")
          } { session =>
            val createSessionInfo = CreateApproveSessionInfo(session.email,
              new Date(session.date.value),
              session.category,
              session.subCategory,
              session.topic,
              session.meetup,
              dateTimeUtility.formatDateWithT(new Date(session.date.value)))
            Ok(views.html.sessions.approvesession(createSessionForm, formIds, sessionId, session.recommendationId, createSessionInfo))
          }
        }
      }
  }

  def approveSessionByAdmin(sessionApprovedId: String,
                            recommendationId: String): Action[AnyContent] = adminAction.async { implicit request =>
    feedbackFormsRepository
      .getAll
      .flatMap { feedbackForms =>
        val formIds = feedbackForms.map(form => (form._id.stringify, form.name))
        sessionRequestRepository.getSession(sessionApprovedId).flatMap { maybeSession =>
          maybeSession.fold {
            Future.successful(Redirect(routes.CalendarController.renderCalendarPage())
              .flashing("message" -> "The selected session does not exist"))
          } { session =>
            val createApproveSessionInfo = CreateApproveSessionInfo(session.email,
              new Date(session.date.value),
              session.category,
              session.subCategory,
              session.topic,
              session.meetup,
              dateTimeUtility.formatDateWithT(new Date(session.date.value)))
            createSessionForm.bindFromRequest.fold(
              formWithErrors => {
                Logger.error(s"Received a bad request while approving the session $formWithErrors")
                Future.successful(BadRequest(
                  views.html.sessions.approvesession(formWithErrors, formIds, sessionApprovedId, recommendationId, createApproveSessionInfo)))
              },
              createSessionInfo => {
                val presenterEmail = createSessionInfo.email.toLowerCase
                usersRepository
                  .getByEmail(presenterEmail)
                  .flatMap(_.fold {
                    Future.successful(
                      BadRequest(
                        views.html.sessions.approvesession(
                          createSessionForm.fill(createSessionInfo).withGlobalError("Email not valid!"),
                          formIds,
                          sessionApprovedId,
                          recommendationId,
                          createApproveSessionInfo)
                      )
                    )
                  } { userJson =>
                    approveSession(createSessionInfo, sessionApprovedId, recommendationId, createApproveSessionInfo, formIds, request, userJson)
                  })
              })
          }
        }
      }
  }

  private def approveSession(createSessionInfo: CreateSessionInformation,
                             sessionApprovedId: String,
                             recommendationId: String,
                             createApproveSessionInfo: CreateApproveSessionInfo,
                             formIds: List[(String, String)],
                             request: SecuredRequest[AnyContent],
                             userJson: UserInfo): Future[Result] = {
    val expirationDateMillis = sessionExpirationMillis(createSessionInfo.date, createSessionInfo.feedbackExpirationDays)
    val session = models.SessionInfo(userJson._id.stringify, createSessionInfo.email.toLowerCase,
      BSONDateTime(createSessionInfo.date.getTime), createSessionInfo.session, createSessionInfo.category,
      createSessionInfo.subCategory, createSessionInfo.feedbackFormId,
      createSessionInfo.topic, createSessionInfo.feedbackExpirationDays, createSessionInfo.meetup, rating = "",
      0, cancelled = false, active = true, BSONDateTime(expirationDateMillis), None, None)

    sessionsRepository.insert(session) flatMap { result =>
      if (result.ok) {
        Logger.info(s"Session for user ${createSessionInfo.email} successfully approved")
        sessionsScheduler ! RefreshSessionsSchedulers
        val approveSessionInfo = UpdateApproveSessionInfo(BSONDateTime(createSessionInfo.date.getTime), sessionApprovedId,
          createSessionInfo.topic, createSessionInfo.email, createSessionInfo.category, createSessionInfo.subCategory,
          createSessionInfo.meetup, approved = true, recommendationId = recommendationId)

        sessionRequestRepository.insertSessionForApprove(approveSessionInfo).flatMap { updatedResult =>
          if (updatedResult.ok) {
            if (approveSessionInfo.recommendationId.isEmpty) {
              Future.successful(Redirect(routes.CalendarController.renderCalendarPage()).flashing("message" -> "Session successfully approved!"))
            } else {
              recommendationsRepository.doneRecommendation(approveSessionInfo.recommendationId).map { status =>
                if (status.ok) {
                  Logger.info("Session with respective recommendation has been scheduled ")
                  Redirect(routes.CalendarController.renderCalendarPage()).flashing("message" -> "Session successfully approved!")
                } else {
                  Logger.error("Something went wrong while scheduling a session with respective recommendation")
                  Redirect(routes.CalendarController.renderCalendarPage()).flashing("error" -> "Something went wrong!")
                }
              }
            }
          } else {
            Future.successful(InternalServerError("Something went wrong!"))
          }
        }
      } else {
        Logger.error(s"Something went wrong while approving a requested Knolx session for user ${createSessionInfo.email}")
        Future.successful(InternalServerError("Something went wrong!"))
      }
    }
  }

}
