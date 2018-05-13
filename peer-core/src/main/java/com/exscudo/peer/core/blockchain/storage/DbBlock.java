package com.exscudo.peer.core.blockchain.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "blocks")
public class DbBlock {

    @DatabaseField(columnName = "id", canBeNull = false)
    private long id;

    @DatabaseField(columnName = "signature", canBeNull = false)
    private String signature;

    @DatabaseField(columnName = "sender_id", canBeNull = false)
    private long senderID;

    @DatabaseField(columnName = "timestamp", canBeNull = false)
    private int timestamp;

    @DatabaseField(columnName = "version", canBeNull = false)
    private int version;

    @DatabaseField(columnName = "previous_block_id", canBeNull = false)
    private long previousBlock;

    @DatabaseField(columnName = "generation_signature", canBeNull = false)
    private String generationSignature;

    @DatabaseField(columnName = "snapshot", canBeNull = false)
    private String snapshot;

    @DatabaseField(columnName = "height", canBeNull = false)
    private int height = Integer.MAX_VALUE;

    @DatabaseField(columnName = "tag", canBeNull = false)
    private int tag;

    @DatabaseField(columnName = "cumulative_difficulty", canBeNull = false)
    private String cumulativeDifficulty;

    public DbBlock() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getSenderID() {
        return senderID;
    }

    public void setSenderID(long senderID) {
        this.senderID = senderID;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getPreviousBlock() {
        return previousBlock;
    }

    public void setPreviousBlock(long previousBlock) {
        this.previousBlock = previousBlock;
    }

    public String getGenerationSignature() {
        return generationSignature;
    }

    public void setGenerationSignature(String generationSignature) {
        this.generationSignature = generationSignature;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    public void setCumulativeDifficulty(String cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }
}
