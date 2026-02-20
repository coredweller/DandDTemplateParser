ALTER TABLE character_sheet_renders
  DROP INDEX idx_character_name,
  ADD CONSTRAINT uq_character_name UNIQUE (character_name);
