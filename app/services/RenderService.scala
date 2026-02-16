package services

import cats.effect.IO
import domain.{RenderId, RenderRecord, SheetType}
import repositories.RenderRepository

import java.time.Instant
import java.time.format.DateTimeFormatter

final class RenderService(repo: RenderRepository):

  private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def saveRender(
    sheetType:     SheetType,
    characterName: String,
    requestJson:   String,
    responseHtml:  String
  ): IO[RenderRecord] =
    val now    = Instant.now()
    val name   = s"$characterName - ${sheetType.value} - ${timestampFormat.format(now.atZone(java.time.ZoneOffset.UTC))}"
    val record = RenderRecord(
      id            = RenderId.generate(),
      name          = name,
      sheetType     = sheetType,
      characterName = characterName,
      requestJson   = requestJson,
      responseHtml  = responseHtml,
      createdAt     = now
    )
    repo.save(record)
