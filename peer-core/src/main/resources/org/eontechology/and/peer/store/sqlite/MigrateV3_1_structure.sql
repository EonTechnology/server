CREATE TABLE IF NOT EXISTS "acc_references" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "account_id" integer NOT NULL,
  "transaction_id" integer NOT NULL,
  "block_id" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "tag" integer NOT NULL
);

CREATE INDEX "acc_references_tag_account_idx" ON "acc_references" ("tag", "account_id");
CREATE INDEX "acc_references_timestamp_idx" ON "acc_references" ("timestamp");
CREATE INDEX "acc_references_block_idx" ON "acc_references" ("block_id");
CREATE INDEX "acc_references_transaction_idx" ON "acc_references" ("transaction_id");