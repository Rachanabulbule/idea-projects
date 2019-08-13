package helpers

import actors._
import actors.SessionsScheduler.{CancelScheduledSession, GetScheduledSessions, ScheduleSession, ScheduledSessions}
import actors.UsersBanScheduler.GetScheduledBannedUsers
import actors.YouTubeDetailsActor.{GetCategories, GetDetails, UpdateVideoDetails}
import akka.actor.Actor
import com.google.api.services.youtube.model.{Video, VideoCategory, VideoCategorySnippet}
import org.apache.commons.mail.EmailException
import play.api.Logger

import scala.collection.mutable.ListBuffer

case class AddSessionUploader(sessionId: String)

case class RemoveSessionUploader(sessionId: String)

case object InsertTrue

case object InsertFalse

class DummySessionsScheduler extends Actor {

  var list: ListBuffer[Boolean] = ListBuffer()

  def receive: Receive = {
    case GetScheduledSessions              => sender ! ScheduledSessions(List.empty)
    case CancelScheduledSession(sessionId) => sender ! list.head
    case ScheduleSession(sessionId)        => sender ! true
    case InsertTrue                        => list += true
    case InsertFalse                       => list += false
  }

}

class DummyUsersBanScheduler extends Actor {

  def receive: Receive = {
    case GetScheduledBannedUsers => sender ! List.empty
  }

}

class TestEmailActor extends Actor {

  def receive: Receive = {
    case EmailActor.SendEmail(_, _, subject, _) if subject == "Exception" => throw new Exception
    case EmailActor.SendEmail(_, _, subject, _) if subject == "crash" => throw new EmailException
    case request: EmailActor.SendEmail                                => sender ! request
  }

}

class DummyYouTubeProgressManager extends Actor {

  var sessionUploaders: Map[String, Double] = Map.empty

  override def receive: Receive = {
    case YouTubeProgressManager.VideoId(sessionId)           => sender() ! Some(new Video)
    case AddSessionUploader(sessionId)                       => sessionUploaders += sessionId -> 50D
    case RemoveSessionUploader(sessionId)                    => sessionUploaders -= sessionId
    case YouTubeProgressManager.GetUploadProgress(sessionId) => sender() ! sessionUploaders.get(sessionId)
  }

}

class DummyYouTubeUploader extends Actor {

  override def receive: Receive = {
    case request: YouTubeUploader.Upload => sender() ! request
    case _                               => sender() ! "This should not print in DummyYouTubeUploader"
  }

}

class DummyYouTubeDetailsActor extends Actor {

  override def receive: Receive = {
    case GetCategories               => sender() ! List[VideoCategory]()
    case request: UpdateVideoDetails => sender() ! request
    case request: GetDetails         => sender() ! request
    case _                           => Logger.info("Unrecognized Message")
  }

}

class DummyYouTubeManager extends Actor {

  override def receive: Receive = {
    case request: YouTubeUploader.Upload => sender() ! "Upload started"
    case request: UpdateVideoDetails     => sender() ! "Updated video details"
    case GetCategories                   =>
    val videoCategorySnippet = new VideoCategorySnippet().setTitle("Education")
    sender() ! List(new VideoCategory().setSnippet(videoCategorySnippet).setId("12"))
    case request: GetDetails             => sender() ! None
    case _                               => sender() ! "What?!?!?!"
  }
}
