package com.exscudo.peer.core.blockchain;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;

public interface ITransactionMapper {

    BlockID getBlockID(Transaction transaction, Blockchain currentBlockchain);

    void map(Block block);
}
