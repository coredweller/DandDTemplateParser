package controllers

import cats.effect.unsafe.IORuntime
import domain.{CharacterSheet, LegendaryCharacterSheet}
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
        service.renderGeneral(sheet, request.body.toString)
          .map(html => Ok(html).as(HTML))
          .unsafeToFuture()
  }

  def renderLegendaryCharacterSheet: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[LegendaryCharacterSheet] match
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      case JsSuccess(sheet, _) =>
        service.renderLegendary(sheet, request.body.toString)
          .map(html => Ok(html).as(HTML))
          .unsafeToFuture()
  }
