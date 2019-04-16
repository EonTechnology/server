package org.eontechnology.and.peer.core.blockchain.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "transactions")
public class DbTransaction {

    @DatabaseField(columnName = "id", canBeNull = false, uniqueIndexName = "transactions_id_block_id_idx")
    private long id;

    @DatabaseField(columnName = "signature", canBeNull = false)
    private String signature;

    @DatabaseField(columnName = "sender_id", canBeNull = false)
    private long senderID;

    @DatabaseField(columnName = "timestamp", canBeNull = false)
    private int timestamp;

    @DatabaseField(columnName = "version", canBeNull = false)
    private int version;

    @DatabaseField(columnName = "type", canBeNull = false)
    private int type;

    @DatabaseField(columnName = "deadline", canBeNull = false)
    private int deadline;

    @DatabaseField(columnName = "fee", canBeNull = false)
    private long fee;

    @DatabaseField(columnName = "reference_id", canBeNull = false)
    private long reference;

    @DatabaseField(columnName = "attachment", canBeNull = false)
    private String attachment;

    @DatabaseField(columnName = "confirmations")
    private String confirmations;

    @DatabaseField(columnName = "block_id", canBeNull = false, uniqueIndexName = "transactions_id_block_id_idx")
    private long blockID;

    @DatabaseField(columnName = "height", canBeNull = false)
    private int height = Integer.MAX_VALUE;

    @DatabaseField(columnName = "tag", canBeNull = false)
    private int tag;

    @DatabaseField(columnName = "note")
    private String note;

    @DatabaseField(columnName = "nested_transactions")
    private String nestedTransactions;

    @DatabaseField(columnName = "payer_id", canBeNull = false)
    private long payerID;

    public DbTransaction() {

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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getReference() {
        return reference;
    }

    public void setReference(long reference) {
        this.reference = reference;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(String confirmations) {
        this.confirmations = confirmations;
    }

    /**
     * Returns the block height.
     *
     * @return
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the block height where a transaction was placed.
     *
     * @param height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns block ID where a transaction was placed.
     *
     * @return
     */
    public long getBlockID() {
        return blockID;
    }

    /**
     * Sets the block ID where a transaction was placed.
     *
     * @param blockID
     */
    public void setBlockID(long blockID) {
        this.blockID = blockID;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNestedTransactions() {
        return nestedTransactions;
    }

    public void setNestedTransactions(String nestedTransactions) {
        this.nestedTransactions = nestedTransactions;
    }

    public long getPayerID() {
        return payerID;
    }

    public void setPayerID(long payerID) {
        this.payerID = payerID;
    }
}
