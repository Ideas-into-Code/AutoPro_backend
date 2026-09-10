-- La colonne reviews.rating a été créée en SMALLINT (V11) alors que l'entité
-- JPA Review la mappe en Integer. Sur une base PostgreSQL neuve, Hibernate
-- `validate` échoue : « found int2, expecting integer ». On aligne la colonne
-- sur INTEGER.
--
-- Forme SQL standard `SET DATA TYPE` : acceptée par PostgreSQL comme par H2
-- (tests). Sans effet là où la colonne est déjà en integer. La contrainte
-- CHECK (rating BETWEEN 1 AND 5) est conservée.
ALTER TABLE reviews ALTER COLUMN rating SET DATA TYPE integer;
