package services

import cats.effect.IO
import domain.{CharacterSheet, LegendaryCharacterSheet, RenderRecord, SheetType}

final class CharacterSheetService(
  renderer:      CharacterSheetRenderer,
  renderService: RenderService
):

  def renderGeneral(sheet: CharacterSheet): IO[Either[List[String], String]] =
    for
      html   <- renderer.renderHtml(sheet)
      result <- renderService.saveRender(SheetType.General, sheet.CharacterName, sheet.Level, html)
    yield result.map(_ => html)

  def renderLegendary(sheet: LegendaryCharacterSheet): IO[Either[List[String], String]] =
    for
      html   <- renderer.renderLegendaryHtml(sheet)
      result <- renderService.saveRender(SheetType.Legendary, sheet.CharacterName, sheet.Level, html)
    yield result.map(_ => html)

  def findByLevel(level: Int): IO[List[RenderRecord]] =
    renderService.findByLevel(level)

  def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
    renderService.findBySheetType(sheetType)

  def searchByCharacterName(query: String): IO[List[RenderRecord]] =
    renderService.searchByCharacterName(query)
