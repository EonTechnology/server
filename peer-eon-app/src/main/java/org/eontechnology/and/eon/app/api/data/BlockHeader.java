package org.eontechnology.and.eon.app.api.data;

import java.util.LinkedList;
import java.util.List;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;

public class BlockHeader {
    public String ID;
    public String signature;
    public String generator;
    public int height;
    public int timestamp;
    public long transactionsFee;
    public int transactionsCount;

    public static BlockHeader fromBlock(Block b) {
        BlockHeader h = new BlockHeader();
        h.ID = b.getID().toString();
        h.signature = Format.convert(b.getSignature());
        h.generator = b.getSenderID().toString();
        h.height = b.getHeight();
        h.timestamp = b.getTimestamp();

        h.transactionsFee = 0;
        h.transactionsCount = 0;

        if (b.getTransactions() != null) {
            for (Transaction tx : b.getTransactions()) {
                h.transactionsCount++;
                h.transactionsFee += tx.getFee();
            }
        }

        return h;
    }

    public static List<BlockHeader> fromBlockList(List<Block> blockSet) {
        List<BlockHeader> headSet = new LinkedList<>();

        for (Block block : blockSet) {
            headSet.add(fromBlock(block));
        }

        return headSet;
    }
}
