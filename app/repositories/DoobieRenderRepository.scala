package repositories

import cats.effect.IO
import domain.{RenderId, RenderRecord, SheetType}
import doobie.*
import doobie.implicits.*

import java.time.Instant
import java.util.UUID

final class DoobieRenderRepository(xa: Transactor[IO]) extends RenderRepository:

  // ── Doobie type mappings ─────────────────────────────────────
  private given Meta[RenderId] =
    Meta[String].imap(s => RenderId(UUID.fromString(s)))(_.asString)

  private given Meta[SheetType] =
    Meta[String].imap(s =>
      SheetType.fromString(s).fold(
        err => throw new IllegalArgumentException(err),
        identity
      )
    )(_.value)

  private given Meta[Instant] =
    Meta[java.sql.Timestamp].imap(_.toInstant)(java.sql.Timestamp.from)

  def save(record: RenderRecord): IO[RenderRecord] =
    sql"""INSERT INTO character_sheet_renders
         |  (id, name, sheet_type, character_name, request_json, response_html, created_at)
         |VALUES
         |  (${record.id}, ${record.name}, ${record.sheetType}, ${record.characterName},
         |   ${record.requestJson}, ${record.responseHtml}, ${record.createdAt})
         """.stripMargin.update.run
      .transact(xa)
      .as(record)
