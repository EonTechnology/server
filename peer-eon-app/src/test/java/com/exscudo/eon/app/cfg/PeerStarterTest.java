package com.exscudo.eon.app.cfg;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.tx.TransactionType;
import org.junit.Assert;
import org.junit.Test;

public class PeerStarterTest {
    private static final String connectURI = "jdbc:sqlite:file:memInitializerJsonTest?mode=memory&cache=shared";
    private static final String genesisJson = "eon_network/dev/genesis_block.json";

    @Test
    public void testBlockIds() throws Exception {
        PeerStarter peerStarter = create(connectURI, genesisJson, true);
        Storage storage = peerStarter.getStorage();

        BlockchainProvider blockchainProvider = new BlockchainProvider(storage, null);

        Storage.Metadata metadata = storage.metadata();
        BlockID lastBlockId = metadata.getLastBlockID();
        BlockID genesisBlockId = metadata.getGenesisBlockID();

        assertEquals(blockchainProvider.getLastBlock().getID().getValue(), lastBlockId.getValue());
        assertNotNull(blockchainProvider.getBlock(genesisBlockId));
    }

    @Test
    public void testBlockSnapshot() throws Exception {

        PeerStarter peerStarter = create(connectURI, genesisJson, true);

        Storage storage = peerStarter.getStorage();

        BlockchainProvider blockchainProvider = new BlockchainProvider(storage, null);
        Block lastBlock = blockchainProvider.getLastBlock();
        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        ILedger ledger = ledgerProvider.getLedger(lastBlock);

        assertEquals(lastBlock.getSnapshot(), ledger.getHash());
    }

    @Test
    public void testSyncSnapshotDisable() throws Exception {
        String connectURI = "jdbc:sqlite:file:memInitializerJsonTest2?mode=memory&cache=shared";

        try {

            PeerStarter ps1 = create(connectURI, genesisJson, true);
            PeerStarter ps2 = create(connectURI, genesisJson, false);

            assertTrue("Snapshot mode switched", true);
        } catch (Exception ex) {
            assertTrue("Snapshot mode cannot be switched", false);
        }
    }

    @Test
    public void testSyncSnapshotEnable() throws Exception {
        String connectURI = "jdbc:sqlite:file:memInitializerJsonTest3?mode=memory&cache=shared";

        try {

            PeerStarter ps1 = create(connectURI, genesisJson, false);
            PeerStarter ps2 = create(connectURI, genesisJson, true);

            assertTrue("Snapshot mode switched", false);
        } catch (Exception ex) {
            assertTrue("Snapshot mode cannot be switched", true);
        }
    }

    @Test
    public void mainBlockIds() throws Exception {
        String connectURI = "jdbc:sqlite:file:memInitializerJsonTest4?mode=memory&cache=shared";
        PeerStarter peerStarter = create(connectURI, "eon_network/main/genesis_block.json", true);
        Storage storage = peerStarter.getStorage();

        BlockchainProvider blockchainProvider = new BlockchainProvider(storage, null);

        Storage.Metadata metadata = storage.metadata();
        BlockID lastBlockId = metadata.getLastBlockID();
        BlockID genesisBlockId = metadata.getGenesisBlockID();

        assertEquals(blockchainProvider.getLastBlock().getID().getValue(), lastBlockId.getValue());
        assertNotNull(blockchainProvider.getBlock(genesisBlockId));
    }

    @Test
    public void forksTest_OK() throws Exception {
        String connectURI = "jdbc:sqlite:file:memInitializerJsonTest5?mode=memory&cache=shared";
        Config config = createConfig(connectURI, "./com/exscudo/eon/app/IT/genesis_block.json", true);
        config.setForksFile("./com/exscudo/eon/app/IT/forks.json");
        PeerStarter peerStarter = new PeerStarter(config);

        IFork fork = peerStarter.getFork();

        int time = getTime("2018-01-01T12:01:00.00Z");
        Assert.assertTrue(fork.getTransactionTypes(time).contains(TransactionType.Payment));
        Assert.assertTrue(fork.getTransactionTypes(time).contains(TransactionType.Registration));
        Assert.assertFalse(fork.getTransactionTypes(time).contains(TransactionType.Quorum));

        time = getTime("2018-02-01T12:01:00.00Z");
        Assert.assertFalse(fork.getTransactionTypes(time).contains(TransactionType.Payment));
        Assert.assertTrue(fork.getTransactionTypes(time).contains(TransactionType.Registration));
        Assert.assertFalse(fork.getTransactionTypes(time).contains(TransactionType.Quorum));

        time = getTime("2018-03-01T12:01:00.00Z");
        Assert.assertTrue(fork.getTransactionTypes(time).contains(TransactionType.ColoredCoinPayment));
        Assert.assertTrue(fork.getTransactionTypes(time).contains(TransactionType.Registration));
        Assert.assertFalse(fork.getTransactionTypes(time).contains(TransactionType.Quorum));
    }

    @Test
    public void forksTest_err_1() throws Exception {

        try {
            String connectURI = "jdbc:sqlite:file:memInitializerJsonTest6?mode=memory&cache=shared";
            Config config = createConfig(connectURI, "./com/exscudo/eon/app/IT/genesis_block.json", true);
            config.setForksFile("./com/exscudo/eon/app/IT/forks_err_add.json");
            PeerStarter peerStarter = new PeerStarter(config);

            peerStarter.getFork();

            Assert.fail();
        } catch (NullPointerException ex) {
            Assert.assertEquals("Unknown type: PaymentTTT", ex.getMessage());
        }
    }

    @Test
    public void forksTest_err_2() throws Exception {

        try {
            String connectURI = "jdbc:sqlite:file:memInitializerJsonTest7?mode=memory&cache=shared";
            Config config = createConfig(connectURI, "./com/exscudo/eon/app/IT/genesis_block.json", true);
            config.setForksFile("./com/exscudo/eon/app/IT/forks_err_del.json");
            PeerStarter peerStarter = new PeerStarter(config);

            peerStarter.getFork();

            Assert.fail();
        } catch (NullPointerException ex) {
            Assert.assertEquals("Unknown type: PaymentTTT", ex.getMessage());
        }
    }

    @Test
    public void forksTest_err_3() throws Exception {

        try {
            String connectURI = "jdbc:sqlite:file:memInitializerJsonTest8?mode=memory&cache=shared";
            Config config = createConfig(connectURI, "./com/exscudo/eon/app/IT/genesis_block.json", true);
            config.setForksFile("./com/exscudo/eon/app/IT/forks_err_format.json");
            PeerStarter peerStarter = new PeerStarter(config);

            peerStarter.getFork();

            Assert.fail();
        } catch (IOException ex) {
            Assert.assertEquals("Invalid forks-file format", ex.getMessage());
        }
    }

    private PeerStarter create(String connectURI,
                               String genesis,
                               boolean fullSync) throws SQLException, IOException, ClassNotFoundException {

        return new PeerStarter(createConfig(connectURI, genesis, fullSync));
    }

    private Config createConfig(String connectURI, String genesis, boolean fullSync) {
        Config config = new Config();
        config.setHost("0");
        config.setBlacklistingPeriod(30000);
        config.setPublicPeers(new String[] {"1"});
        config.setFullSync(fullSync);
        config.setDbUrl(connectURI);
        config.setGenesisFile(genesis);

        return config;
    }

    private int getTime(String time) {
        return (int) Instant.parse(time).getEpochSecond();
    }
}
