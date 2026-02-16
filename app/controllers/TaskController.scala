package controllers

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import domain.{TaskId, TaskError}
import play.api.libs.json.*
import play.api.mvc.*
import services.TaskService

import scala.concurrent.Future

final class TaskController(
  service: TaskService,
  cc:      ControllerComponents
)(using runtime: IORuntime)
    extends AbstractController(cc):

  // ── Helper: IO[Either[TaskError, A]] → Future[Result] ─────
  private def toResult[A](io: IO[Either[TaskError, A]])(f: A => Result): Future[Result] =
    io.map {
      case Right(value)                          => f(value)
      case Left(TaskError.NotFound(id))          => NotFound(Json.obj("error" -> s"Task $id not found"))
      case Left(TaskError.ValidationError(msg))  => BadRequest(Json.obj("error" -> msg))
    }.unsafeToFuture()

  def list: Action[AnyContent] = Action.async {
    service.listAll()
      .map(tasks => Ok(Json.toJson(tasks)))
      .unsafeToFuture()
  }

  def findById(id: String): Action[AnyContent] = Action.async {
    TaskId.fromString(id) match
      case Left(err)  => Future.successful(BadRequest(Json.obj("error" -> err)))
      case Right(tid) => toResult(service.findById(tid))(task => Ok(Json.toJson(task)))
  }

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateTaskRequest] match
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      case JsSuccess(req, _) =>
        toResult(service.create(req.title))(task => Created(Json.toJson(task)))
  }

// ── Request DTO ───────────────────────────────────────────────
case class CreateTaskRequest(title: String)
object CreateTaskRequest:
  given Reads[CreateTaskRequest] = Json.reads[CreateTaskRequest]
