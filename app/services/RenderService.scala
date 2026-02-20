package services

import cats.effect.IO
import domain.{OptionsAlreadyTakenErrorResponse, RenderId, RenderRecord, SheetType}
import repositories.RenderRepository

import java.sql.SQLIntegrityConstraintViolationException
import java.time.Instant

final class RenderService(repo: RenderRepository):

  def saveRender(
    sheetType:     SheetType,
    characterName: String,
    level:         Int,
    responseHtml:  String
  ): IO[Either[OptionsAlreadyTakenErrorResponse, RenderRecord]] =
    val record = RenderRecord(
      id            = RenderId.generate(),
      sheetType     = sheetType,
      characterName = characterName,
      level         = level,
      responseHtml  = responseHtml,
      createdAt     = Instant.now()
    )
    repo.save(record).map(Right(_)).handleErrorWith {
      case _: SQLIntegrityConstraintViolationException =>
        repo.searchByCharacterName(characterName)
          .map(records => Left(OptionsAlreadyTakenErrorResponse(records.map(_.characterName).distinct)))
      case other =>
        IO.raiseError(other)
    }

  def findByLevel(level: Int): IO[List[RenderRecord]] =
    repo.findByLevel(level)

  def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
    repo.findBySheetType(sheetType)

  def searchByCharacterName(query: String): IO[List[RenderRecord]] =
    repo.searchByCharacterName(query)
