package services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import domain.{Task, TaskError, TaskId}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.InMemoryTaskRepository

class TaskServiceSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def makeService: IO[TaskService] =
    InMemoryTaskRepository.make().map(TaskService(_))

  "TaskService.listAll" should {
    "return empty list initially" in {
      makeService.flatMap(_.listAll()).asserting(_ shouldBe empty)
    }
  }

  "TaskService.create" should {
    "create a task with a generated ID" in {
      makeService.flatMap { svc =>
        svc.create("Buy milk").asserting {
          case Right(task) =>
            task.title shouldBe "Buy milk"
          case Left(err) =>
            fail(s"Expected Right but got Left($err)")
        }
      }
    }

    "reject blank titles" in {
      makeService.flatMap(_.create("  ")).asserting {
        _ shouldBe Left(TaskError.ValidationError("title must not be blank"))
      }
    }
  }

  "TaskService.findById" should {
    "return NotFound for unknown ID" in {
      val unknownId = TaskId.generate()
      makeService.flatMap(_.findById(unknownId)).asserting {
        _ shouldBe Left(TaskError.NotFound(unknownId))
      }
    }

    "return the task after creation" in {
      makeService.flatMap { svc =>
        for
          created <- svc.create("Walk the dog")
          id       = created.toOption.get.id
          found   <- svc.findById(id)
        yield found shouldBe Right(created.toOption.get)
      }
    }
  }
