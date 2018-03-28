package com.exscudo.eon.api.explorer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.exscudo.eon.api.BacklogService;
import com.exscudo.eon.api.BlockService;
import com.exscudo.eon.api.TransactionService;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;

public class BlockchainExplorerService {
    private final BacklogService backlogService;
    private final TransactionService transactionService;
    private final BlockService blockService;

    public BlockchainExplorerService(BacklogService backlogService,
                                     TransactionService transactionService,
                                     BlockService blockService) {

        this.transactionService = transactionService;
        this.backlogService = backlogService;
        this.blockService = blockService;
    }

    public Collection<Transaction> getCommittedPage(String accountId,
                                                    int page) throws RemotePeerException, IOException {
        return transactionService.getByAccountId(accountId, page);
    }

    public Collection<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {
        return backlogService.getByAccountId(id);
    }

    public Transaction getRegTransaction(String accountId) throws RemotePeerException, IOException {
        return transactionService.getRegistration(accountId);
    }

    public List<Block> getLastBlocks() throws RemotePeerException, IOException {
        return blockService.getLastPage();
    }

    public List<Block> getLastBlocksFrom(int height) throws RemotePeerException, IOException {
        return blockService.getPage(height);
    }

    public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
        return blockService.getByHeight(height);
    }

    public Block getBlockById(String blockId) throws RemotePeerException, IOException {
        return blockService.getById(blockId);
    }

    public Collection<Transaction> getTrsByBlockId(String blockId) throws RemotePeerException, IOException {
        return transactionService.getByBlockId(blockId);
    }

    public Transaction getTransactionById(String id) throws RemotePeerException, IOException {
        // ATTENTION
        // TODO: Null may be returned even if the transaction exists (see BacklogCleaner)
        Transaction tx = backlogService.getById(id);
        if (tx == null) {
            return transactionService.getById(id);
        }
        return tx;
    }

    public Collection<Transaction> getLastUncommittedTrs() throws RemotePeerException, IOException {
        return backlogService.getLastPage();
    }
}
