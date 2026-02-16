package services

import cats.effect.{IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import domain.{RenderRecord, SheetType}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.RenderRepository

class RenderServiceSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private class InMemoryRenderRepository(store: Ref[IO, List[RenderRecord]])
      extends RenderRepository:
    def save(record: RenderRecord): IO[RenderRecord] =
      store.update(record :: _).as(record)

  private def makeService: IO[(RenderService, Ref[IO, List[RenderRecord]])] =
    Ref.of[IO, List[RenderRecord]](Nil).map { store =>
      (RenderService(InMemoryRenderRepository(store)), store)
    }

  "RenderService.saveRender" should {
    "create a record with correct fields" in {
      makeService.flatMap { (service, _) =>
        service.saveRender(SheetType.General, "Thorn Ironforge", """{"CharacterName":"Thorn"}""", "<html>test</html>")
          .flatMap { record =>
            IO.pure(record).asserting { r =>
              r.sheetType     shouldBe SheetType.General
              r.characterName shouldBe "Thorn Ironforge"
              r.requestJson   shouldBe """{"CharacterName":"Thorn"}"""
              r.responseHtml  shouldBe "<html>test</html>"
              r.name          should include("Thorn Ironforge")
              r.name          should include("general")
            }
          }
      }
    }

    "generate a unique id for each record" in {
      makeService.flatMap { (service, _) =>
        for
          r1 <- service.saveRender(SheetType.General, "A", "{}", "<html/>")
          r2 <- service.saveRender(SheetType.Legendary, "B", "{}", "<html/>")
        yield (r1, r2)
      }.asserting { (r1, r2) =>
        r1.id should not be r2.id
      }
    }

    "persist the record to the repository" in {
      makeService.flatMap { (service, store) =>
        for
          _     <- service.saveRender(SheetType.Legendary, "Tiamat", "{}", "<html/>")
          saved <- store.get
        yield saved
      }.asserting { saved =>
        saved should have size 1
        saved.head.characterName shouldBe "Tiamat"
        saved.head.sheetType     shouldBe SheetType.Legendary
      }
    }

    "include timestamp in the name" in {
      makeService.flatMap { (service, _) =>
        service.saveRender(SheetType.General, "Test", "{}", "<html/>")
      }.asserting { record =>
        // name format: "{characterName} - {sheetType} - {yyyy-MM-dd HH:mm:ss}"
        record.name should fullyMatch regex """Test - general - \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}"""
      }
    }
  }
