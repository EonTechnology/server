package com.exscudo.peer.core.data.mapper.transport;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.Constants;
import com.exscudo.peer.core.utils.Format;

/**
 * Convert Block to and from Map
 */
public class BlockMapper {

	public static Map<String, Object> convert(Block block) {

		Map<String, Object> map = new TreeMap<>();
		map.put(Constants.VERSION, block.getVersion());
		map.put(Constants.TIMESTAMP, block.getTimestamp());
		map.put(Constants.PREVIOUS_BLOCK, Format.ID.blockId(block.getPreviousBlock()));
		map.put(Constants.GENERATOR, Format.ID.accountId(block.getSenderID()));
		map.put(Constants.GENERATION_SIGNATURE, Format.convert(block.getGenerationSignature()));
		map.put(Constants.SIGNATURE, Format.convert(block.getSignature()));
		map.put(Constants.HEIGHT, block.getHeight());
		map.put(Constants.CUMMULATIVE_DIFFICULTY, block.getCumulativeDifficulty().toString());

		Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
		if (transactions.length != 0) {

			ArrayList<Object> list = new ArrayList<Object>(transactions.length);
			for (Transaction tx : transactions) {
				list.add(TransactionMapper.convert(tx));
			}
			map.put(Constants.TRANSACTIONS, list);

		}

		if (block.getVersion() >= 2) {
			map.put(Constants.SNAPSHOT, Format.convert(block.getSnapshot()));
		}

		return map;

	}

	public static Block convert(Map<String, Object> map) {

		int version = Integer.parseInt(map.get(Constants.VERSION).toString());
		int timestamp = Integer.parseInt(map.get(Constants.TIMESTAMP).toString());
		long previousBlock = Format.ID.blockId(map.get(Constants.PREVIOUS_BLOCK).toString());
		long generator = Format.ID.accountId(map.get(Constants.GENERATOR).toString());
		byte[] generationSignature = Format.convert(map.get(Constants.GENERATION_SIGNATURE).toString());
		byte[] blockSignature = Format.convert(map.get(Constants.SIGNATURE).toString());
		BigInteger cumDif = new BigInteger(map.get(Constants.CUMMULATIVE_DIFFICULTY).toString());

		Block block = new Block();
		block.setVersion(version);
		block.setTimestamp(timestamp);
		block.setPreviousBlock(previousBlock);
		block.setGenerationSignature(generationSignature);
		block.setSenderID(generator);
		block.setSignature(blockSignature);
		block.setCumulativeDifficulty(cumDif);

		Object aObj = map.get(Constants.TRANSACTIONS);
		if (aObj != null) {

			List<Transaction> txMap = new ArrayList<>();
			if (!(aObj instanceof Iterable)) {
				throw new ClassCastException();
			}
			for (Object txObj : (Iterable) aObj) {
				Transaction tx = TransactionMapper.convert((Map<String, Object>) txObj);
				txMap.add(tx);
			}
			block.setTransactions(txMap);

		} else {
			block.setTransactions(new ArrayList<>());
		}

		if (block.getVersion() >= 2) {
			byte[] snapshot = Format.convert(map.get(Constants.SNAPSHOT).toString());
			block.setSnapshot(snapshot);
		}

		return block;

	}
}
