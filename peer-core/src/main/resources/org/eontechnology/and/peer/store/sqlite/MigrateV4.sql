update "acc_references" set "tag" = (select "tag" from "blocks" where "id" = "block_id");