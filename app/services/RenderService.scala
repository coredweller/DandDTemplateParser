package services

import cats.effect.IO
import domain.{RenderId, RenderRecord, RenderSummary, SheetType}
import domain.RenderRecord.toSummary
import repositories.RenderRepository

import java.time.Instant

final class RenderService(repo: RenderRepository):

  def saveRender(
    sheetType:     SheetType,
    characterName: String,
    level:         Int,
    requestJson:   String,
    responseHtml:  String
  ): IO[RenderRecord] =
    val now    = Instant.now()
    val record = RenderRecord(
      id            = RenderId.generate(),
      sheetType     = sheetType,
      characterName = characterName,
      level         = level,
      requestJson   = requestJson,
      responseHtml  = responseHtml,
      createdAt     = now
    )
    repo.save(record)

  def findByLevel(level: Int): IO[List[RenderSummary]] =
    repo.findByLevel(level).map(_.map(_.toSummary))

  def findBySheetType(sheetType: SheetType): IO[List[RenderSummary]] =
    repo.findBySheetType(sheetType).map(_.map(_.toSummary))

  def searchByCharacterName(query: String): IO[List[RenderSummary]] =
    repo.searchByCharacterName(query).map(_.map(_.toSummary))
