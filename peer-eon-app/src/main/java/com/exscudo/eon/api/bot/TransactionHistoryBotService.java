package com.exscudo.eon.api.bot;

import java.io.IOException;
import java.util.List;

import com.exscudo.eon.api.BacklogService;
import com.exscudo.eon.api.BlockService;
import com.exscudo.eon.api.TransactionService;
import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;

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
        return transactionService.getByAccountId(id, 0);
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
        return transactionService.getByAccountId(id, page);
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
        return backlogService.getByAccountId(id);
    }

    /**
     * Get committed blocks
     *
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public List<Block> getSignedBlock(String id) throws RemotePeerException, IOException {
        return blockService.getByAccountId(id);
    }
}
