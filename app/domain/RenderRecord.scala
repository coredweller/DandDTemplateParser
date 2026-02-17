package domain

import play.api.libs.json.*

import java.time.Instant
import java.util.UUID

// ── Opaque type for render identity ──────────────────────────
opaque type RenderId = UUID

object RenderId:
  def apply(uuid: UUID): RenderId     = uuid
  def generate(): RenderId            = UUID.randomUUID()
  def fromString(s: String): Either[String, RenderId] =
    try Right(UUID.fromString(s))
    catch case _: IllegalArgumentException => Left(s"Invalid RenderId: $s")

  given Format[RenderId] = Format(
    Reads(_.validate[String].flatMap { s =>
      fromString(s).fold(
        err => JsError(err),
        id  => JsSuccess(id)
      )
    }),
    Writes(id => JsString(id.toString))
  )

  extension (id: RenderId)
    def toUUID: UUID     = id
    def asString: String = id.toString

// ── Sheet type enum ──────────────────────────────────────────
enum SheetType(val value: String):
  case General   extends SheetType("general")
  case Legendary extends SheetType("legendary")

object SheetType:
  def fromString(s: String): Either[String, SheetType] = s match
    case "general"   => Right(SheetType.General)
    case "legendary" => Right(SheetType.Legendary)
    case other       => Left(s"Unknown sheet type: $other")

  given Format[SheetType] = Format(
    Reads(_.validate[String].flatMap { s =>
      fromString(s).fold(err => JsError(err), st => JsSuccess(st))
    }),
    Writes(st => JsString(st.value))
  )

// ── Render Record ────────────────────────────────────────────
case class RenderRecord(
  id:            RenderId,
  name:          String,
  sheetType:     SheetType,
  characterName: String,
  level:         Int,
  requestJson:   String,
  responseHtml:  String,
  createdAt:     Instant
)

object RenderRecord:
  given OFormat[RenderRecord] = Json.format[RenderRecord]
