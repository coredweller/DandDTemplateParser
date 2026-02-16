package loader

import cats.effect.unsafe.IORuntime
import controllers.{CharacterSheetController, HealthController, TaskController}
import doobie.hikari.HikariTransactor
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import repositories.{DoobieRenderRepository, InMemoryTaskRepository}
import router.Routes
import services.{CharacterSheetRenderer, CharacterSheetService, RenderService, TaskService}

import scala.concurrent.ExecutionContext

class AppLoader extends play.api.ApplicationLoader:
  def load(context: Context): play.api.Application =
    new AppComponents(context).application

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents:

  // Cats Effect runtime — one per JVM, lives for app lifetime
  given IORuntime = IORuntime.global
  given ExecutionContext = executionContext

  // ── Database ────────────────────────────────────────────────
  private val dbConfig = configuration.get[play.api.Configuration]("db.default")
  private val transactor: HikariTransactor[cats.effect.IO] =
    HikariTransactor.newHikariTransactor[cats.effect.IO](
      driverClassName = dbConfig.get[String]("driver"),
      url             = dbConfig.get[String]("url"),
      user            = dbConfig.get[String]("username"),
      pass            = dbConfig.get[String]("password"),
      connectEC       = executionContext
    ).allocated.unsafeRunSync()._1 //This makes it fail at boot if MySql isnt running. Bad idea? Consider investigating as human

  // ── Task wiring ─────────────────────────────────────────────
  private val taskRepo: InMemoryTaskRepository =
    InMemoryTaskRepository.make().unsafeRunSync()

  private val taskService    = TaskService(taskRepo)
  private val taskController = TaskController(taskService, controllerComponents)
  private val healthController = HealthController(controllerComponents)

  // ── Character sheet wiring ──────────────────────────────────
  private val renderRepo    = DoobieRenderRepository(transactor)
  private val renderService = RenderService(renderRepo)
  private val renderer      = CharacterSheetRenderer()
  private val characterSheetService    = CharacterSheetService(renderer, renderService)
  private val characterSheetController = CharacterSheetController(characterSheetService, controllerComponents)

  // Play's generated router from conf/routes
  override def router: Router =
    new Routes(httpErrorHandler, healthController, taskController, characterSheetController)

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters
