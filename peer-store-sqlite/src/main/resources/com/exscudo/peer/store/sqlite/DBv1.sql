-- 
CREATE TABLE IF NOT EXISTS "settings" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "name" text NOT NULL,
  "value" text NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "settings_name" ON "settings" ("name");
INSERT OR IGNORE INTO "settings" ("id", "name", "value") VALUES (1,	'DB_VERSION',	'1');

CREATE TABLE IF NOT EXISTS "transaction" (
  "rowid" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "id" integer NOT NULL,
  "type" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "deadline" integer NOT NULL,
  "sender" integer NOT NULL,
  "recipient" integer NOT NULL,
  "fee" integer NOT NULL,
  "referencedTransaction" integer NOT NULL,
  "signature" text NOT NULL,
  "attachment" text NOT NULL,
  "block" integer NOT NULL,
  "height" integer NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS "transaction_id" ON "transaction" ("id");
CREATE INDEX IF NOT EXISTS  "transaction_block" ON "transaction" ("block");
CREATE INDEX IF NOT EXISTS  "transaction_recipient" ON "transaction" ("recipient");
CREATE INDEX IF NOT EXISTS  "transaction_sender" ON "transaction" ("sender");

CREATE TABLE IF NOT EXISTS "block" (
  "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "version" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "previousBlock" integer NOT NULL,
  "generator" integer NOT NULL,
  "generationSignature" text NOT NULL,
  "blockSignature" text NOT NULL,
  "height" integer NOT NULL,
  "nextBlock" integer NOT NULL,
  "cumulativeDifficulty" text NOT NULL
);
CREATE INDEX IF NOT EXISTS "block_height" ON "block" ("height");

CREATE TABLE IF NOT EXISTS "property" (
  "rowid" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "type" integer NOT NULL,
  "account" integer NOT NULL,
  "value" text NOT NULL,
  "blockid" integer NOT NULL,
  "height" integer NOT NULL
);
CREATE INDEX IF NOT EXISTS  "property_blockid" ON "property" ("blockid");
CREATE INDEX IF NOT EXISTS  "property_account" ON "property" ("account");
CREATE INDEX IF NOT EXISTS  "property_account_type" ON "property" ("account", "type");
CREATE UNIQUE INDEX IF NOT EXISTS "property_account_type_blockid" ON "property" ("account", "type", "blockid");

DROP TABLE IF EXISTS "account";
CREATE TABLE "account" (
	"rowid" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
	"id" integer NOT NULL,
	"publicKey" text NOT NULL
);
DROP INDEX IF EXISTS "account_id";
CREATE INDEX "account_id" ON "account"("id"); 
--
