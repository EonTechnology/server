package com.exscudo.eon.app.api.bot;

import java.io.IOException;
import java.util.List;

import com.exscudo.eon.app.api.data.BlockHeader;
import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.backlog.services.BacklogService;
import com.exscudo.peer.core.blockchain.services.BlockService;
import com.exscudo.peer.core.blockchain.services.TransactionService;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;

/**
 * Account history service
 */
public class TransactionHistoryBotService {

    private final TransactionService transactionService;
    private final BlockService blockService;
    private final BacklogService backlogService;

    public TransactionHistoryBotService(BacklogService backlogService,
                                        TransactionService transactionService,
                                        BlockService blockService) {

        this.transactionService = transactionService;
        this.backlogService = backlogService;
        this.blockService = blockService;
    }

    /**
     * Get committed transactions
     *
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public List<Transaction> getCommitted(String id) throws RemotePeerException, IOException {
        return transactionService.getByAccountId(new AccountID(id), 0);
    }

    /**
     * Get committed transactions
     *
     * @param id   account ID
     * @param page page number
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public List<Transaction> getCommittedPage(String id, int page) throws RemotePeerException, IOException {
        return transactionService.getByAccountId(new AccountID(id), page);
    }

    /**
     * Get uncommitted transactions
     *
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     * @see IBacklog
     */
    public List<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {
        return backlogService.getForAccount(new AccountID(id));
    }

    /**
     * Get committed blocks
     *
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public List<BlockHeader> getSignedBlock(String id) throws RemotePeerException, IOException {
        return BlockHeader.fromBlockList(blockService.getByAccountId(new AccountID(id)));
    }
}
