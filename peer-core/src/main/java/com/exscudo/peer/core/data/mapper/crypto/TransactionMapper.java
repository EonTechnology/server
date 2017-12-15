package com.exscudo.peer.core.data.mapper.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.Constants;
import com.exscudo.peer.core.utils.Format;

/**
 * Convert transaction to Map
 */
class TransactionMapper {

	/**
	 * Convert transaction to map
	 *
	 * @param transaction
	 *            transaction
	 * @return Map
	 */
	public static Map<String, Object> convert(Transaction transaction) {

		Map<String, Object> map = new TreeMap<>();

		map.put(Constants.TYPE, transaction.getType());
		map.put(Constants.TIMESTAMP, transaction.getTimestamp());
		map.put(Constants.DEADLINE, transaction.getDeadline());
		map.put(Constants.FEE, transaction.getFee());
		if (transaction.getVersion() > 1) {
			map.put(Constants.VERSION, transaction.getVersion());
		}
		if (transaction.getReference() != 0) {
			map.put(Constants.REFERENCED_TRANSACTION, Format.ID.transactionId(transaction.getReference()));
		}
		map.put(Constants.SENDER, Format.ID.accountId(transaction.getSenderID()));
		if (transaction.getData() != null) {
			map.put(Constants.ATTACHMENT, transaction.getData());
		} else {
			map.put(Constants.ATTACHMENT, new HashMap<>());
		}

		return map;
	}

}