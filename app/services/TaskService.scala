package services

import cats.effect.IO
import domain.*
import repositories.TaskRepository
import java.time.Instant

final class TaskService(repo: TaskRepository):

  def listAll(): IO[List[Task]] =
    repo.findAll()

  def findById(id: TaskId): IO[Either[TaskError, Task]] =
    repo.findById(id).map {
      case Some(task) => Right(task)
      case None       => Left(TaskError.NotFound(id))
    }

  def create(title: String): IO[Either[TaskError, Task]] =
    if title.isBlank then
      IO.pure(Left(TaskError.ValidationError("title must not be blank")))
    else
      val task = Task(TaskId.generate(), title.trim, Instant.now())
      repo.save(task).map(Right(_))
