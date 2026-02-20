package domain

import play.api.libs.json.*

// PascalCase wire format for all macros in this file.
// Special cases preserve the original template keys that PascalCase alone cannot produce.
private given JsonConfiguration = JsonConfiguration(naming = JsonNaming {
  case "characterClass" => "Class"
  case "hp"             => "HP"
  case "ac"             => "AC"
  case s                => JsonNaming.PascalCase(s)
})

// ── Ability Score ──────────────────────────────────────────────
case class AbilityScore(score: Int, modifier: String)

object AbilityScore:
  given OFormat[AbilityScore] = Json.format[AbilityScore]

// ── Ability Scores block ───────────────────────────────────────
case class AbilityScores(
  strength:     AbilityScore,
  dexterity:    AbilityScore,
  constitution: AbilityScore,
  intelligence: AbilityScore,
  wisdom:       AbilityScore,
  charisma:     AbilityScore
)

object AbilityScores:
  given OFormat[AbilityScores] = Json.format[AbilityScores]

// ── Equipment ──────────────────────────────────────────────────
case class Equipment(armor: String, weapons: String, other: String)

object Equipment:
  given OFormat[Equipment] = Json.format[Equipment]

// ── Character Sheet (root model) ───────────────────────────────
case class CharacterSheet(
  characterName:  String,
  level:          Int,
  race:           String,
  characterClass: String,
  alignment:      String,
  hp:             String,
  ac:             Int,
  speed:          String,
  abilityScores:  AbilityScores,
  savingThrows:   Map[String, String],
  skills:         Map[String, String],
  senses:         String,
  languages:      String,
  specialTraits:  Map[String, String],
  actions:        Map[String, String],
  equipment:      Equipment,
  notes:          String
)

object CharacterSheet:
  given OFormat[CharacterSheet] = Json.format[CharacterSheet]

// ── Legendary Actions (nested structure) ───────────────────────
case class LegendaryActions(
  legendaryActionUses: String,
  options:             Map[String, String]
)

object LegendaryActions:
  given Format[LegendaryActions] = Format(
    Reads { json =>
      for
        uses    <- (json \ "Legendary Action Uses").validate[String]
        options <- (json \ "Options").validate[Map[String, String]]
      yield LegendaryActions(uses, options)
    },
    Writes { la =>
      Json.obj(
        "Legendary Action Uses" -> la.legendaryActionUses,
        "Options"               -> la.options
      )
    }
  )

// ── Mythic Trait ───────────────────────────────────────────────
case class MythicTrait(name: String, description: String)

object MythicTrait:
  given OFormat[MythicTrait] = Json.format[MythicTrait]

// ── Legendary Character Sheet (root model) ─────────────────────
case class LegendaryCharacterSheet(
  characterName:       String,
  level:               Int,
  race:                String,
  characterClass:      String,
  alignment:           String,
  hp:                  String,
  ac:                  Int,
  speed:               String,
  abilityScores:       AbilityScores,
  savingThrows:        Map[String, String],
  skills:              Map[String, String],
  damageResistances:   String,
  damageImmunities:    String,
  conditionImmunities: String,
  senses:              String,
  languages:           String,
  challengeRating:     String,
  proficiencyBonus:    String,
  specialTraits:       Map[String, String],
  actions:             Map[String, String],
  bonusActions:        Map[String, String],
  reactions:           Map[String, String],
  legendaryTraits:     Map[String, String],
  legendaryActions:    LegendaryActions,
  mythicTrait:         MythicTrait,
  lairActions:         Map[String, String],
  regionalEffects:     List[String],
  equipment:           Equipment,
  notes:               String
)

object LegendaryCharacterSheet:
  given OFormat[LegendaryCharacterSheet] = Json.format[LegendaryCharacterSheet]
