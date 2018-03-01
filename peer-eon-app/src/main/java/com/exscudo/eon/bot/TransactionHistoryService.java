package com.exscudo.eon.bot;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.exscudo.eon.utils.TransactionHistoryHelper;
import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.AccountHelper;

/**
 * Account history service
 */
public class TransactionHistoryService {
    private final Storage storage;
    private final IBacklogService backlogService;
    private final IBlockchainService blockchainService;
    private final AccountHelper accountHelper;

    public TransactionHistoryService(Storage state,
                                     IBacklogService backlogService,
                                     IBlockchainService blockchainService) {
        this.storage = state;
        this.backlogService = backlogService;
        this.blockchainService = blockchainService;

        this.accountHelper = storage.getAccountHelper();
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
        return getCommittedPage(id, 0);
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
        try {
            return TransactionHistoryHelper.getCommittedPage(accountHelper, id, page);
        } catch (Exception e) {
            Loggers.error(TransactionHistoryService.class, e);
            throw new RemotePeerException(e);
        }
    }

    /**
     * Get uncommitted transactions
     *
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     * @see IBacklogService
     */
    public List<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {

        try {
            return TransactionHistoryHelper.getUncommitted(backlogService, id);
        } catch (Exception e) {
            Loggers.error(TransactionHistoryService.class, e);
            throw new RemotePeerException(e);
        }
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
        List<Block> items = new ArrayList<>();

        AccountID accID = TransactionHistoryHelper.getAccountId(id);

        Block lastBlock = blockchainService.getLastBlock();
        try {
            BlockID[] list = accountHelper.getCreatedBlockList(accID, lastBlock.getTimestamp() - (24 * 60 * 60));
            for (BlockID item : list) {
                Block b = accountHelper.getBlockHeader(item);

                if (b != null) {
                    // Clear transactions in block ti minimize package size
                    items.add(b);
                }
            }
        } catch (SQLException e) {
            Loggers.error(TransactionHistoryService.class, e);
            throw new RemotePeerException(e);
        }

        return items;
    }
}
