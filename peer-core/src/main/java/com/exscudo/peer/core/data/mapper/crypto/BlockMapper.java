package com.exscudo.peer.core.data.mapper.crypto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.Constants;
import com.exscudo.peer.core.utils.Format;

/**
 * Convert block to Map
 */
class BlockMapper {

	public static Map<String, Object> convert(Block block) {

		ArrayList<String> idSet = new ArrayList<>();
		for (Transaction tx : block.getTransactions()) {
			idSet.add(Format.ID.transactionId(tx.getID()));
		}

		Collections.sort(idSet);

		Map<String, Object> map = new TreeMap<>();
		map.put(Constants.VERSION, block.getVersion());
		map.put(Constants.TIMESTAMP, block.getTimestamp());
		map.put(Constants.PREVIOUS_BLOCK, Format.ID.blockId(block.getPreviousBlock()));
		map.put(Constants.TRANSACTIONS, idSet);
		map.put(Constants.GENERATION_SIGNATURE, Format.convert(block.getGenerationSignature()));
		map.put(Constants.GENERATOR, Format.ID.accountId(block.getSenderID()));

		if (block.getVersion() >= 2) {
			map.put(Constants.SNAPSHOT, Format.convert(block.getSnapshot()));
		}

		return map;
	}

}
