package repositories

import cats.effect.{IO, Ref}
import domain.{Task, TaskId}

final class InMemoryTaskRepository(store: Ref[IO, Map[TaskId, Task]])
    extends TaskRepository:

  def findAll(): IO[List[Task]] =
    store.get.map(_.values.toList)

  def findById(id: TaskId): IO[Option[Task]] =
    store.get.map(_.get(id))

  def save(task: Task): IO[Task] =
    store.update(_ + (task.id -> task)).as(task)

  def delete(id: TaskId): IO[Boolean] =
    store.modify { m =>
      if m.contains(id) then (m - id, true)
      else (m, false)
    }

object InMemoryTaskRepository:
  def make(): IO[InMemoryTaskRepository] =
    Ref.of[IO, Map[TaskId, Task]](Map.empty)
      .map(new InMemoryTaskRepository(_))
