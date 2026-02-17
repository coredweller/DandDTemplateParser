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
  sheetType:     SheetType,
  characterName: String,
  level:         Int,
  requestJson:   String,
  responseHtml:  String,
  createdAt:     Instant
)

object RenderRecord:
  given OFormat[RenderRecord] = Json.format[RenderRecord]

  extension (r: RenderRecord)
    def toSummary: RenderSummary = RenderSummary(
      id            = r.id,
      sheetType     = r.sheetType,
      characterName = r.characterName,
      level         = r.level,
      responseHtml  = r.responseHtml,
      createdAt     = r.createdAt
    )

// ── Render Summary (excludes requestJson) ────────────────────
case class RenderSummary(
  id:            RenderId,
  sheetType:     SheetType,
  characterName: String,
  level:         Int,
  responseHtml:  String,
  createdAt:     Instant
)

object RenderSummary:
  given OFormat[RenderSummary] = Json.format[RenderSummary]
