package com.exscudo.peer.core.crypto.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.Constants;

/**
 * Convert transaction to Map
 */
class TransactionMapper {

    /**
     * Convert transaction to map
     *
     * @param transaction transaction
     * @return Map
     */
    public static Map<String, Object> convert(Transaction transaction) {

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

        return map;
    }
}