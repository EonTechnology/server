package com.exscudo.peer.core.ledger;

import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.ledger.tree.CachedNodeCollection;
import com.exscudo.peer.core.ledger.tree.ITreeNodeCollection;
import com.exscudo.peer.core.storage.Storage;

public class LedgerProvider {
    private final Storage storage;

    public LedgerProvider(Storage storage) {
        this.storage = storage;
    }

    /**
     * Returns the status of accounts corresponding to the specified block.
     *
     * @param block target block
     * @return {@code ILedger} object or null
     */
    public ILedger getLedger(Block block) {
        return getLedger(block.getSnapshot(), block.getTimestamp() + Constant.BLOCK_PERIOD);
    }

    public ILedger getLedger(String snapshot, int timestamp) {
        ITreeNodeCollection collection = new CachedNodeCollection(storage);
        return new CachedLedger(new Ledger(collection, snapshot, timestamp));
    }

    public Void addLedger(ILedger ledger) {
        return storage.callInTransaction(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                ledger.save();
                return null;
            }
        });
    }
}
