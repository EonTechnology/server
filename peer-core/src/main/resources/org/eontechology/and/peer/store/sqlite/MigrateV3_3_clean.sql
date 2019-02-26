CREATE TABLE "temp_transactions" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "id" integer NOT NULL,
  "type" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "deadline" integer NOT NULL,
  "sender_id" integer NOT NULL,
  "fee" integer NOT NULL,
  "reference_id" integer NOT NULL,
  "signature" text NOT NULL,
  "attachment" text NOT NULL,
  "block_id" integer NOT NULL,
  "height" integer NOT NULL,
  "version" integer NOT NULL,
  "tag" integer NOT NULL,
  "payer_id" integer NOT NULL,
  "confirmations" text NULL,
  "note" text NULL,
  "nested_transactions" text NULL
);

INSERT INTO "temp_transactions" ("row_id", "id", "type", "timestamp", "deadline", "sender_id", "fee", "reference_id", "signature", "attachment", "block_id", "height", "version", "tag", "confirmations", "note", "nested_transactions", "payer_id")
SELECT "row_id", "id", "type", "timestamp", "deadline", "sender_id", "fee", "reference_id", "signature", "attachment", "block_id", "height", "version", "tag", "confirmations", "note", "nested_transactions", "payer_id" FROM "transactions";

DROP TABLE "transactions";
ALTER TABLE "temp_transactions" RENAME TO "transactions";

CREATE UNIQUE INDEX "transactions_id_block_id_idx" ON "transactions" ("id", "block_id");
CREATE INDEX "transactions_id_idx" ON "transactions" ("id");
CREATE INDEX "transactions_tag_timestamp_idx" ON "transactions" ("tag", "timestamp");
CREATE INDEX "transactions_id_block_idx" ON "transactions" ("block_id");
