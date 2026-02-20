package loader

import cats.effect.unsafe.IORuntime
import controllers.{CharacterSheetController, HealthController, JsonHttpErrorHandler}
import doobie.hikari.HikariTransactor
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.OptionalSourceMapper
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import repositories.DoobieRenderRepository
import router.Routes
import services.{CharacterSheetRenderer, CharacterSheetService, RenderService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

class AppLoader extends play.api.ApplicationLoader:
  def load(context: Context): play.api.Application =
    new AppComponents(context).application

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents:

  // Cats Effect runtime — one per JVM, lives for app lifetime
  given IORuntime = IORuntime.global
  given ExecutionContext = executionContext

  private val dbExecutionContext: ExecutionContext =
    actorSystem.dispatchers.lookup("contexts.database")

  // ── Database ────────────────────────────────────────────────
  private val dbConfig = configuration.get[play.api.Configuration]("db.default")
  private val transactor: HikariTransactor[cats.effect.IO] = {
    val (xa, release) =
      HikariTransactor.newHikariTransactor[cats.effect.IO](
        driverClassName = dbConfig.get[String]("driver"),
        url             = dbConfig.get[String]("url"),
        user            = dbConfig.get[String]("username"),
        pass            = dbConfig.get[String]("password"),
        connectEC       = dbExecutionContext
      ).allocated
        .timeout(30.seconds)
        .unsafeRunSync()
    try
      applicationLifecycle.addStopHook(() => release.unsafeToFuture())
      xa
    catch
      case ex: Throwable =>
        release.unsafeRunSync()
        throw ex
  }

  // ── Wiring ────────────────────────────────────────────────────
  private val healthController = HealthController(controllerComponents)

  private val renderRepo    = DoobieRenderRepository(transactor)
  private val renderService = RenderService(renderRepo)
  private val renderer      = CharacterSheetRenderer()
  private val characterSheetService    = CharacterSheetService(renderer, renderService)
  private val characterSheetController = CharacterSheetController(characterSheetService, controllerComponents)

  override lazy val httpErrorHandler =
    JsonHttpErrorHandler(environment, configuration, OptionalSourceMapper(devContext.map(_.sourceMapper)), () => router)

  // Play's generated router from conf/routes
  override def router: Router =
    new Routes(httpErrorHandler, healthController, characterSheetController)

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters
