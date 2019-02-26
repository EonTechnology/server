package org.eontechology.and.peer.core.blockchain;

import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.BlockID;

public interface ITransactionMapper {

    BlockID getBlockID(Transaction transaction, Blockchain currentBlockchain);

    void map(Block block);
}
