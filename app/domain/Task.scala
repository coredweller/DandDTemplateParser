package domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json.*

// ── Opaque type for domain identity ───────────────────────────
opaque type TaskId = UUID

object TaskId:
  def apply(uuid: UUID): TaskId      = uuid
  def generate(): TaskId             = UUID.randomUUID()
  def fromString(s: String): Either[String, TaskId] =
    try Right(UUID.fromString(s))
    catch case _: IllegalArgumentException => Left(s"Invalid TaskId: $s")

  // Scala 3 given — NOT implicit def
  given Format[TaskId] = Format(
    Reads(_.validate[String].flatMap { s =>
      fromString(s).fold(
        err => JsError(err),
        id  => JsSuccess(id)
      )
    }),
    Writes(id => JsString(id.toString))
  )

// ── Aggregate ─────────────────────────────────────────────────
case class Task(
  id:        TaskId,
  title:     String,
  createdAt: Instant
)

object Task:
  given OFormat[Task] = Json.format[Task]
