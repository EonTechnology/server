--
ALTER TABLE "transactions" ADD COLUMN "note" text DEFAULT NULL;
CREATE INDEX "nodes_color" ON "nodes" ("color");
--