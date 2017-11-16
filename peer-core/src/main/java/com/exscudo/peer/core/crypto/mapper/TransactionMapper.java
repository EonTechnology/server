package com.exscudo.peer.core.crypto.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;

/**
 * Convert transaction to Map
 */
public class TransactionMapper {

	/**
	 * Convert transaction to map
	 *
	 * @param transaction
	 *            transaction
	 * @param forSign
	 *            remove EDS field needed
	 * @return Map
	 */
	public static Map<String, Object> convert(Transaction transaction, boolean forSign) {

		Map<String, Object> map = convert(transaction);

		if (forSign) {
			map.remove(Constants.SIGNATURE);
			map.put(Constants.NETWORK, Format.ID.blockId(ForkProvider.getInstance().getGenesisBlockID()));
			if (transaction.getVersion() == 1) {
				map.remove(Constants.VERSION);
			}
		}

		return map;
	}

	/**
	 * Convert transaction to map
	 *
	 * @param transaction
	 *            transaction
	 * @return Map
	 */
	public static Map<String, Object> convert(Transaction transaction) {
		Map<String, Object> map = new TreeMap<>();

		if (transaction.getSignature() != null) {
			map.put(Constants.SIGNATURE, Format.convert(transaction.getSignature()));
		}
		map.put(Constants.TYPE, transaction.getType());
		map.put(Constants.TIMESTAMP, transaction.getTimestamp());
		map.put(Constants.DEADLINE, transaction.getDeadline());
		map.put(Constants.FEE, transaction.getFee());
		map.put(Constants.VERSION, transaction.getVersion());
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

	/**
	 * Convert transaction from map
	 *
	 * @param map
	 *            map
	 * @return transaction
	 */
	public static Transaction convert(Map<String, Object> map) {

		Transaction tx = new Transaction();

		int type = Integer.parseInt(map.get(Constants.TYPE).toString());
		int timestamp = Integer.parseInt(map.get(Constants.TIMESTAMP).toString());
		int deadline = Integer.parseInt(map.get(Constants.DEADLINE).toString());
		long fee = Long.parseLong(map.get(Constants.FEE).toString());
		long referencedTransaction = 0L;
		if (map.containsKey(Constants.REFERENCED_TRANSACTION)) {
			referencedTransaction = Format.ID.transactionId(map.get(Constants.REFERENCED_TRANSACTION).toString());
		}
		long sender = Format.ID.accountId(map.get(Constants.SENDER).toString());
		byte[] signature = Format.convert(map.get(Constants.SIGNATURE).toString());

		int version = 1;
		if (map.containsKey(Constants.VERSION)) {
			version = Integer.parseInt(map.get(Constants.VERSION).toString());
		}

		Map<String, Object> attachment = null;
		Object obj = map.get(Constants.ATTACHMENT);
		if (obj != null && obj instanceof Map) {
			attachment = new HashMap<>();
			for (Object k : ((Map<?, ?>) obj).keySet()) {
				Object v = ((Map<?, ?>) obj).get(k);
				attachment.put(k.toString(), v);
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
		tx.setSignature(signature);

		return tx;
	}

	private static class Constants {

		public static final String TYPE = "type";
		public static final String TIMESTAMP = "timestamp";
		public static final String DEADLINE = "deadline";
		public static final String FEE = "fee";
		public static final String REFERENCED_TRANSACTION = "referencedTransaction";
		public static final String SENDER = "sender";
		public static final String SIGNATURE = "signature";
		public static final String ATTACHMENT = "attachment";
		public static final String NETWORK = "network";
		public static final String VERSION = "version";
	}
}