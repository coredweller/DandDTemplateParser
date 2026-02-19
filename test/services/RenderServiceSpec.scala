package services

import cats.effect.{IO, Ref}
import cats.effect.testing.scalatest.AsyncIOSpec
import domain.{OptionsAlreadyTakenErrorResponse, RenderRecord, SheetType}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.RenderRepository

class RenderServiceSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private class InMemoryRenderRepository(store: Ref[IO, List[RenderRecord]])
      extends RenderRepository:
    def save(record: RenderRecord): IO[RenderRecord] =
      store.update(record :: _).as(record)
    def existsByCharacterName(name: String): IO[Boolean] =
      store.get.map(_.exists(_.characterName == name))
    def findByLevel(level: Int): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.level == level))
    def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.sheetType == sheetType))
    def searchByCharacterName(query: String): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.characterName.toLowerCase.contains(query.toLowerCase)))

  private def makeService: IO[(RenderService, Ref[IO, List[RenderRecord]])] =
    Ref.of[IO, List[RenderRecord]](Nil).map { store =>
      (RenderService(InMemoryRenderRepository(store)), store)
    }

  "RenderService.saveRender" should {
    "create a record with correct fields" in {
      makeService.flatMap { (service, _) =>
        service.saveRender(SheetType.General, "Thorn Ironforge", 5, "<html>test</html>")
          .asserting { result =>
            result shouldBe a[Right[?, ?]]
            val r = result.toOption.get
            r.sheetType     shouldBe SheetType.General
            r.characterName shouldBe "Thorn Ironforge"
            r.level         shouldBe 5
            r.responseHtml  shouldBe "<html>test</html>"
          }
      }
    }

    "generate a unique id for each record" in {
      makeService.flatMap { (service, _) =>
        for
          r1 <- service.saveRender(SheetType.General, "A", 1, "<html/>")
          r2 <- service.saveRender(SheetType.Legendary, "B", 10, "<html/>")
        yield (r1, r2)
      }.asserting { (r1, r2) =>
        r1.toOption.get.id should not be r2.toOption.get.id
      }
    }

    "persist the record to the repository" in {
      makeService.flatMap { (service, store) =>
        for
          _     <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "<html/>")
          saved <- store.get
        yield saved
      }.asserting { saved =>
        saved should have size 1
        saved.head.characterName shouldBe "Tiamat"
        saved.head.sheetType     shouldBe SheetType.Legendary
      }
    }

    "return Left with fuzzy matches when character name already exists" in {
      makeService.flatMap { (service, _) =>
        for
          _      <- service.saveRender(SheetType.General, "Ogre", 5, "<html/>")
          _      <- service.saveRender(SheetType.General, "Ogre1", 5, "<html/>")
          _      <- service.saveRender(SheetType.General, "Ogre Titan", 8, "<html/>")
          result <- service.saveRender(SheetType.General, "Ogre", 3, "<html/>")
        yield result
      }.asserting { result =>
        result shouldBe a[Left[?, ?]]
        val options = result.swap.toOption.get
        options shouldBe a[OptionsAlreadyTakenErrorResponse]
        options.alreadyTakenOptions should contain("Ogre")
        options.alreadyTakenOptions should contain("Ogre1")
        options.alreadyTakenOptions should contain("Ogre Titan")
      }
    }

    "return Right when character name does not exist" in {
      makeService.flatMap { (service, _) =>
        service.saveRender(SheetType.General, "Thorn", 5, "<html/>")
      }.asserting { result =>
        result shouldBe a[Right[?, ?]]
      }
    }
  }

  "RenderService.findByLevel" should {
    "return matching records" in {
      makeService.flatMap { (service, _) =>
        for
          _        <- service.saveRender(SheetType.General, "Thorn", 5, "<html>thorn</html>")
          _        <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "<html>tiamat</html>")
          _        <- service.saveRender(SheetType.General, "Gimli", 5, "<html>gimli</html>")
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
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "<html/>")
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
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "<html>thorn</html>")
          _       <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "<html>tiamat</html>")
          _       <- service.saveRender(SheetType.General, "Gimli", 8, "<html>gimli</html>")
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
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "<html/>")
          results <- service.findBySheetType(SheetType.Legendary)
        yield results
      }.asserting { results =>
        results shouldBe empty
      }
    }
  }

  "RenderService.searchByCharacterName" should {
    "return records with partial name match" in {
      makeService.flatMap { (service, _) =>
        for
          _       <- service.saveRender(SheetType.General, "Ogre Warchief", 8, "<html/>")
          _       <- service.saveRender(SheetType.Legendary, "Tiamat", 30, "<html/>")
          _       <- service.saveRender(SheetType.General, "Young Ogre", 3, "<html/>")
          results <- service.searchByCharacterName("Ogre")
        yield results
      }.asserting { results =>
        results should have size 2
        results.map(_.characterName) should contain allOf ("Ogre Warchief", "Young Ogre")
        succeed
      }
    }

    "be case-insensitive" in {
      makeService.flatMap { (service, _) =>
        for
          _       <- service.saveRender(SheetType.General, "Ogre Warchief", 8, "<html/>")
          results <- service.searchByCharacterName("ogre")
        yield results
      }.asserting { results =>
        results should have size 1
        results.head.characterName shouldBe "Ogre Warchief"
      }
    }

    "return empty list when no names match" in {
      makeService.flatMap { (service, _) =>
        for
          _       <- service.saveRender(SheetType.General, "Thorn", 5, "<html/>")
          results <- service.searchByCharacterName("Dragon")
        yield results
      }.asserting { results =>
        results shouldBe empty
      }
    }
  }
