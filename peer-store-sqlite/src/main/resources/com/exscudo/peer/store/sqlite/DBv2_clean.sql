--

DROP INDEX IF EXISTS "property_blockid";
DROP INDEX IF EXISTS "property_account";
DROP INDEX IF EXISTS "property_account_type";
DROP INDEX IF EXISTS "property_account_type_blockid";
DROP TABLE IF EXISTS "property";

DELETE FROM "settings" WHERE "name"='DB_UPDATE_STEP';
DELETE FROM "settings" WHERE "name"='FORK_BEGIN';
DELETE FROM "settings" WHERE "name"='FORK_END';

INSERT OR REPLACE INTO "forks" ("id", "begin", "end", "target_tx", "target_block") VALUES (1, '2017-10-04T12:00:00.00Z', '2017-11-15T12:00:00.00Z', '1', '1');
INSERT OR REPLACE INTO "forks" ("id", "begin", "end", "target_tx", "target_block") VALUES (2, '2017-11-15T12:00:00.00Z', '2017-12-15T12:00:00.00Z', '2', '2');

INSERT OR REPLACE INTO "settings" ("name", "value") VALUES ('DB_VERSION', '2');
