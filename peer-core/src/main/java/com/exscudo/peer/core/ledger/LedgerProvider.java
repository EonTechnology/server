package com.exscudo.peer.core.ledger;

import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.ledger.storage.DbNodeCache;
import com.exscudo.peer.core.storage.CacheManager;
import com.exscudo.peer.core.storage.Storage;

public class LedgerProvider {
    private final Storage storage;

    public LedgerProvider(Storage storage) {
        this.storage = storage;
        CacheManager.createCache(storage.getConnectionSource(), DbNodeCache.class);
    }

    /**
     * Returns the status of accounts corresponding to the specified block.
     *
     * @param block target block
     * @return {@code ILedger} object or null
     */
    public ILedger getLedger(Block block) {
        return new CachedLedger(new Ledger(storage.getConnectionSource(),
                                           block.getSnapshot(),
                                           block.getTimestamp() + Constant.BLOCK_PERIOD));
    }

    public void addLedger(ILedger ledger) {
        storage.callInTransaction(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                ledger.save();
                return null;
            }
        });
    }
}
