package services

import cats.effect.IO
import domain.{CharacterSheet, LegendaryCharacterSheet, RenderRecord, SheetType}

final class CharacterSheetService(
  renderer:      CharacterSheetRenderer,
  renderService: RenderService
):

  def renderGeneral(sheet: CharacterSheet): IO[String] =
    for
      html <- renderer.renderHtml(sheet)
      _    <- renderService.saveRender(SheetType.General, sheet.CharacterName, sheet.Level, html)
    yield html

  def renderLegendary(sheet: LegendaryCharacterSheet): IO[String] =
    for
      html <- renderer.renderLegendaryHtml(sheet)
      _    <- renderService.saveRender(SheetType.Legendary, sheet.CharacterName, sheet.Level, html)
    yield html

  def findByLevel(level: Int): IO[List[RenderRecord]] =
    renderService.findByLevel(level)

  def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
    renderService.findBySheetType(sheetType)

  def searchByCharacterName(query: String): IO[List[RenderRecord]] =
    renderService.searchByCharacterName(query)
