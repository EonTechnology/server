package com.exscudo.eon.explorer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import com.exscudo.eon.utils.TransactionHistoryHelper;
import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.AccountHelper;
import com.exscudo.peer.core.storage.utils.BlockHelper;
import com.exscudo.peer.core.storage.utils.BlockchainHelper;
import com.exscudo.peer.core.storage.utils.TransactionHelper;

/**
 * Account history service
 */
public class BlockchainExplorerService {
    private final static int LAST_BLOCK_CNT = 10;
    private final static int LAST_UNC_TRS_CNT = 10;
    private final ExecutionContext context;
    private final AccountHelper accountHelper;
    private final BlockHelper blockHelper;
    private final BlockchainHelper blockchainHelper;
    private final TransactionHelper transactionHelper;
    private final Storage storage;
    private final IBacklogService backlogService;
    private final IBlockchainService blockchainService;
    private final Comparator<Block> cmprBlockDesc = new Comparator<Block>() {
        @Override
        public int compare(Block b1, Block b2) {
            return Integer.compare(b2.getHeight(), b1.getHeight());
        }
    };
    private final Comparator<Transaction> cmprUncTrs = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            return Long.compare(o2.getTimestamp(), o1.getTimestamp());
        }
    };

    public BlockchainExplorerService(Storage storage,
                                     ExecutionContext context,
                                     IBacklogService backlogService,
                                     IBlockchainService blockchainService) {
        this.context = context;
        this.storage = storage;
        this.backlogService = backlogService;
        this.blockchainService = blockchainService;
        this.accountHelper = storage.getAccountHelper();
        this.blockHelper = storage.getBlockHelper();
        this.blockchainHelper = storage.getBlockchainHelper();
        this.transactionHelper = storage.getTransactionHelper();
    }

    public Collection<Transaction> getCommittedPage(String accountId,
                                                    int page) throws RemotePeerException, IOException {
        try {
            return TransactionHistoryHelper.getCommittedPage(accountHelper, accountId, page);
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }
    }

    public Collection<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {
        return TransactionHistoryHelper.getUncommitted(backlogService, id);
    }

    /**
     * Get transaction of account registration
     *
     * @param id account ID
     * @return Transaction of account registration if exists, else null
     * @throws RemotePeerException
     * @throws IOException
     */
    public Transaction getRegTransaction(String id) throws RemotePeerException, IOException {
        AccountID accId = getAccountId(id);
        try {
            return accountHelper.findRegTransaction(accId).toTransaction();
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }
    }

    public List<Block> getLastBlocks() throws RemotePeerException, IOException {
        Block lastBlock = blockchainService.getLastBlock();
        return getLastBlocksFrom(lastBlock.getHeight());
    }

    public List<Block> getLastBlocksFrom(int height) throws RemotePeerException, IOException {
        int end = height;
        int begin = height - LAST_BLOCK_CNT + 1;
        if (begin < 0) {
            begin = 0;
        }
        if (end < 0) {
            end = 0;
        }

        ArrayList<Block> blocks = new ArrayList<>();
        try {
            BlockID[] blocIds = blockchainHelper.getBlockLinkedList(begin, end);
            for (BlockID blocId : blocIds) {
                Block block = blockHelper.get(blocId);
                // block.setTransactions(new LinkedList<>());
                blocks.add(block);
            }
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }

        blocks.sort(cmprBlockDesc);
        return blocks;
    }

    public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
        try {
            Block block = blockchainHelper.getByHeight(height);
            block.setTransactions(new LinkedList<>());
            return block;
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }
    }

    public Block getBlockById(String blockId) throws RemotePeerException, IOException {
        BlockID id = getBlockId(blockId);
        try {
            Block block = blockHelper.get(id);
            block.setTransactions(new LinkedList<>());
            return block;
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }
    }

    public Collection<Transaction> getTrsByBlockId(String blockId) throws RemotePeerException, IOException {
        BlockID id = getBlockId(blockId);

        try {
            Block block = blockHelper.get(id);
            return block.getTransactions();
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }
    }

    public Transaction getTransactionById(String trId) throws RemotePeerException, IOException {
        TransactionID id = getTrId(trId);
        Transaction tr = null;

        try {
            tr = transactionHelper.get(id);
            if (tr != null) {
                return tr;
            }
        } catch (SQLException e) {
            Loggers.error(BlockchainExplorerService.class, e);
            throw new RemotePeerException();
        }

        tr = backlogService.get(id);
        if (tr != null) {
            return tr;
        }
        return null;
    }

    public Collection<Transaction> getLastUncommittedTrs() throws RemotePeerException, IOException {
        TreeSet<Transaction> lastUncTrs = new TreeSet<>(cmprUncTrs);

        IBacklogService backlog = backlogService;
        for (TransactionID id : backlog) {
            Transaction tr = backlog.get(id);
            if (tr == null) {
                continue;
            }
            lastUncTrs.add(tr);
            if (lastUncTrs.size() > LAST_UNC_TRS_CNT) {
                lastUncTrs.pollLast();
            }
        }
        return lastUncTrs;
    }

    private AccountID getAccountId(String accountId) throws RemotePeerException {
        try {
            return new AccountID(accountId);
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        }
    }

    private BlockID getBlockId(String blockId) throws RemotePeerException {
        try {
            return new BlockID(blockId);
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        }
    }

    private TransactionID getTrId(String trId) throws RemotePeerException {
        try {
            return new TransactionID(trId);
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        }
    }
}
