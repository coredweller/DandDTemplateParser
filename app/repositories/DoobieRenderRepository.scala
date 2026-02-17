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

  def searchByCharacterName(query: String): IO[List[RenderRecord]] =
    val pattern = s"%$query%"
    sql"""SELECT id, sheet_type, character_name, level, request_json, response_html, created_at
         |FROM character_sheet_renders
         |WHERE character_name LIKE $pattern
         |ORDER BY created_at DESC
         """.stripMargin.query[RenderRecord].to[List]
      .transact(xa)

  def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
    sql"""SELECT id, sheet_type, character_name, level, request_json, response_html, created_at
         |FROM character_sheet_renders
         |WHERE sheet_type = $sheetType
         |ORDER BY created_at DESC
         """.stripMargin.query[RenderRecord].to[List]
      .transact(xa)

  def findByLevel(level: Int): IO[List[RenderRecord]] =
    sql"""SELECT id, sheet_type, character_name, level, request_json, response_html, created_at
         |FROM character_sheet_renders
         |WHERE level = $level
         |ORDER BY created_at DESC
         """.stripMargin.query[RenderRecord].to[List]
      .transact(xa)

  def save(record: RenderRecord): IO[RenderRecord] =
    sql"""INSERT INTO character_sheet_renders
         |  (id, sheet_type, character_name, level, request_json, response_html, created_at)
         |VALUES
         |  (${record.id}, ${record.sheetType}, ${record.characterName},
         |   ${record.level}, ${record.requestJson}, ${record.responseHtml}, ${record.createdAt})
         """.stripMargin.update.run
      .transact(xa)
      .as(record)
