ALTER TABLE mechanics
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

CREATE INDEX idx_mechanics_latitude  ON mechanics (latitude);
CREATE INDEX idx_mechanics_longitude ON mechanics (longitude);