package domain

import play.api.libs.json.*

import java.time.Instant

// ── Duplicate character name error response ───────────────────
case class OptionsAlreadyTakenErrorResponse(alreadyTakenOptions: List[String])

object OptionsAlreadyTakenErrorResponse:
  given OFormat[OptionsAlreadyTakenErrorResponse] = Json.format[OptionsAlreadyTakenErrorResponse]

// ── API response model (decoupled from DB model RenderRecord) ─
case class RenderResponse(
  id:            RenderId,
  sheetType:     SheetType,
  characterName: String,
  level:         Int,
  responseHtml:  String,
  createdAt:     Instant
)

object RenderResponse:
  given OFormat[RenderResponse] = Json.format[RenderResponse]

  extension (r: RenderRecord)
    def toResponse: RenderResponse = RenderResponse(
      id            = r.id,
      sheetType     = r.sheetType,
      characterName = r.characterName,
      level         = r.level,
      responseHtml  = r.responseHtml,
      createdAt     = r.createdAt
    )
