-- Passe la recherche geospatiale des mecaniciens de la formule de Haversine (calcul
-- manuel sur latitude/longitude, sans index) a PostGIS (colonne geography + index GIST).
CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE mechanics
    ADD COLUMN location geography(Point, 4326);

-- Retro-remplissage pour les mecaniciens qui ont deja une position (V13).
UPDATE mechanics
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX idx_mechanics_location ON mechanics USING GIST (location);

-- Le code applicatif continue de lire/ecrire latitude et longitude (aucun changement
-- cote entite/service/controller) : ce trigger tient "location" synchronisee en base
-- a chaque insertion ou mise a jour de la position.
CREATE OR REPLACE FUNCTION sync_mechanic_location() RETURNS trigger AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    ELSE
        NEW.location := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_mechanic_location
    BEFORE INSERT OR UPDATE OF latitude, longitude ON mechanics
    FOR EACH ROW
    EXECUTE FUNCTION sync_mechanic_location();

-- Les anciens index simples sur latitude/longitude (V13) sont remplaces par l'index
-- spatial GIST ci-dessus pour la recherche de proximite.
DROP INDEX IF EXISTS idx_mechanics_latitude;
DROP INDEX IF EXISTS idx_mechanics_longitude;
