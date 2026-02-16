package services

import cats.effect.IO
import domain.{CharacterSheet, LegendaryCharacterSheet, SheetType}

final class CharacterSheetService(
  renderer:      CharacterSheetRenderer,
  renderService: RenderService
):

  def renderGeneral(sheet: CharacterSheet, requestJson: String): IO[String] =
    for
      html <- renderer.renderHtml(sheet)
      _    <- renderService.saveRender(SheetType.General, sheet.CharacterName, requestJson, html)
    yield html

  def renderLegendary(sheet: LegendaryCharacterSheet, requestJson: String): IO[String] =
    for
      html <- renderer.renderLegendaryHtml(sheet)
      _    <- renderService.saveRender(SheetType.Legendary, sheet.CharacterName, requestJson, html)
    yield html
