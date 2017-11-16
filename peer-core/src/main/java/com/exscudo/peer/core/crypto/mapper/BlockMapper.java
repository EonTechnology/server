package com.exscudo.peer.core.crypto.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
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
		map.put("version", block.getVersion());
		map.put("timestamp", block.getTimestamp());
		map.put("prev", Format.ID.blockId(block.getPreviousBlock()));
		map.put("transactions", idSet);
		map.put("generationSignature", Format.convert(block.getGenerationSignature()));
		map.put("generator", Format.ID.accountId(block.getSenderID()));

		if (block.getVersion() >= 2) {
			map.put("snapshot", Format.convert(block.getSnapshot()));
		}

		return map;
	}
}
