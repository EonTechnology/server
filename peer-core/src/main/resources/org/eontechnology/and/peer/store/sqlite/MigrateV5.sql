DROP INDEX "blocks_height_idx";
CREATE INDEX "blocks_height_tag_idx" ON "blocks" ("height", "tag");
