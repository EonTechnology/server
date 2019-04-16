package org.eontechnology.and.peer.core.crypto.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

/**
 * Convert transaction to Map
 */
class CryptoTransactionMapper {

    /**
     * Convert transaction to map
     *
     * @param transaction transaction
     * @return Map
     */
    public static Map<String, Object> convert(Transaction transaction, BlockID networkID) {

        Map<String, Object> map = new TreeMap<>();

        map.put(Constants.TYPE, transaction.getType());
        map.put(Constants.TIMESTAMP, transaction.getTimestamp());
        map.put(Constants.DEADLINE, transaction.getDeadline());
        map.put(Constants.FEE, transaction.getFee());
        map.put(Constants.VERSION, transaction.getVersion());

        if (transaction.getReference() != null) {
            map.put(Constants.REFERENCED_TRANSACTION, transaction.getReference().toString());
        }
        map.put(Constants.SENDER, transaction.getSenderID().toString());
        if (transaction.getData() != null) {
            map.put(Constants.ATTACHMENT, transaction.getData());
        } else {
            map.put(Constants.ATTACHMENT, new HashMap<>());
        }

        if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
            map.put(Constants.NOTE, transaction.getNote());
        }

        if (transaction.hasNestedTransactions()) {

            Map<String, Object> nestedTxMap = new HashMap<>();
            for (Map.Entry<String, Transaction> e : transaction.getNestedTransactions().entrySet()) {
                nestedTxMap.put(e.getKey(), convert(e.getValue(), networkID));
            }
            map.put(Constants.NESTED_TRANSACTIONS, nestedTxMap);
        }
        map.put(Constants.NETWORK, networkID.toString());
        if (transaction.getPayer() != null) {
            map.put(Constants.PAYER, transaction.getPayer().toString());
        }

        return map;
    }
}