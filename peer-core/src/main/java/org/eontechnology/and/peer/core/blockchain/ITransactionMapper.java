package org.eontechnology.and.peer.core.blockchain;

import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

public interface ITransactionMapper {

    BlockID getBlockID(Transaction transaction, Blockchain currentBlockchain);

    void map(Block block);
}
