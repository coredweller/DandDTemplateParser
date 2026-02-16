package repositories

import cats.effect.IO
import domain.{Task, TaskId}

trait TaskRepository:
  def findAll(): IO[List[Task]]
  def findById(id: TaskId): IO[Option[Task]]
  def save(task: Task): IO[Task]
  def delete(id: TaskId): IO[Boolean]
