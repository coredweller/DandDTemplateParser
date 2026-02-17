package repositories

import cats.effect.IO
import domain.{RenderRecord, SheetType}

trait RenderRepository:
  def save(record: RenderRecord): IO[RenderRecord]
  def findByLevel(level: Int): IO[List[RenderRecord]]
  def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]]
  def searchByCharacterName(query: String): IO[List[RenderRecord]]
