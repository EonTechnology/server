package com.exscudo.peer.core.api.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exscudo.peer.core.api.ISnapshotSynchronizationService;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;

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
    public Map<String, Object> getAccounts(String blockID) throws RemotePeerException, IOException {

        BlockID id = new BlockID(blockID);
        Block block = blockchain.getBlock(id);
        ILedger ledger = ledgerProvider.getLedger(block);

        return getAccFromIterator(ledger.iterator());
    }

    @Override
    public Map<String, Object> getNextAccounts(String blockID,
                                               String accountID) throws RemotePeerException, IOException {

        BlockID id = new BlockID(blockID);
        Block block = blockchain.getBlock(id);
        ILedger ledger = ledgerProvider.getLedger(block);

        AccountID accID = new AccountID(accountID);
        if (ledger.getAccount(accID) != null) {
            return getAccFromIterator(ledger.iterator(accID));
        }

        return null;
    }

    private Map<String, Object> getAccFromIterator(Iterator<Account> iterator) {

        HashMap<String, Object> accSet = new HashMap<>();

        while (iterator.hasNext() && accSet.size() < ACCOUNT_LIMIT) {
            Account account = iterator.next();

            HashMap<String, Object> accData = new HashMap<>();
            for (AccountProperty property : account.getProperties()) {
                accData.put(property.getType(), property.getData());
            }

            accSet.put(account.getID().toString(), accData);
        }

        return accSet;
    }
}
