 --
DROP TABLE IF EXISTS "account";
DROP INDEX IF EXISTS "account_id";
DROP TABLE IF EXISTS "nodes";
CREATE TABLE IF NOT EXISTS "nodes" (
	"rowid" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
	"key" text NOT NULL,
	"id" integer NOT NULL,
	"type" integer NOT NULL,
	"index" integer NOT NULL,
	"value" text NOT NULL
);
CREATE INDEX IF NOT EXISTS  "nodes_index" ON "nodes" ("index");
--CREATE UNIQUE INDEX IF NOT EXISTS  "nodes_key" ON "nodes" ("key");

ALTER TABLE "block" ADD COLUMN "snapshot" text NOT NULL DEFAULT '';
ALTER TABLE "transaction" ADD COLUMN "version" integer NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS "forks" (
    "rowid" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
    "id" integer NOT NULL,
    "begin" text NOT NULL,
    "end" text NOT NULL,
    "target_tx" text NOT NULL,
    "target_block" text NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS  "forks_id" ON "forks" ("id");

INSERT OR IGNORE INTO "settings" ("name", "value") VALUES ('DB_UPDATE_STEP', '1');

