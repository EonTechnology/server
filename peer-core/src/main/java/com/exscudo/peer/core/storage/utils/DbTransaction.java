package com.exscudo.peer.core.storage.utils;

import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BaseIdentifier;
import com.exscudo.peer.core.data.identifier.TransactionID;
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

    @DatabaseField(columnName = "recipient_id", canBeNull = false)
    private long recipientID;

    @DatabaseField(columnName = "block_id", canBeNull = false, uniqueIndexName = "transactions_id_block_id_idx")
    private long blockID;

    @DatabaseField(columnName = "height", canBeNull = false)
    private int height = Integer.MAX_VALUE;

    @DatabaseField(columnName = "tag", canBeNull = false)
    private int tag;

    public DbTransaction() {

    }

    public DbTransaction(Transaction transaction) {

        this.setId(transaction.getID().getValue());
        this.setVersion(transaction.getVersion());
        this.setTimestamp(transaction.getTimestamp());
        this.setDeadline(transaction.getDeadline());
        this.setSenderID(transaction.getSenderID().getValue());
        this.setFee(transaction.getFee());
        this.setReference(getRefOrDefault(transaction.getReference()));
        this.setType(transaction.getType());
        this.setSignature(Format.convert(transaction.getSignature()));
        if (transaction.getData() != null) {
            Bencode bencode = new Bencode();
            byte[] encoded = bencode.encode(transaction.getData());
            this.setAttachment(new String(encoded, bencode.getCharset()));
        }
        if (transaction.getConfirmations() != null) {
            Bencode bencode = new Bencode();
            byte[] encoded = bencode.encode(transaction.getConfirmations());
            this.setConfirmations(new String(encoded, bencode.getCharset()));
        }

        // TODO : add method to Transaction class?
        AccountID recipientID = null;
        if (transaction.getData() != null && transaction.getData().containsKey("recipient")) {
            recipientID = new AccountID(transaction.getData().get("recipient").toString());
        }

        if (transaction.getType() == TransactionType.Registration) {
            String accountId = transaction.getData().keySet().iterator().next();
            recipientID = new AccountID(accountId);
        }
        this.setRecipientID(getRefOrDefault(recipientID));
    }

    public Transaction toTransaction() {

        Transaction tx = new Transaction();
        tx.setType(getType());
        tx.setVersion(getVersion());
        tx.setTimestamp(getTimestamp());
        tx.setDeadline(getDeadline());
        if (getReference() != 0L) {
            tx.setReference(new TransactionID(getReference()));
        }
        tx.setSenderID(new AccountID(getSenderID()));
        tx.setFee(getFee());
        tx.setSignature(Format.convert(getSignature()));

        Map<String, Object> data = null;
        String attachmentText = getAttachment();
        if (attachmentText != null && attachmentText.length() > 0) {
            Bencode bencode = new Bencode();
            data = bencode.decode(attachmentText.getBytes(), Type.DICTIONARY);
        }
        tx.setData(data);

        Map<String, Object> confirmations = null;
        String confirmationText = getConfirmations();
        if (confirmationText != null && confirmationText.length() > 0) {
            Bencode bencode = new Bencode();
            confirmations = bencode.decode(confirmationText.getBytes(), Type.DICTIONARY);
        }
        tx.setConfirmations(confirmations);

        return tx;
    }

    private long getRefOrDefault(BaseIdentifier id) {
        if (id == null) {
            return 0L;
        }
        return id.getValue();
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
     * Returns the recipient account ID
     *
     * @return
     */
    public long getRecipientID() {
        return recipientID;
    }

    /**
     * Sets the recipient account ID.
     *
     * @param recipientID
     */
    public void setRecipientID(long recipientID) {
        this.recipientID = recipientID;
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
}
