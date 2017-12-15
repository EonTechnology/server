--
ALTER TABLE "transaction" ADD COLUMN "confirmation" text NOT NULL DEFAULT '';
DROP TABLE IF EXISTS "forks";
DROP INDEX IF EXISTS "forks_id";
INSERT OR REPLACE INTO "settings" ("name", "value") VALUES ('DB_VERSION', '3');
--