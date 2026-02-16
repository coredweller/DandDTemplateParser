CREATE TABLE character_sheet_renders (
  id              VARCHAR(36)  NOT NULL PRIMARY KEY,
  name            VARCHAR(255) NOT NULL,
  sheet_type      VARCHAR(20)  NOT NULL,
  character_name  VARCHAR(255) NOT NULL,
  request_json    LONGTEXT     NOT NULL,
  response_html   LONGTEXT     NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_sheet_type (sheet_type),
  INDEX idx_character_name (character_name),
  INDEX idx_created_at (created_at)
);
