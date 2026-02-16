package domain

import play.api.libs.json.*

// ── Ability Score ──────────────────────────────────────────────
case class AbilityScore(Score: Int, Modifier: String)

object AbilityScore:
  given Format[AbilityScore] = Json.format[AbilityScore]

// ── Ability Scores block ───────────────────────────────────────
case class AbilityScores(
  Strength:     AbilityScore,
  Dexterity:    AbilityScore,
  Constitution: AbilityScore,
  Intelligence: AbilityScore,
  Wisdom:       AbilityScore,
  Charisma:     AbilityScore
)

object AbilityScores:
  given Format[AbilityScores] = Json.format[AbilityScores]

// ── Equipment ──────────────────────────────────────────────────
case class Equipment(Armor: String, Weapons: String, Other: String)

object Equipment:
  given Format[Equipment] = Json.format[Equipment]

// ── Character Sheet (root model) ───────────────────────────────
case class CharacterSheet(
  CharacterName: String,
  Level:         Int,
  Race:          String,
  Class:         String,
  Alignment:     String,
  HP:            String,
  AC:            Int,
  Speed:         String,
  AbilityScores: AbilityScores,
  SavingThrows:  Map[String, String],
  Skills:        Map[String, String],
  Senses:        String,
  Languages:     String,
  SpecialTraits: Map[String, String],
  Actions:       Map[String, String],
  Equipment:     Equipment,
  Notes:         String
)

object CharacterSheet:
  given Format[CharacterSheet] = Json.format[CharacterSheet]

// ── Legendary Actions (nested structure) ───────────────────────
case class LegendaryActions(
  legendaryActionUses: String,
  Options:             Map[String, String]
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
        "Options"               -> la.Options
      )
    }
  )

// ── Mythic Trait ───────────────────────────────────────────────
case class MythicTrait(Name: String, Description: String)

object MythicTrait:
  given Format[MythicTrait] = Json.format[MythicTrait]

// ── Legendary Character Sheet (root model) ─────────────────────
case class LegendaryCharacterSheet(
  CharacterName:       String,
  Level:               Int,
  Race:                String,
  Class:               String,
  Alignment:           String,
  HP:                  String,
  AC:                  Int,
  Speed:               String,
  AbilityScores:       AbilityScores,
  SavingThrows:        Map[String, String],
  Skills:              Map[String, String],
  DamageResistances:   String,
  DamageImmunities:    String,
  ConditionImmunities: String,
  Senses:              String,
  Languages:           String,
  ChallengeRating:     String,
  ProficiencyBonus:    String,
  SpecialTraits:       Map[String, String],
  Actions:             Map[String, String],
  BonusActions:        Map[String, String],
  Reactions:           Map[String, String],
  LegendaryTraits:     Map[String, String],
  LegendaryActions:    LegendaryActions,
  MythicTrait:         MythicTrait,
  LairActions:         Map[String, String],
  RegionalEffects:     List[String],
  Equipment:           Equipment,
  Notes:               String
)

object LegendaryCharacterSheet:
  given Format[LegendaryCharacterSheet] = Json.format[LegendaryCharacterSheet]
