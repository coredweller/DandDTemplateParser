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
