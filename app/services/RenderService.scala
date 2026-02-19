package services

import cats.effect.IO
import domain.{RenderId, RenderRecord, SheetType}
import repositories.RenderRepository

import java.time.Instant

final class RenderService(repo: RenderRepository):

  def saveRender(
    sheetType:     SheetType,
    characterName: String,
    level:         Int,
    responseHtml:  String
  ): IO[Either[List[String], RenderRecord]] =
    repo.existsByCharacterName(characterName).flatMap {
      case true =>
        repo.searchByCharacterName(characterName)
          .map(records => Left(records.map(_.characterName).distinct))
      case false =>
        val now    = Instant.now()
        val record = RenderRecord(
          id            = RenderId.generate(),
          sheetType     = sheetType,
          characterName = characterName,
          level         = level,
          responseHtml  = responseHtml,
          createdAt     = now
        )
        repo.save(record).map(Right(_))
    }

  def findByLevel(level: Int): IO[List[RenderRecord]] =
    repo.findByLevel(level)

  def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
    repo.findBySheetType(sheetType)

  def searchByCharacterName(query: String): IO[List[RenderRecord]] =
    repo.searchByCharacterName(query)
