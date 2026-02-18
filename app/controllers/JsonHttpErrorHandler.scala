package controllers

import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}
import play.api.http.DefaultHttpErrorHandler
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
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {
  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      Forbidden("You're not allowed to access this resource.")
    )
  }
}
