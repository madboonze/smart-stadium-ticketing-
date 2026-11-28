-- Module 6: seat inventory schema.
--
-- Every value column is TEXT or INTEGER on purpose. SQLite has dynamic typing and will
-- happily turn a NUMERIC money column into a floating point REAL, which loses precision
-- on prices. Prices are therefore stored as the exact BigDecimal string, timestamps as
-- ISO-8601 text, and enums as their Java name. The mapper converts in both directions,
-- so no part of this depends on how a particular JDBC driver or Hibernate dialect
-- handles decimals and dates.
--
-- Relationships are enforced here, in the database, with real foreign keys.

CREATE TABLE stadium (
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    city TEXT NOT NULL
);

CREATE TABLE stand (
    id         TEXT PRIMARY KEY,
    stadium_id TEXT    NOT NULL REFERENCES stadium (id) ON DELETE CASCADE,
    name       TEXT    NOT NULL,
    ordinal    INTEGER NOT NULL
);

CREATE INDEX idx_stand_stadium ON stand (stadium_id);

CREATE TABLE section (
    id         TEXT PRIMARY KEY,
    stand_id   TEXT    NOT NULL REFERENCES stand (id) ON DELETE CASCADE,
    code       TEXT    NOT NULL,
    name       TEXT    NOT NULL,
    category   TEXT    NOT NULL,
    base_price TEXT    NOT NULL,
    ordinal    INTEGER NOT NULL
);

CREATE INDEX idx_section_stand ON section (stand_id);

CREATE TABLE seat_row (
    id         TEXT PRIMARY KEY,
    section_id TEXT    NOT NULL REFERENCES section (id) ON DELETE CASCADE,
    label      TEXT    NOT NULL,
    ordinal    INTEGER NOT NULL
);

CREATE INDEX idx_seat_row_section ON seat_row (section_id);

-- A seat hangs off exactly one parent: a row when it belongs to a section, or a stand
-- directly when it was added with Stand.addSeat, which is how the Module 1 demo fixture
-- works. The CHECK makes the "exactly one" part impossible to get wrong.
CREATE TABLE seat (
    id         TEXT PRIMARY KEY,
    row_id     TEXT REFERENCES seat_row (id) ON DELETE CASCADE,
    stand_id   TEXT REFERENCES stand (id) ON DELETE CASCADE,
    label      TEXT    NOT NULL,
    category   TEXT    NOT NULL,
    base_price TEXT    NOT NULL,
    status     TEXT    NOT NULL,
    ordinal    INTEGER NOT NULL,
    CONSTRAINT seat_has_one_parent CHECK (
        (row_id IS NOT NULL AND stand_id IS NULL)
            OR (row_id IS NULL AND stand_id IS NOT NULL)
        )
);

CREATE INDEX idx_seat_row ON seat (row_id);
CREATE INDEX idx_seat_stand ON seat (stand_id);

-- Named football_match because MATCH is a reserved keyword in SQLite.
CREATE TABLE football_match (
    id         TEXT PRIMARY KEY,
    stadium_id TEXT NOT NULL REFERENCES stadium (id) ON DELETE CASCADE,
    home_team  TEXT NOT NULL,
    away_team  TEXT NOT NULL,
    kickoff    TEXT NOT NULL,
    status     TEXT NOT NULL
);

CREATE INDEX idx_match_stadium ON football_match (stadium_id);
