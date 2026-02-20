package controllers

import play.api.{Configuration, Environment, Logging, OptionalSourceMapper}
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results.*
import play.api.mvc.RequestHeader
import play.api.routing.Router

import javax.inject.Provider
import scala.concurrent.Future

class JsonHttpErrorHandler(
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with Logging {
  override def onProdServerError(request: RequestHeader, exception: play.api.UsefulException) = {
    logger.error(s"Internal server error on ${request.method} ${request.uri}", exception)
    Future.successful(
      InternalServerError(Json.obj("error" -> "Internal server error"))
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      Forbidden(Json.obj("error" -> "You're not allowed to access this resource."))
    )
  }
}
