package com.exscudo.peer.core.blockchain.storage.converters;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BaseIdentifier;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.Storage;

/**
 * Entity to DTO converter
 */
public class DTOConverter {

    public static DbBlock convert(Block block) {

        DbBlock dbBlock = new DbBlock();

        dbBlock.setId(block.getID().getValue());
        dbBlock.setVersion(block.getVersion());
        dbBlock.setTimestamp(block.getTimestamp());
        dbBlock.setPreviousBlock(block.getPreviousBlock().getValue());
        dbBlock.setSenderID(block.getSenderID().getValue());
        dbBlock.setSignature(Format.convert(block.getSignature()));
        dbBlock.setHeight(block.getHeight());
        dbBlock.setGenerationSignature(Format.convert(block.getGenerationSignature()));
        dbBlock.setCumulativeDifficulty(block.getCumulativeDifficulty().toString());
        dbBlock.setSnapshot(block.getSnapshot());
        dbBlock.setTag(0);

        return dbBlock;
    }

    public static Block convert(DbBlock dbBlock, final Storage storage) {

        Block block = new LazyBlock(storage);
        block.setVersion(dbBlock.getVersion());
        block.setTimestamp(dbBlock.getTimestamp());
        block.setPreviousBlock(new BlockID(dbBlock.getPreviousBlock()));
        block.setGenerationSignature(Format.convert(dbBlock.getGenerationSignature()));
        block.setSenderID(new AccountID(dbBlock.getSenderID()));
        block.setSignature(Format.convert(dbBlock.getSignature()));
        block.setSnapshot(dbBlock.getSnapshot());
        block.setHeight(dbBlock.getHeight());
        block.setCumulativeDifficulty(new BigInteger(dbBlock.getCumulativeDifficulty()));
        return block;
    }

    public static DbTransaction convert(Transaction transaction) {

        DbTransaction dbTx = new DbTransaction();

        dbTx.setId(transaction.getID().getValue());
        dbTx.setVersion(transaction.getVersion());
        dbTx.setTimestamp(transaction.getTimestamp());
        dbTx.setDeadline(transaction.getDeadline());
        dbTx.setSenderID(transaction.getSenderID().getValue());
        dbTx.setFee(transaction.getFee());
        dbTx.setReference(BaseIdentifier.getValueOrRef(transaction.getReference()));
        dbTx.setType(transaction.getType());
        dbTx.setSignature(Format.convert(transaction.getSignature()));
        if (transaction.getData() != null) {
            Bencode bencode = new Bencode();
            byte[] encoded = bencode.encode(transaction.getData());
            dbTx.setAttachment(new String(encoded, bencode.getCharset()));
        }
        if (transaction.getConfirmations() != null) {
            Bencode bencode = new Bencode();
            byte[] encoded = bencode.encode(transaction.getConfirmations());
            dbTx.setConfirmations(new String(encoded, bencode.getCharset()));
        }
        dbTx.setNote(transaction.getNote());
        if (transaction.hasNestedTransactions()) {
            Bencode bencode = new Bencode();
            Map<String, Object> map = new HashMap<>();
            for (Transaction nestedTx : transaction.getNestedTransactions().values()) {
                map.put(nestedTx.getID().toString(), StorageTransactionMapper.convert(nestedTx));
            }
            byte[] encoded = bencode.encode(map);
            dbTx.setNestedTransactions(new String(encoded, bencode.getCharset()));
        }

        return dbTx;
    }

    public static Transaction convert(DbTransaction dbTransaction) {

        Transaction tx = new Transaction();
        tx.setVerifiedState();

        tx.setType(dbTransaction.getType());
        tx.setVersion(dbTransaction.getVersion());
        tx.setTimestamp(dbTransaction.getTimestamp());
        tx.setDeadline(dbTransaction.getDeadline());
        if (dbTransaction.getReference() != 0L) {
            tx.setReference(new TransactionID(dbTransaction.getReference()));
        }
        tx.setSenderID(new AccountID(dbTransaction.getSenderID()));
        tx.setFee(dbTransaction.getFee());
        tx.setSignature(Format.convert(dbTransaction.getSignature()));

        Map<String, Object> data = null;
        String attachmentText = dbTransaction.getAttachment();
        if (attachmentText != null && attachmentText.length() > 0) {
            Bencode bencode = new Bencode();
            data = bencode.decode(attachmentText.getBytes(), Type.DICTIONARY);
        }
        tx.setData(data);

        Map<String, Object> confirmations = null;
        String confirmationText = dbTransaction.getConfirmations();
        if (confirmationText != null && confirmationText.length() > 0) {
            Bencode bencode = new Bencode();
            confirmations = bencode.decode(confirmationText.getBytes(), Type.DICTIONARY);
        }
        tx.setConfirmations(confirmations);
        tx.setNote(dbTransaction.getNote());

        Map<String, Transaction> nestedTransaction = null;
        String nestedTransactionsText = dbTransaction.getNestedTransactions();
        if (nestedTransactionsText != null && nestedTransactionsText.length() > 0) {
            Bencode bencode = new Bencode();
            Map<String, Object> map = bencode.decode(nestedTransactionsText.getBytes(), Type.DICTIONARY);

            nestedTransaction = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Transaction nestedTx = StorageTransactionMapper.convert((Map<String, Object>) entry.getValue());
                nestedTx.setVerifiedState();
                nestedTransaction.put(entry.getKey(), nestedTx);
            }
        }
        tx.setNestedTransactions(nestedTransaction);
        return tx;
    }
}
