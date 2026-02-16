package controllers

import play.api.libs.json.Json
import play.api.mvc.*
import java.time.Instant

final class HealthController(cc: ControllerComponents)
    extends AbstractController(cc):

  def check: Action[AnyContent] = Action {
    Ok(Json.obj(
      "status"    -> "ok",
      "timestamp" -> Instant.now().toString
    ))
  }
