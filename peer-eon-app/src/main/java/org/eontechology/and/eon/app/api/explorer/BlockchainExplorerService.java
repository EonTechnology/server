package org.eontechology.and.eon.app.api.explorer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eontechology.and.eon.app.api.data.BlockHeader;
import org.eontechology.and.peer.core.backlog.services.BacklogService;
import org.eontechology.and.peer.core.blockchain.services.BlockService;
import org.eontechology.and.peer.core.blockchain.services.TransactionService;
import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.core.data.identifier.TransactionID;

public class BlockchainExplorerService {
    private static final int TRANSACTION_LAST_PAGE_SIZE = 10;

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

    public Collection<Transaction> getCommittedPage(String id, int page) throws RemotePeerException, IOException {
        return transactionService.getByAccountId(new AccountID(id), page);
    }

    public Collection<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {
        return backlogService.getForAccount(new AccountID(id));
    }

    public List<BlockHeader> getLastBlocks() throws RemotePeerException, IOException {
        return BlockHeader.fromBlockList(blockService.getLastPage());
    }

    public List<BlockHeader> getLastBlocksFrom(int height) throws RemotePeerException, IOException {
        return BlockHeader.fromBlockList(blockService.getPage(height));
    }

    public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
        return blockService.getByHeight(height);
    }

    public Block getBlockById(String blockId) throws RemotePeerException, IOException {
        return blockService.getById(new BlockID(blockId));
    }

    public Transaction getTransactionById(String id) throws RemotePeerException, IOException {

        TransactionID transactionID = new TransactionID(id);

        // ATTENTION: Null may be returned even if the transaction exists (see BacklogCleaner)
        Transaction tx = backlogService.get(transactionID);
        if (tx == null) {
            return transactionService.getById(transactionID);
        }
        return tx;
    }

    public Collection<Transaction> getLastUncommittedTrs() throws RemotePeerException, IOException {
        return backlogService.getLatest(TRANSACTION_LAST_PAGE_SIZE);
    }
}
