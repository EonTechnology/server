package com.exscudo.peer.core.storage.utils;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
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

    public DbBlock(Block block) {

        this.setId(block.getID().getValue());
        this.setVersion(block.getVersion());
        this.setTimestamp(block.getTimestamp());
        this.setPreviousBlock(block.getPreviousBlock().getValue());
        this.setSenderID(block.getSenderID().getValue());
        this.setSignature(Format.convert(block.getSignature()));
        this.setHeight(block.getHeight());
        this.setGenerationSignature(Format.convert(block.getGenerationSignature()));
        this.setCumulativeDifficulty(block.getCumulativeDifficulty().toString());
        this.setSnapshot(block.getSnapshot());
        this.setTag(0);
    }

    public Block toBlock(final BlockHelper helper) {

        Block block = new LazyBlock(helper);
        block.setVersion(getVersion());
        block.setTimestamp(getTimestamp());
        block.setPreviousBlock(new BlockID(getPreviousBlock()));
        block.setGenerationSignature(Format.convert(getGenerationSignature()));
        block.setSenderID(new AccountID(getSenderID()));
        block.setSignature(Format.convert(getSignature()));
        block.setSnapshot(getSnapshot());
        block.setHeight(getHeight());
        block.setCumulativeDifficulty(new BigInteger(getCumulativeDifficulty()));
        return block;
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

    private static class LazyBlock extends Block {
        private final BlockHelper helper;
        private boolean txLoaded = false;

        private LazyBlock(BlockHelper helper) {
            super();
            this.helper = helper;
        }

        @Override
        public Collection<Transaction> getTransactions() {
            if (!txLoaded) {
                try {

                    List<Transaction> transactions = new ArrayList<>();
                    for (DbTransaction dbtx : helper.getTransactions(getID())) {
                        transactions.add(dbtx.toTransaction());
                    }

                    setTransactions(transactions);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            return super.getTransactions();
        }

        @Override
        public void setTransactions(Collection<Transaction> transactions) {
            txLoaded = true;
            super.setTransactions(transactions);
        }
    }
}
