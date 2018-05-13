
CREATE TABLE "blocks" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "id" integer NOT NULL,
  "version" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "previous_block_id" integer NOT NULL,
  "sender_id" integer NOT NULL,
  "generation_signature" text NOT NULL,
  "signature" text NOT NULL,
  "height" integer NOT NULL,
  "cumulative_difficulty" text NOT NULL,
  "snapshot" text NOT NULL,
  "tag" integer NOT NULL);

CREATE INDEX "blocks_height_idx" ON "blocks" ("height");
CREATE INDEX "blocks_tag_timestamp_idx" ON "blocks" ("tag", "timestamp");
CREATE UNIQUE INDEX "blocks_id_idx" ON "blocks" ("id");


CREATE TABLE IF NOT EXISTS "nodes" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "key" text NOT NULL,
  "index" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "type" integer NOT NULL,
  "mask" integer NOT NULL,
  "mask_length" integer NOT NULL,
  "right_node_id" text,
  "left_node_id" text,
  "value" text,
  "color" integer NOT NULL DEFAULT 0
);

CREATE INDEX "nodes_index_idx" ON "nodes" ("index");
CREATE INDEX "nodes_color" ON "nodes" ("color");


CREATE TABLE "settings" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "name" text NOT NULL,
  "value" text NOT NULL
);

CREATE UNIQUE INDEX "settings_name_idx" ON "settings" ("name");


CREATE TABLE "transactions" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "id" integer NOT NULL,
  "type" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "deadline" integer NOT NULL,
  "sender_id" integer NOT NULL,
  "recipient_id" integer NOT NULL,
  "fee" integer NOT NULL,
  "reference_id" integer NOT NULL,
  "signature" text NOT NULL,
  "attachment" text NOT NULL,
  "block_id" integer NOT NULL,
  "height" integer NOT NULL,
  "version" integer NOT NULL,
  "tag" integer NOT NULL,
  "confirmations" text DEFAULT NULL,
  "note" text DEFAULT NULL,
  "nested_transactions" text DEFAULT NULL);

CREATE UNIQUE INDEX "transactions_id_block_id_idx" ON "transactions" ("id", "block_id");
CREATE INDEX "transactions_id_idx" ON "transactions" ("id");
CREATE INDEX "transactions_tag_timestamp_idx" ON "transactions" ("tag", "timestamp");
CREATE INDEX "transactions_sender_id_idx" ON "transactions" ("sender_id");
CREATE INDEX "transactions_recipient_id_idx" ON "transactions" ("recipient_id");
CREATE INDEX "transactions_id_block_idx" ON "transactions" ("block_id");


CREATE TABLE "nested_transactions" (
  "row_id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  "id" integer NOT NULL,
  "owner_id" integer NOT NULL,
  "block_id" integer NOT NULL,
  "height" integer NOT NULL);

