package org.eontechology.and.eon.app.utils.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eontechology.and.peer.core.common.Format;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.TransactionID;

/**
 * Convert Transaction to and from Map
 */
public class TransportTransactionMapper {

    /**
     * Convert transaction to map
     *
     * @param transaction transaction
     * @return Map
     */
    public static Map<String, Object> convert(Transaction transaction) {
        Map<String, Object> map = new TreeMap<>();

        map.put(Constants.ID, transaction.getID().toString());
        if (transaction.getSignature() != null) {
            map.put(Constants.SIGNATURE, Format.convert(transaction.getSignature()));
        }
        map.put(Constants.TYPE, transaction.getType());
        map.put(Constants.TIMESTAMP, transaction.getTimestamp());
        map.put(Constants.DEADLINE, transaction.getDeadline());
        map.put(Constants.FEE, transaction.getFee());
        map.put(Constants.VERSION, transaction.getVersion());
        if (transaction.getReference() != null) {
            map.put(Constants.REFERENCED_TRANSACTION, transaction.getReference().toString());
        }
        map.put(Constants.SENDER, transaction.getSenderID().toString());
        if (!transaction.getData().isEmpty()) {
            map.put(Constants.ATTACHMENT, transaction.getData());
        }
        if (transaction.getConfirmations() != null && !transaction.getConfirmations().isEmpty()) {
            map.put(Constants.CONFIRMATIONS, transaction.getConfirmations());
        }
        if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
            map.put(Constants.NOTE, transaction.getNote());
        }
        if (transaction.hasNestedTransactions()) {
            Map<String, Object> nestedTxMap = new HashMap<>();
            for (Map.Entry<String, Transaction> e : transaction.getNestedTransactions().entrySet()) {
                nestedTxMap.put(e.getKey(), TransportTransactionMapper.convert(e.getValue()));
            }
            map.put(Constants.NESTED_TRANSACTIONS, nestedTxMap);
        }
        if (transaction.getPayer() != null) {
            map.put(Constants.PAYER, transaction.getPayer().toString());
        }

        return map;
    }

    /**
     * Convert transaction from map
     *
     * @param map map
     * @return transaction
     */
    public static Transaction convert(Map<String, Object> map) {

        Transaction tx = new Transaction();

        int type = Integer.parseInt(map.get(Constants.TYPE).toString());
        int timestamp = Integer.parseInt(map.get(Constants.TIMESTAMP).toString());
        int deadline = Integer.parseInt(map.get(Constants.DEADLINE).toString());
        long fee = Long.parseLong(map.get(Constants.FEE).toString());
        TransactionID referencedTransaction = null;
        if (map.containsKey(Constants.REFERENCED_TRANSACTION)) {
            referencedTransaction = new TransactionID(map.get(Constants.REFERENCED_TRANSACTION).toString());
        }
        AccountID sender = new AccountID(map.get(Constants.SENDER).toString());
        byte[] signature = Format.convert(map.get(Constants.SIGNATURE).toString());

        int version = Integer.parseInt(map.get(Constants.VERSION).toString());

        AccountID payer = null;
        if (map.containsKey(Constants.PAYER)) {
            payer = new AccountID(map.get(Constants.PAYER).toString());
        }

        Map<String, Object> attachment = new HashMap<>();
        Object obj = map.get(Constants.ATTACHMENT);
        if (obj != null && obj instanceof Map) {
            for (Object k : ((Map<?, ?>) obj).keySet()) {
                Object v = ((Map<?, ?>) obj).get(k);
                if (v instanceof Number) {
                    attachment.put(k.toString(), ((Number) v).longValue());
                } else {
                    attachment.put(k.toString(), v.toString());
                }
            }
        }

        Map<String, Object> confirmations = null;
        Object cObj = map.get(Constants.CONFIRMATIONS);
        if (cObj != null && cObj instanceof Map) {
            confirmations = new HashMap<>();
            for (Object k : ((Map<?, ?>) cObj).keySet()) {
                Object v = ((Map<?, ?>) cObj).get(k);
                confirmations.put(k.toString(), v);
            }
        }

        String note = null;
        Object noteObj = map.get(Constants.NOTE);
        if (noteObj != null) {
            note = String.valueOf(noteObj);
        }

        Map<String, Transaction> nestedTransactions = null;
        Object nestedObj = map.get(Constants.NESTED_TRANSACTIONS);
        if (nestedObj != null && nestedObj instanceof Map) {
            nestedTransactions = new HashMap<>();
            for (Object k : ((Map<?, ?>) nestedObj).keySet()) {

                Object v = ((Map) nestedObj).get(k);
                Transaction nestedTx = TransportTransactionMapper.convert((Map<String, Object>) v);
                nestedTransactions.put(String.valueOf(k), nestedTx);
            }
        }

        tx.setType(type);
        tx.setVersion(version);
        tx.setTimestamp(timestamp);
        tx.setDeadline(deadline);
        tx.setReference(referencedTransaction);
        tx.setSenderID(sender);
        tx.setFee(fee);
        tx.setData(attachment);
        tx.setConfirmations(confirmations);
        tx.setSignature(signature);
        tx.setNote(note);
        tx.setNestedTransactions(nestedTransactions);
        tx.setPayer(payer);

        return tx;
    }
}
