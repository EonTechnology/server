ALTER TABLE "transactions" ADD "payer_id" integer NOT NULL DEFAULT '0';
CREATE INDEX "transactions_id_payer_idx" ON "transactions" ("payer_id");
