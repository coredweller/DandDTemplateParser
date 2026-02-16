package services

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import domain.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.Json

class CharacterSheetRendererSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private val renderer = CharacterSheetRenderer()

  private val sampleSheet = CharacterSheet(
    CharacterName = "Thorn Ironforge",
    Level         = 5,
    Race          = "Dwarf",
    Class         = "Fighter",
    Alignment     = "Lawful Good",
    HP            = "45",
    AC            = 18,
    Speed         = "25 ft.",
    AbilityScores = AbilityScores(
      Strength     = AbilityScore(16, "+3"),
      Dexterity    = AbilityScore(12, "+1"),
      Constitution = AbilityScore(14, "+2"),
      Intelligence = AbilityScore(10, "+0"),
      Wisdom       = AbilityScore(13, "+1"),
      Charisma     = AbilityScore(8, "-1")
    ),
    SavingThrows  = Map("Strength" -> "+6", "Constitution" -> "+5"),
    Skills        = Map("Athletics" -> "+6", "Intimidation" -> "+2"),
    Senses        = "Darkvision 60 ft., Passive Perception 11",
    Languages     = "Common, Dwarvish",
    SpecialTraits = Map("Second Wind" -> "Regain 1d10+5 HP as a bonus action"),
    Actions       = Map("Warhammer" -> "+6 to hit, 1d8+3 bludgeoning"),
    Equipment     = Equipment(Armor = "Chain Mail", Weapons = "Warhammer, Handaxe", Other = "Explorer's Pack"),
    Notes         = "Veteran of the Siege of Ironhold"
  )

  "CharacterSheetRenderer.renderHtml" should {
    "produce valid HTML containing the character name" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("Thorn Ironforge")
        html should include("<!DOCTYPE html>")
        html should include("</html>")
      }
    }

    "include combat stats" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("45")   // HP
        html should include("18")   // AC
        html should include("25 ft.") // Speed
      }
    }

    "include ability scores" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("STR")
        html should include("+3")
        html should include("16")
      }
    }

    "include saving throws and skills" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("Saving Throws")
        html should include("+6")
        html should include("Athletics")
      }
    }

    "include special traits and actions" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("Second Wind")
        html should include("Warhammer")
      }
    }

    "include equipment" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("Chain Mail")
        html should include("Explorer&#39;s Pack")
      }
    }

    "include notes" in {
      renderer.renderHtml(sampleSheet).asserting { html =>
        html should include("Veteran of the Siege of Ironhold")
      }
    }

    "escape HTML in character name" in {
      val xssSheet = sampleSheet.copy(CharacterName = "<script>alert('xss')</script>")
      renderer.renderHtml(xssSheet).asserting { html =>
        html should not include "<script>"
        html should include("&lt;script&gt;")
      }
    }

    "omit empty sections" in {
      val minimalSheet = sampleSheet.copy(
        SavingThrows  = Map.empty,
        Skills        = Map.empty,
        SpecialTraits = Map.empty,
        Actions       = Map.empty,
        Notes         = ""
      )
      renderer.renderHtml(minimalSheet).asserting { html =>
        html should not include "Saving Throws"
        html should not include "Special Traits"
        html should not include "<section class=\"notes\">"
      }
    }
  }

  "CharacterSheet JSON format" should {
    "round-trip through JSON" in {
      val json   = Json.toJson(sampleSheet)
      val parsed = json.as[CharacterSheet]
      IO.pure(parsed).asserting(_ shouldBe sampleSheet)
    }

    "parse the blank template structure" in {
      val templateJson = Json.parse(
        """{
          |  "CharacterName": "",
          |  "Level": 0,
          |  "Race": "",
          |  "Class": "",
          |  "Alignment": "",
          |  "HP": "",
          |  "AC": 0,
          |  "Speed": "",
          |  "AbilityScores": {
          |    "Strength": { "Score": 0, "Modifier": "+0" },
          |    "Dexterity": { "Score": 0, "Modifier": "+0" },
          |    "Constitution": { "Score": 0, "Modifier": "+0" },
          |    "Intelligence": { "Score": 0, "Modifier": "+0" },
          |    "Wisdom": { "Score": 0, "Modifier": "+0" },
          |    "Charisma": { "Score": 0, "Modifier": "+0" }
          |  },
          |  "SavingThrows": {},
          |  "Skills": {},
          |  "Senses": "",
          |  "Languages": "",
          |  "SpecialTraits": {},
          |  "Actions": {},
          |  "Equipment": {
          |    "Armor": "",
          |    "Weapons": "",
          |    "Other": ""
          |  },
          |  "Notes": ""
          |}""".stripMargin
      )
      val sheet = templateJson.as[CharacterSheet]
      IO.pure(sheet.CharacterName).asserting(_ shouldBe "")
    }
  }
