package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;

import com.exscudo.peer.core.blockchain.BlockchainService;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.Ledger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.InitializerJson;
import com.exscudo.peer.core.storage.Storage;
import org.junit.Before;
import org.junit.Test;

public class InitializerJsonTest {

    private Storage storage;

    @Before
    public void setUp() throws Exception {
        storage = Storage.create("jdbc:sqlite:file:memInitializerJsonTest?mode=memory&cache=shared",
                                 new InitializerJson("/genesis_eon.json"));
    }

    @Test
    public void testBlockIds() throws Exception {

        BlockchainService blockchainProvider = new BlockchainService(storage);

        Storage.Metadata metadata = storage.metadata();
        BlockID lastBlockId = metadata.getLastBlockID();
        BlockID genesisBlockId = metadata.getGenesisBlockID();

        assertEquals(blockchainProvider.getLastBlock().getID().getValue(), lastBlockId.getValue());
        assertEquals(blockchainProvider.getGenesisBlockID().getValue(), genesisBlockId.getValue());
    }

    @Test
    public void testBlockSnapshot() throws Exception {

        BlockchainService blockchainProvider = new BlockchainService(storage);
        Block lastBlock = blockchainProvider.getLastBlock();
        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        Ledger ledger = ledgerProvider.getLedger(lastBlock);

        assertEquals(lastBlock.getSnapshot(), ledger.getHash());
    }
}
