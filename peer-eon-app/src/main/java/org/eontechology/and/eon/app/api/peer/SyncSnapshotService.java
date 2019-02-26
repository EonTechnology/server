package org.eontechology.and.eon.app.api.peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eontechology.and.peer.core.api.ISnapshotSynchronizationService;
import org.eontechology.and.peer.core.blockchain.BlockchainProvider;
import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.ledger.LedgerProvider;

/**
 * Basic implementation of the {@code IBlockSynchronizationService} interface
 */
public class SyncSnapshotService extends BaseService implements ISnapshotSynchronizationService {
    /**
     * The maximum number of account transmitted during synchronization.
     */
    public static final int ACCOUNT_LIMIT = 100;
    public static final int BLOCK_LIMIT = 300;

    private final BlockchainProvider blockchain;
    private final LedgerProvider ledgerProvider;

    public SyncSnapshotService(BlockchainProvider blockchain, LedgerProvider ledgerProvider) {
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
    }

    @Override
    public Block getLastBlock() throws RemotePeerException, IOException {
        return blockchain.getLastBlock();
    }

    @Override
    public Block getBlockByHeight(int height) throws RemotePeerException, IOException {

        return blockchain.getBlockByHeight(height);
    }

    @Override
    public Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException {

        List<Block> list = new LinkedList<>();
        for (int i = height; i < height + BLOCK_LIMIT; i++) {
            Block block = blockchain.getBlockByHeight(i);
            block.setTransactions(new LinkedList<>());
            list.add(block);
        }

        return list.toArray(new Block[0]);
    }

    @Override
    public Account[] getAccounts(String blockID) throws RemotePeerException, IOException {

        BlockID id = new BlockID(blockID);
        Block block = blockchain.getBlock(id);
        ILedger ledger = ledgerProvider.getLedger(block);

        return getAccounts(ledger.iterator());
    }

    @Override
    public Account[] getNextAccounts(String blockID, String accountID) throws RemotePeerException, IOException {

        BlockID id = new BlockID(blockID);
        Block block = blockchain.getBlock(id);
        ILedger ledger = ledgerProvider.getLedger(block);

        AccountID accID = new AccountID(accountID);
        if (ledger.getAccount(accID) != null) {
            return getAccounts(walkTo(ledger.iterator(), accID));
        }

        return null;
    }

    private Account[] getAccounts(Iterator<Account> iterator) {

        List<Account> accounts = new ArrayList<>();
        while (iterator.hasNext() && accounts.size() < ACCOUNT_LIMIT) {
            accounts.add(iterator.next());
        }
        return accounts.toArray(new Account[0]);
    }

    private Iterator<Account> walkTo(Iterator<Account> iterator, AccountID id) {
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            Account next = iterator.next();
            if (next.getID().equals(id)) {
                return iterator;
            }
        }
        return null;
    }
}
