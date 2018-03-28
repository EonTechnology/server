package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.exscudo.peer.core.InitializerJson;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.Storage;
import org.junit.Test;

public class InitializerJsonTest {

    private static final String connectURI = "jdbc:sqlite:file:memInitializerJsonTest?mode=memory&cache=shared";
    private static final String genesisJson = "/genesis_eon.json";

    @Test
    public void testBlockIds() throws Exception {
        Storage storage = Storage.create(connectURI, new InitializerJson(genesisJson, true));

        BlockchainProvider blockchainProvider = new BlockchainProvider(storage, null, null);

        Storage.Metadata metadata = storage.metadata();
        BlockID lastBlockId = metadata.getLastBlockID();
        BlockID genesisBlockId = metadata.getGenesisBlockID();

        assertEquals(blockchainProvider.getLastBlock().getID().getValue(), lastBlockId.getValue());
        assertEquals(blockchainProvider.getGenesisBlockID().getValue(), genesisBlockId.getValue());
    }

    @Test
    public void testBlockSnapshot() throws Exception {
        Storage storage = Storage.create(connectURI, new InitializerJson(genesisJson, true));

        BlockchainProvider blockchainProvider = new BlockchainProvider(storage, null, null);
        Block lastBlock = blockchainProvider.getLastBlock();
        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        ILedger ledger = ledgerProvider.getLedger(lastBlock);

        assertEquals(lastBlock.getSnapshot(), ledger.getHash());
    }

    @Test
    public void testSyncSnapshotDisable() throws Exception {
        String connectURI = "jdbc:sqlite:file:memInitializerJsonTest2?mode=memory&cache=shared";

        try {
            Storage s1 = Storage.create(connectURI, new InitializerJson(genesisJson, true));
            Storage s2 = Storage.create(connectURI, new InitializerJson(genesisJson, false));

            assertTrue("Snapshot mode switched", true);
        } catch (Exception ex) {
            assertTrue("Snapshot mode cannot be switched", false);
        }
    }

    @Test
    public void testSyncSnapshotEnable() throws Exception {
        String connectURI = "jdbc:sqlite:file:memInitializerJsonTest3?mode=memory&cache=shared";

        try {
            Storage s1 = Storage.create(connectURI, new InitializerJson(genesisJson, false));
            Storage s2 = Storage.create(connectURI, new InitializerJson(genesisJson, true));

            assertTrue("Snapshot mode switched", false);
        } catch (Exception ex) {
            assertTrue("Snapshot mode cannot be switched", true);
        }
    }
}
