package controllers

import cats.effect.unsafe.IORuntime
import domain.CharacterSheet
import play.api.libs.json.*
import play.api.mvc.*
import services.CharacterSheetRenderer

import scala.concurrent.{ExecutionContext, Future}

final class CharacterSheetController(
  renderer: CharacterSheetRenderer,
  cc:       ControllerComponents
)(using runtime: IORuntime, ec: ExecutionContext)
    extends AbstractController(cc):

  def renderGeneralCharacterSheet: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CharacterSheet] match
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      case JsSuccess(sheet, _) =>
        renderer.renderHtml(sheet)
          .map(html => Ok(html).as(HTML))
          .unsafeToFuture()
  }
