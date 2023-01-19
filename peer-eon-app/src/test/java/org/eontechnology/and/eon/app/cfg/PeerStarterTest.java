package org.eontechnology.and.eon.app.cfg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import junit.framework.TestCase;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.blockchain.BlockchainProvider;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.eontechnology.and.peer.core.storage.Storage;
import org.eontechnology.and.peer.tx.TransactionType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PeerStarterTest {
  private static final String connectURI =
      "jdbc:sqlite:file:memInitializerJsonTest?mode=memory&cache=shared";
  private static final String genesisJson = "eon_network/dev/genesis_block.json";
  private static final String genesisForks = "eon_network/dev/forks.json";

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testBlockIds() throws Exception {
    PeerStarter peerStarter = create(connectURI, genesisJson, genesisForks, true);

    Storage storage = peerStarter.getStorage();
    Storage.Metadata metadata = storage.metadata();
    BlockID lastBlockId = metadata.getLastBlockID();
    BlockID genesisBlockId = metadata.getGenesisBlockID();

    BlockchainProvider blockchainProvider = peerStarter.getBlockchainProvider();
    Assert.assertEquals(
        blockchainProvider.getLastBlock().getID().getValue(), lastBlockId.getValue());
    TestCase.assertNotNull(blockchainProvider.getBlock(genesisBlockId));
  }

  @Test
  public void testBlockSnapshot() throws Exception {

    PeerStarter peerStarter = create(connectURI, genesisJson, genesisForks, true);

    BlockchainProvider blockchainProvider = peerStarter.getBlockchainProvider();
    Block lastBlock = blockchainProvider.getLastBlock();

    LedgerProvider ledgerProvider = peerStarter.getLedgerProvider();
    ILedger ledger = ledgerProvider.getLedger(lastBlock);

    assertEquals(lastBlock.getSnapshot(), ledger.getHash());
  }

  @Test
  public void testSyncSnapshotDisable() throws Exception {
    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest2?mode=memory&cache=shared";

    try {

      PeerStarter ps1 = create(connectURI, genesisJson, genesisForks, true);
      PeerStarter ps2 = create(connectURI, genesisJson, genesisForks, false);

      assertTrue("Snapshot mode switched", true);
    } catch (Exception ex) {
      assertTrue("Snapshot mode cannot be switched", false);
    }
  }

  @Test
  public void testSyncSnapshotEnable() throws Exception {
    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest3?mode=memory&cache=shared";

    try {

      PeerStarter ps1 = create(connectURI, genesisJson, genesisForks, false);
      PeerStarter ps2 = create(connectURI, genesisJson, genesisForks, true);

      assertTrue("Snapshot mode switched", false);
    } catch (Exception ex) {
      assertTrue("Snapshot mode cannot be switched", true);
    }
  }

  @Test
  public void mainBlockIds() throws Exception {
    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest4?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI, "eon_network/main/genesis_block.json", "eon_network/main/forks.json", true);

    Storage storage = peerStarter.getStorage();
    Storage.Metadata metadata = storage.metadata();
    BlockID lastBlockId = metadata.getLastBlockID();
    BlockID genesisBlockId = metadata.getGenesisBlockID();

    BlockchainProvider blockchainProvider = peerStarter.getBlockchainProvider();
    Assert.assertEquals(
        blockchainProvider.getLastBlock().getID().getValue(), lastBlockId.getValue());
    TestCase.assertNotNull(blockchainProvider.getBlock(genesisBlockId));
  }

  @Test
  public void forksTest_OK() throws Exception {
    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest5?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks.json",
            true);

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
  public void forksTest_err_add_cast_exception() throws Exception {
    expectedException.expect(IOException.class);

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest6?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_add_cast_exception.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_re_adding_parser() throws Exception {
    expectedException.expect(IOException.class);

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest7?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_add_re_adding.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_unknown_parser() throws Exception {
    expectedException.expect(IOException.class);

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest7?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_add_unknown_parser.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_add_unknown_type_parser() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Unknown type: PaymentTTT");

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest6?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_add_unknwon_type.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_del_parser() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Unknown type: PaymentTTT");

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest8?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_del.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_format() throws Exception {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Invalid forks-file format");

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest8?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_format.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_no_params() throws Exception {
    expectedException.expect(IOException.class);

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest7?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_no_params.json",
            true);

    peerStarter.getFork();
  }

  @Test
  public void forksTest_err_incorrect_params() throws Exception {
    expectedException.expect(IOException.class);

    String connectURI = "jdbc:sqlite:file:memInitializerJsonTest7?mode=memory&cache=shared";
    PeerStarter peerStarter =
        create(
            connectURI,
            "org/eontechnology/and/eon/app/IT/genesis_block.json",
            "org/eontechnology/and/eon/app/IT/forks_err_incorrect_params.json",
            true);

    peerStarter.getFork();
  }

  private PeerStarter create(String connectURI, String genesis, String forks, boolean fullSync)
      throws SQLException, IOException, ClassNotFoundException {

    PeerStarter peerStarter = new PeerStarter(createConfig(connectURI, genesis, forks, fullSync));
    peerStarter.initialize();
    return peerStarter;
  }

  private Config createConfig(String connectURI, String genesis, String forks, boolean fullSync) {
    Config config = new Config();
    config.setHost("0");
    config.setBlacklistingPeriod(30000);
    config.setPublicPeers(new String[] {"1"});
    config.setFullSync(fullSync);
    config.setDbUrl(connectURI);
    config.setGenesisFile(genesis);
    config.setForksFile(forks);

    return config;
  }

  private int getTime(String time) {
    return (int) Instant.parse(time).getEpochSecond();
  }
}
