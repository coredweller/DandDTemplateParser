package services

import cats.effect.IO
import domain.*

final class CharacterSheetRenderer:

  def renderHtml(sheet: CharacterSheet): IO[String] = IO.pure {
    val abilities = renderAbilityScores(sheet.AbilityScores)
    val savingThrows = renderKeyValueSection("Saving Throws", sheet.SavingThrows)
    val skills = renderKeyValueSection("Skills", sheet.Skills)
    val specialTraits = renderKeyValueSection("Special Traits", sheet.SpecialTraits)
    val actions = renderKeyValueSection("Actions", sheet.Actions)

    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <title>${escapeHtml(sheet.CharacterName)} — Character Sheet</title>
       |  <style>
       |    ${css}
       |  </style>
       |</head>
       |<body>
       |  <div class="sheet">
       |    <header class="header">
       |      <h1 class="char-name">${escapeHtml(sheet.CharacterName)}</h1>
       |      <div class="header-details">
       |        <span class="badge">Level ${sheet.Level}</span>
       |        <span class="badge">${escapeHtml(sheet.Race)}</span>
       |        <span class="badge">${escapeHtml(sheet.Class)}</span>
       |        <span class="badge">${escapeHtml(sheet.Alignment)}</span>
       |      </div>
       |    </header>
       |
       |    <section class="combat-stats">
       |      <div class="stat-box">
       |        <div class="stat-label">HP</div>
       |        <div class="stat-value">${escapeHtml(sheet.HP)}</div>
       |      </div>
       |      <div class="stat-box">
       |        <div class="stat-label">AC</div>
       |        <div class="stat-value">${sheet.AC}</div>
       |      </div>
       |      <div class="stat-box">
       |        <div class="stat-label">Speed</div>
       |        <div class="stat-value">${escapeHtml(sheet.Speed)}</div>
       |      </div>
       |    </section>
       |
       |    <section class="abilities">
       |      <h2>Ability Scores</h2>
       |      <div class="ability-grid">
       |        ${abilities}
       |      </div>
       |    </section>
       |
       |    ${savingThrows}
       |    ${skills}
       |
       |    <section class="info-row">
       |      <div class="info-block">
       |        <h3>Senses</h3>
       |        <p>${escapeHtml(sheet.Senses)}</p>
       |      </div>
       |      <div class="info-block">
       |        <h3>Languages</h3>
       |        <p>${escapeHtml(sheet.Languages)}</p>
       |      </div>
       |    </section>
       |
       |    ${specialTraits}
       |    ${actions}
       |
       |    <section class="equipment">
       |      <h2>Equipment</h2>
       |      <div class="equipment-grid">
       |        <div class="equip-item">
       |          <h3>Armor</h3>
       |          <p>${escapeHtml(sheet.Equipment.Armor)}</p>
       |        </div>
       |        <div class="equip-item">
       |          <h3>Weapons</h3>
       |          <p>${escapeHtml(sheet.Equipment.Weapons)}</p>
       |        </div>
       |        <div class="equip-item">
       |          <h3>Other</h3>
       |          <p>${escapeHtml(sheet.Equipment.Other)}</p>
       |        </div>
       |      </div>
       |    </section>
       |
       |    ${renderNotes(sheet.Notes)}
       |  </div>
       |</body>
       |</html>""".stripMargin
  }

  private def renderAbilityScores(a: AbilityScores): String =
    val scores = List(
      ("STR", a.Strength),
      ("DEX", a.Dexterity),
      ("CON", a.Constitution),
      ("INT", a.Intelligence),
      ("WIS", a.Wisdom),
      ("CHA", a.Charisma)
    )
    scores.map { (label, ab) =>
      s"""<div class="ability-card">
         |  <div class="ability-label">$label</div>
         |  <div class="ability-score">${ab.Score}</div>
         |  <div class="ability-mod">${escapeHtml(ab.Modifier)}</div>
         |</div>""".stripMargin
    }.mkString("\n")

  private def renderKeyValueSection(title: String, entries: Map[String, String]): String =
    if entries.isEmpty then ""
    else
      val rows = entries.map { (k, v) =>
        s"""<tr><td class="kv-key">${escapeHtml(k)}</td><td class="kv-val">${escapeHtml(v)}</td></tr>"""
      }.mkString("\n")
      s"""<section class="kv-section">
         |  <h2>$title</h2>
         |  <table class="kv-table">$rows</table>
         |</section>""".stripMargin

  private def renderNotes(notes: String): String =
    if notes.isBlank then ""
    else
      s"""<section class="notes">
         |  <h2>Notes</h2>
         |  <p>${escapeHtml(notes)}</p>
         |</section>""".stripMargin

  private def escapeHtml(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")

  private val css: String =
    """* { margin: 0; padding: 0; box-sizing: border-box; }
      |body {
      |  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
      |  background: #1a1a2e; color: #e0e0e0; padding: 2rem;
      |}
      |.sheet {
      |  max-width: 800px; margin: 0 auto;
      |  background: #16213e; border-radius: 12px;
      |  padding: 2rem; box-shadow: 0 8px 32px rgba(0,0,0,0.4);
      |  border: 1px solid #0f3460;
      |}
      |.header { text-align: center; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 2px solid #e94560; }
      |.char-name { font-size: 2rem; color: #e94560; margin-bottom: 0.5rem; }
      |.header-details { display: flex; gap: 0.5rem; justify-content: center; flex-wrap: wrap; }
      |.badge {
      |  background: #0f3460; color: #e0e0e0; padding: 0.25rem 0.75rem;
      |  border-radius: 20px; font-size: 0.85rem; border: 1px solid #533483;
      |}
      |.combat-stats {
      |  display: flex; justify-content: center; gap: 2rem;
      |  margin-bottom: 1.5rem;
      |}
      |.stat-box {
      |  text-align: center; background: #0f3460; padding: 1rem 1.5rem;
      |  border-radius: 8px; min-width: 100px; border: 1px solid #533483;
      |}
      |.stat-label { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.1em; color: #aaa; }
      |.stat-value { font-size: 1.5rem; font-weight: 700; color: #e94560; }
      |h2 {
      |  font-size: 1.1rem; color: #e94560; text-transform: uppercase;
      |  letter-spacing: 0.05em; margin-bottom: 0.75rem;
      |  padding-bottom: 0.25rem; border-bottom: 1px solid #533483;
      |}
      |.abilities { margin-bottom: 1.5rem; }
      |.ability-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 0.5rem; }
      |.ability-card {
      |  text-align: center; background: #0f3460; padding: 0.75rem 0.25rem;
      |  border-radius: 8px; border: 1px solid #533483;
      |}
      |.ability-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.1em; color: #aaa; }
      |.ability-score { font-size: 1.5rem; font-weight: 700; color: #fff; }
      |.ability-mod { font-size: 0.9rem; color: #e94560; }
      |.kv-section { margin-bottom: 1.5rem; }
      |.kv-table { width: 100%; border-collapse: collapse; }
      |.kv-table tr { border-bottom: 1px solid #0f3460; }
      |.kv-key { padding: 0.4rem 0.5rem; font-weight: 600; width: 40%; color: #ccc; }
      |.kv-val { padding: 0.4rem 0.5rem; color: #e0e0e0; }
      |.info-row { display: flex; gap: 2rem; margin-bottom: 1.5rem; }
      |.info-block { flex: 1; }
      |.info-block h3 { font-size: 0.85rem; color: #aaa; text-transform: uppercase; margin-bottom: 0.25rem; }
      |.info-block p { color: #e0e0e0; }
      |.equipment { margin-bottom: 1.5rem; }
      |.equipment-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
      |.equip-item { background: #0f3460; padding: 0.75rem; border-radius: 8px; border: 1px solid #533483; }
      |.equip-item h3 { font-size: 0.75rem; color: #aaa; text-transform: uppercase; margin-bottom: 0.25rem; }
      |.equip-item p { color: #e0e0e0; }
      |.notes { margin-top: 1rem; }
      |.notes p { background: #0f3460; padding: 1rem; border-radius: 8px; border: 1px solid #533483; white-space: pre-wrap; }
      |@media (max-width: 600px) {
      |  .ability-grid { grid-template-columns: repeat(3, 1fr); }
      |  .equipment-grid { grid-template-columns: 1fr; }
      |  .combat-stats { flex-direction: column; align-items: center; }
      |  .info-row { flex-direction: column; gap: 1rem; }
      |}""".stripMargin
