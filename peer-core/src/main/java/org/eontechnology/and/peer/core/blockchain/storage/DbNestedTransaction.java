package org.eontechnology.and.peer.core.blockchain.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "nested_transactions")
public class DbNestedTransaction {

    @DatabaseField(columnName = "id", canBeNull = false)
    private long id;

    @DatabaseField(columnName = "owner_id", canBeNull = false)
    private long ownerID;

    @DatabaseField(columnName = "block_id", canBeNull = false)
    private long blockID;

    @DatabaseField(columnName = "height", canBeNull = false)
    private int height = Integer.MIN_VALUE;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBlockID() {
        return blockID;
    }

    public void setBlockID(long blockID) {
        this.blockID = blockID;
    }

    public long getOwnerID() {
        return this.ownerID;
    }

    public void setOwnerID(long ownerID) {
        this.ownerID = ownerID;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
