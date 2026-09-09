-- Photo de profil (URL Cloudinary) et horaires d'ouverture (texte libre)
-- affichés sur la fiche publique du mécanicien.
ALTER TABLE mechanics
    ADD COLUMN photo_url     VARCHAR(500),
    ADD COLUMN opening_hours VARCHAR(500);
