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
    def findByLevel(level: Int): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.level == level))
    def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.sheetType == sheetType))

  private def makeService: IO[(RenderService, Ref[IO, List[RenderRecord]])] =
    Ref.of[IO, List[RenderRecord]](Nil).map { store =>
      (RenderService(InMemoryRenderRepository(store)), store)
    }

  "RenderService.saveRender" should {
    "create a record with correct fields" in {
      makeService.flatMap { (service, _) =>
        service.saveRender(SheetType.General, "Thorn Ironforge", 5, """{"CharacterName":"Thorn"}""", "<html>test</html>")
          .flatMap { record =>
            IO.pure(record).asserting { r =>
              r.sheetType     shouldBe SheetType.General
              r.characterName shouldBe "Thorn Ironforge"
              r.level         shouldBe 5
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
          r1 <- service.saveRender(SheetType.General, "A", 1, "{}", "<html/>")
          r2 <- service.saveRender(SheetType.Legendary, "B", 10, "{}", "<html/>")
        yield (r1, r2)
      }.asserting { (r1, r2) =>
        r1.id should not be r2.id
      }
    }

    "persist the record to the repository" in {
      makeService.flatMap { (service, store) =>
        for
          _     <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "{}", "<html/>")
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
        service.saveRender(SheetType.General, "Test", 3, "{}", "<html/>")
      }.asserting { record =>
        // name format: "{characterName} - {sheetType} - {yyyy-MM-dd HH:mm:ss}"
        record.name should fullyMatch regex """Test - general - \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}"""
      }
    }
  }

  "RenderService.findByLevel" should {
    "return matching records as summaries without requestJson" in {
      makeService.flatMap { (service, _) =>
        for
          _        <- service.saveRender(SheetType.General, "Thorn", 5, """{"big":"json"}""", "<html>thorn</html>")
          _        <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "{}", "<html>tiamat</html>")
          _        <- service.saveRender(SheetType.General, "Gimli", 5, """{"other":"data"}""", "<html>gimli</html>")
          results  <- service.findByLevel(5)
        yield results
      }.asserting { results =>
        results should have size 2
        results.map(_.characterName) should contain allOf ("Thorn", "Gimli")
        results.map(_.level).distinct shouldBe List(5)
        results.foreach(s => assert(s.responseHtml.nonEmpty))
        succeed
      }
    }

    "return empty list for non-matching level" in {
      makeService.flatMap { (service, _) =>
        for
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "{}", "<html/>")
          results <- service.findByLevel(99)
        yield results
      }.asserting { results =>
        results shouldBe empty
      }
    }
  }

  "RenderService.findBySheetType" should {
    "return only records matching the sheet type" in {
      makeService.flatMap { (service, _) =>
        for
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "{}", "<html>thorn</html>")
          _       <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "{}", "<html>tiamat</html>")
          _       <- service.saveRender(SheetType.General, "Gimli", 8, "{}", "<html>gimli</html>")
          results <- service.findBySheetType(SheetType.General)
        yield results
      }.asserting { results =>
        results should have size 2
        results.map(_.characterName) should contain allOf ("Thorn", "Gimli")
        results.map(_.sheetType).distinct shouldBe List(SheetType.General)
        succeed
      }
    }

    "return empty list when no records match" in {
      makeService.flatMap { (service, _) =>
        for
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "{}", "<html/>")
          results <- service.findBySheetType(SheetType.Legendary)
        yield results
      }.asserting { results =>
        results shouldBe empty
      }
    }
  }
