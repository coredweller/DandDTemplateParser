package controllers

import cats.effect.unsafe.IORuntime
import domain.{CharacterSheet, LegendaryCharacterSheet, OptionsAlreadyTakenErrorResponse, RenderResponse, SheetType}
import play.api.libs.json.*
import play.api.mvc.*
import services.CharacterSheetService

import scala.concurrent.{ExecutionContext, Future}

final class CharacterSheetController(
  service: CharacterSheetService,
  cc:      ControllerComponents
)(using runtime: IORuntime, ec: ExecutionContext)
    extends AbstractController(cc):

  def renderGeneralCharacterSheet: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CharacterSheet] match
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      case JsSuccess(sheet, _) =>
        service.renderGeneral(sheet)
          .map {
            case Left(response) => Conflict(Json.toJson(response))
            case Right(response) => Ok(Json.toJson(response))
          }
          .unsafeToFuture()
  }

  def renderLegendaryCharacterSheet: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[LegendaryCharacterSheet] match
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      case JsSuccess(sheet, _) =>
        service.renderLegendary(sheet)
          .map {
            case Left(response) => Conflict(Json.toJson(response))
            case Right(response) => Ok(Json.toJson(response))
          }
          .unsafeToFuture()
  }

  def findByLevel(level: Int): Action[AnyContent] = Action.async {
    service.findByLevel(level)
      .map(responses => Ok(Json.toJson(responses)))
      .unsafeToFuture()
  }

  def findBySheetType(sheetType: String): Action[AnyContent] = Action.async {
    SheetType.fromString(sheetType) match
      case Left(err) =>
        Future.successful(BadRequest(Json.obj("error" -> err)))
      case Right(st) =>
        service.findBySheetType(st)
          .map(responses => Ok(Json.toJson(responses)))
          .unsafeToFuture()
  }

  def searchByCharacterName: Action[JsValue] = Action.async(parse.json) { request =>
    (request.body \ "name").validate[String] match
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      case JsSuccess(name, _) =>
        service.searchByCharacterName(name)
          .map(responses => Ok(Json.toJson(responses)))
          .unsafeToFuture()
  }
