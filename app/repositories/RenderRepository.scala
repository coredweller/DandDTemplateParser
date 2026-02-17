package repositories

import cats.effect.IO
import domain.RenderRecord

trait RenderRepository:
  def save(record: RenderRecord): IO[RenderRecord]
  def findByLevel(level: Int): IO[List[RenderRecord]]
