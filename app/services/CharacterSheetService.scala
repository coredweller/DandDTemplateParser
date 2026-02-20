package services

import cats.effect.IO
import domain.{CharacterSheet, LegendaryCharacterSheet, OptionsAlreadyTakenErrorResponse, RenderResponse, SheetType}
import domain.RenderResponse.toResponse

final class CharacterSheetService(
  renderer:      CharacterSheetRenderer,
  renderService: RenderService
):

  def renderGeneral(sheet: CharacterSheet): IO[Either[OptionsAlreadyTakenErrorResponse, RenderResponse]] =
    for
      html   <- renderer.renderHtml(sheet)
      result <- renderService.saveRender(SheetType.General, sheet.characterName, sheet.level, html)
    yield result.map(_.toResponse)

  def renderLegendary(sheet: LegendaryCharacterSheet): IO[Either[OptionsAlreadyTakenErrorResponse, RenderResponse]] =
    for
      html   <- renderer.renderLegendaryHtml(sheet)
      result <- renderService.saveRender(SheetType.Legendary, sheet.characterName, sheet.level, html)
    yield result.map(_.toResponse)

  def findByLevel(level: Int): IO[List[RenderResponse]] =
    renderService.findByLevel(level).map(_.map(_.toResponse))

  def findBySheetType(sheetType: SheetType): IO[List[RenderResponse]] =
    renderService.findBySheetType(sheetType).map(_.map(_.toResponse))

  def searchByCharacterName(query: String): IO[List[RenderResponse]] =
    renderService.searchByCharacterName(query).map(_.map(_.toResponse))
