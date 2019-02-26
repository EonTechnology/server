package org.eontechology.and.peer.core.blockchain.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "acc_references")
public class DbAccTransaction {

    @DatabaseField(columnName = "account_id", canBeNull = false)
    private long accountID;

    @DatabaseField(columnName = "transaction_id", canBeNull = false)
    private long transactionID;

    @DatabaseField(columnName = "block_id", canBeNull = false)
    private long blockID;

    @DatabaseField(columnName = "timestamp", canBeNull = false)
    private int timestamp;

    @DatabaseField(columnName = "tag", canBeNull = false)
    private int tag;

    public long getAccountID() {
        return accountID;
    }

    public void setAccountID(long accountID) {
        this.accountID = accountID;
    }

    public long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(long transactionID) {
        this.transactionID = transactionID;
    }

    public long getBlockID() {
        return blockID;
    }

    public void setBlockID(long blockID) {
        this.blockID = blockID;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }
}
