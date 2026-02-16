package loader

import cats.effect.unsafe.IORuntime
import controllers.{HealthController, TaskController}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import repositories.InMemoryTaskRepository
import router.Routes
import services.TaskService

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

  // ── Wiring ────────────────────────────────────────────────
  private val repo: InMemoryTaskRepository =
    InMemoryTaskRepository.make().unsafeRunSync()

  private val taskService    = TaskService(repo)
  private val taskController = TaskController(taskService, controllerComponents)
  private val healthController = HealthController(controllerComponents)

  // Play's generated router from conf/routes
  override def router: Router =
    new Routes(httpErrorHandler, healthController, taskController)

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters
