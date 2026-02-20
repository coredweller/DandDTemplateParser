package controllers

import cats.effect.IO
import cats.effect.Ref
import cats.effect.unsafe.IORuntime
import domain.*
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import play.api.test.*
import play.api.test.Helpers.*
import repositories.RenderRepository
import services.{CharacterSheetRenderer, CharacterSheetService, RenderService}

import scala.concurrent.ExecutionContext

class CharacterSheetControllerSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll:

  private val actorSystem: ActorSystem = ActorSystem("CharacterSheetControllerSpec")
  private given Materializer           = Materializer(actorSystem)
  private given IORuntime              = IORuntime.global
  private given ExecutionContext       = ExecutionContext.global

  override def afterAll(): Unit =
    actorSystem.terminate()
    super.afterAll()

  // ── In-memory repo (same approach as RenderServiceSpec) ─────────
  private class InMemoryRenderRepository(store: Ref[IO, List[RenderRecord]])
      extends RenderRepository:
    def save(record: RenderRecord): IO[RenderRecord] =
      store.modify { records =>
        if records.exists(_.characterName == record.characterName) then
          (records, IO.raiseError(new java.sql.SQLIntegrityConstraintViolationException(
            s"Duplicate entry '${record.characterName}'"
          )))
        else
          (record :: records, IO.pure(record))
      }.flatten
    def findByLevel(level: Int): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.level == level))
    def findBySheetType(sheetType: SheetType): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.sheetType == sheetType))
    def searchByCharacterName(query: String): IO[List[RenderRecord]] =
      store.get.map(_.filter(_.characterName.toLowerCase.contains(query.toLowerCase)))

  // Fresh controller (and fresh in-memory store) per test
  private def makeController: CharacterSheetController =
    val store   = Ref.of[IO, List[RenderRecord]](Nil).unsafeRunSync()
    val service = CharacterSheetService(
      CharacterSheetRenderer(),
      RenderService(InMemoryRenderRepository(store))
    )
    CharacterSheetController(service, stubControllerComponents())

  // ── Fixture JSON (PascalCase wire format) ────────────────────────
  private val validGeneralJson: JsValue = Json.parse(
    """{
      |  "CharacterName": "Thorn Ironforge",
      |  "Level": 5,
      |  "Race": "Dwarf",
      |  "Class": "Fighter",
      |  "Alignment": "Lawful Good",
      |  "HP": "45",
      |  "AC": 18,
      |  "Speed": "25 ft.",
      |  "AbilityScores": {
      |    "Strength":     { "Score": 16, "Modifier": "+3" },
      |    "Dexterity":    { "Score": 12, "Modifier": "+1" },
      |    "Constitution": { "Score": 14, "Modifier": "+2" },
      |    "Intelligence": { "Score": 10, "Modifier": "+0" },
      |    "Wisdom":       { "Score": 13, "Modifier": "+1" },
      |    "Charisma":     { "Score":  8, "Modifier": "-1" }
      |  },
      |  "SavingThrows": { "Strength": "+6" },
      |  "Skills":       { "Athletics": "+6" },
      |  "Senses":       "Darkvision 60 ft.",
      |  "Languages":    "Common, Dwarvish",
      |  "SpecialTraits": {},
      |  "Actions":       {},
      |  "Equipment": { "Armor": "Chain Mail", "Weapons": "Warhammer", "Other": "" },
      |  "Notes": ""
      |}""".stripMargin
  )

  // ── POST /api/v1/character/render ────────────────────────────────
  "CharacterSheetController.renderGeneralCharacterSheet" should {

    "return 200 with a render response for valid input" in {
      val result = makeController.renderGeneralCharacterSheet(
        FakeRequest(POST, "/api/v1/character/render").withBody(validGeneralJson)
      )
      status(result) shouldBe OK
      val json = contentAsJson(result)
      (json \ "characterName").as[String] shouldBe "Thorn Ironforge"
      (json \ "sheetType").as[String]     shouldBe "general"
      (json \ "level").as[Int]            shouldBe 5
      (json \ "responseHtml").as[String] should include("<!DOCTYPE html>")
    }

    "return 400 for a request missing required fields" in {
      val result = makeController.renderGeneralCharacterSheet(
        FakeRequest(POST, "/api/v1/character/render").withBody[JsValue](Json.obj("CharacterName" -> "Broken"))
      )
      status(result)                               shouldBe BAD_REQUEST
      (contentAsJson(result) \ "error").isDefined  shouldBe true
    }

    "return 409 when the character name already exists" in {
      val controller = makeController
      val request    = FakeRequest(POST, "/api/v1/character/render").withBody(validGeneralJson)
      controller.renderGeneralCharacterSheet(request)        // seed
      val result = controller.renderGeneralCharacterSheet(request)  // duplicate
      status(result) shouldBe CONFLICT
    }
  }

  // ── GET /api/v1/character/renders/level/:level ───────────────────
  "CharacterSheetController.findByLevel" should {

    "return 200 with an empty array when no renders exist at that level" in {
      val result = makeController.findByLevel(99)(
        FakeRequest(GET, "/api/v1/character/renders/level/99")
      )
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe JsArray.empty
    }

    "return the render after it has been saved" in {
      val controller = makeController
      // Block until save completes before querying
      status(controller.renderGeneralCharacterSheet(
        FakeRequest(POST, "/api/v1/character/render").withBody(validGeneralJson)
      )) shouldBe OK
      val result = controller.findByLevel(5)(
        FakeRequest(GET, "/api/v1/character/renders/level/5")
      )
      status(result) shouldBe OK
      val arr = contentAsJson(result).as[JsArray]
      arr.value should have size 1
      (arr.value.head \ "characterName").as[String] shouldBe "Thorn Ironforge"
    }
  }

  // ── GET /api/v1/character/renders/type/:sheetType ────────────────
  "CharacterSheetController.findBySheetType" should {

    "return 400 for an unrecognised sheet type" in {
      val result = makeController.findBySheetType("unknown")(
        FakeRequest(GET, "/api/v1/character/renders/type/unknown")
      )
      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] should include("Unknown sheet type")
    }

    "return 200 with an empty array for a valid type with no data" in {
      val result = makeController.findBySheetType("legendary")(
        FakeRequest(GET, "/api/v1/character/renders/type/legendary")
      )
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe JsArray.empty
    }
  }
