package com.exscudo.peer.core.crypto.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;

/**
 * Convert block to Map
 */
class BlockMapper {

    public static Map<String, Object> convert(Block block, BlockID networkID) {

        ArrayList<String> idSet = new ArrayList<>();
        for (Transaction tx : block.getTransactions()) {
            idSet.add(tx.getID().toString());
        }

        Collections.sort(idSet);

        Map<String, Object> map = new TreeMap<>();
        map.put(Constants.VERSION, block.getVersion());
        map.put(Constants.TIMESTAMP, block.getTimestamp());
        map.put(Constants.PREVIOUS_BLOCK, block.getPreviousBlock().toString());
        map.put(Constants.TRANSACTIONS, idSet);
        map.put(Constants.GENERATION_SIGNATURE, Format.convert(block.getGenerationSignature()));
        map.put(Constants.GENERATOR, block.getSenderID().toString());
        map.put(Constants.SNAPSHOT, block.getSnapshot());
        map.put(Constants.NETWORK, networkID.toString());

        return map;
    }
}
