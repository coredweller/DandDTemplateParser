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

    "parse the blank general template structure" in {
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

  // ── Legendary Character Sheet ────────────────────────────────

  private val sampleLegendary = LegendaryCharacterSheet(
    CharacterName       = "Tiamat, Queen of Dragons",
    Level               = 30,
    Race                = "Dragon God",
    Class               = "Deity",
    Alignment           = "Chaotic Evil",
    HP                  = "615 (30d20+300)",
    AC                  = 25,
    Speed               = "60 ft., fly 120 ft.",
    AbilityScores       = AbilityScores(
      Strength     = AbilityScore(30, "+10"),
      Dexterity    = AbilityScore(10, "+0"),
      Constitution = AbilityScore(30, "+10"),
      Intelligence = AbilityScore(26, "+8"),
      Wisdom       = AbilityScore(26, "+8"),
      Charisma     = AbilityScore(28, "+9")
    ),
    SavingThrows        = Map("STR" -> "+19", "DEX" -> "+9", "WIS" -> "+17"),
    Skills              = Map("Arcana" -> "+17", "Perception" -> "+26", "Religion" -> "+17"),
    DamageResistances   = "bludgeoning, piercing, slashing from nonmagical attacks",
    DamageImmunities    = "acid, cold, fire, lightning, poison",
    ConditionImmunities = "blinded, charmed, frightened, poisoned, stunned",
    Senses              = "darkvision 240 ft., truesight 120 ft., passive Perception 36",
    Languages           = "Common, Draconic, Infernal, telepathy 120 ft.",
    ChallengeRating     = "30",
    ProficiencyBonus    = "+9",
    SpecialTraits       = Map("Legendary Resistance (5/Day)" -> "If Tiamat fails a saving throw, she can choose to succeed instead."),
    Actions             = Map("Bite" -> "+19 to hit, 4d6+10 piercing plus 4d6 acid"),
    BonusActions        = Map("Chromatic Surge" -> "Exhale a burst of elemental energy"),
    Reactions           = Map("Reactive Heads" -> "Tiamat can take up to five reactions per round"),
    LegendaryTraits     = Map("Legendary Resistance (5/Day)" -> "Choose to succeed a failed saving throw"),
    LegendaryActions    = LegendaryActions(
      legendaryActionUses = "5",
      Options = Map("Bite Attack" -> "Tiamat makes a bite attack", "Wing Attack (2)" -> "Tiamat beats her wings")
    ),
    MythicTrait         = MythicTrait("Chromatic Rebirth", "When Tiamat is reduced to 0 HP, she regains all HP and each head ignites."),
    LairActions         = Map("Magma Eruption" -> "Magma erupts from the ground in a 20-foot radius"),
    RegionalEffects     = List("Draconic storms of acid, lightning, and fire rage within 6 miles", "Creatures within 1 mile feel an overwhelming sense of dread"),
    Equipment           = Equipment(Armor = "Natural Armor", Weapons = "Five Chromatic Heads", Other = "—"),
    Notes               = "The five-headed queen of evil dragonkind"
  )

  "CharacterSheetRenderer.renderLegendaryHtml" should {
    "produce valid HTML with legendary styling" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("Tiamat, Queen of Dragons")
        html should include("<!DOCTYPE html>")
        html should include("legendary")
      }
    }

    "include challenge rating and proficiency bonus" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("CR 30")
        html should include("Prof +9")
      }
    }

    "include defenses section" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("Damage Resistances")
        html should include("Damage Immunities")
        html should include("Condition Immunities")
        html should include("acid, cold, fire, lightning, poison")
      }
    }

    "include bonus actions and reactions" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("Bonus Actions")
        html should include("Chromatic Surge")
        html should include("Reactions")
        html should include("Reactive Heads")
      }
    }

    "include legendary traits and actions" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("Legendary Traits")
        html should include("Legendary Actions")
        html should include("5 legendary action uses per round")
        html should include("Bite Attack")
        html should include("Wing Attack (2)")
      }
    }

    "include mythic trait" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("Mythic Trait")
        html should include("Chromatic Rebirth")
        html should include("regains all HP")
      }
    }

    "include lair actions and regional effects" in {
      renderer.renderLegendaryHtml(sampleLegendary).asserting { html =>
        html should include("Lair Actions")
        html should include("Magma Eruption")
        html should include("Regional Effects")
        html should include("Draconic storms of acid")
      }
    }

    "omit mythic trait when empty" in {
      val noMythic = sampleLegendary.copy(MythicTrait = MythicTrait("", ""))
      renderer.renderLegendaryHtml(noMythic).asserting { html =>
        html should not include "Mythic Trait"
      }
    }

    "show dash for empty defense fields" in {
      val noDefenses = sampleLegendary.copy(
        DamageResistances = "",
        DamageImmunities = "",
        ConditionImmunities = ""
      )
      renderer.renderLegendaryHtml(noDefenses).asserting { html =>
        // empty fields render as "—"
        html should include("—")
      }
    }
  }

  "LegendaryCharacterSheet JSON format" should {
    "round-trip through JSON" in {
      val json   = Json.toJson(sampleLegendary)
      val parsed = json.as[LegendaryCharacterSheet]
      IO.pure(parsed).asserting(_ shouldBe sampleLegendary)
    }

    "parse the blank legendary template structure" in {
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
          |  "DamageResistances": "",
          |  "DamageImmunities": "",
          |  "ConditionImmunities": "",
          |  "Senses": "",
          |  "Languages": "",
          |  "ChallengeRating": "",
          |  "ProficiencyBonus": "",
          |  "SpecialTraits": {},
          |  "Actions": {},
          |  "BonusActions": {},
          |  "Reactions": {},
          |  "LegendaryTraits": {
          |    "Legendary Resistance (3/Day)": ""
          |  },
          |  "LegendaryActions": {
          |    "Legendary Action Uses": "3",
          |    "Options": {
          |      "Option1": "",
          |      "Option2": "",
          |      "Option3": ""
          |    }
          |  },
          |  "MythicTrait": {
          |    "Name": "",
          |    "Description": ""
          |  },
          |  "LairActions": {},
          |  "RegionalEffects": [],
          |  "Equipment": {
          |    "Armor": "",
          |    "Weapons": "",
          |    "Other": ""
          |  },
          |  "Notes": ""
          |}""".stripMargin
      )
      val sheet = templateJson.as[LegendaryCharacterSheet]
      IO.pure(sheet.LegendaryActions.legendaryActionUses).asserting(_ shouldBe "3")
    }
  }
