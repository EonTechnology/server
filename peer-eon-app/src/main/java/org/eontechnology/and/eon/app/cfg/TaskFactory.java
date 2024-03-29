package org.eontechnology.and.eon.app.cfg;

import java.io.IOException;
import java.sql.SQLException;
import org.eontechnology.and.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import org.eontechnology.and.peer.core.backlog.tasks.SyncTransactionListTask;
import org.eontechnology.and.peer.core.blockchain.tasks.BlockCleanerTask;
import org.eontechnology.and.peer.core.blockchain.tasks.NestedTransactionCleanupTask;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.common.tasks.SyncTimeTask;
import org.eontechnology.and.peer.core.env.tasks.PeerConnectTask;
import org.eontechnology.and.peer.core.env.tasks.PeerDistributeTask;
import org.eontechnology.and.peer.core.env.tasks.PeerRemoveTask;
import org.eontechnology.and.peer.core.env.tasks.SyncPeerListTask;
import org.eontechnology.and.peer.core.importer.tasks.GenerateBlockTask;
import org.eontechnology.and.peer.core.importer.tasks.SyncBlockListTask;
import org.eontechnology.and.peer.core.importer.tasks.SyncSnapshotTask;
import org.eontechnology.and.peer.core.ledger.tasks.NodesCleanupTask;
import org.eontechnology.and.peer.core.storage.tasks.AnalyzeTask;

public class TaskFactory {

  private final PeerStarter starter;

  public TaskFactory(PeerStarter starter) {
    this.starter = starter;
  }

  public PeerConnectTask getPeerConnectTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new PeerConnectTask(
        starter.getFork(),
        starter.getExecutionContext(),
        starter.getTimeProvider(),
        starter.getBlockchainProvider());
  }

  public PeerDistributeTask getPeerDistributeTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new PeerDistributeTask(starter.getExecutionContext());
  }

  public PeerRemoveTask getPeerRemoveTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new PeerRemoveTask(starter.getExecutionContext());
  }

  public SyncForkedTransactionListTask getSyncForkedTransactionListTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncForkedTransactionListTask(
        starter.getFork(),
        starter.getExecutionContext(),
        starter.getTimeProvider(),
        starter.getBacklog(),
        starter.getBlockchainProvider());
  }

  public SyncPeerListTask getSyncPeerListTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncPeerListTask(starter.getExecutionContext());
  }

  public SyncTimeTask getSyncTimeTask() {
    return new SyncTimeTask((TimeProvider) starter.getTimeProvider());
  }

  public SyncTransactionListTask getSyncTransactionListTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncTransactionListTask(
        starter.getFork(),
        starter.getExecutionContext(),
        starter.getTimeProvider(),
        starter.getBacklog(),
        starter.getBlockchainProvider());
  }

  public GenerateBlockTask getGenerateBlockTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new GenerateBlockTask(
        starter.getFork(),
        starter.getBlockGenerator(),
        starter.getTimeProvider(),
        starter.getLedgerProvider(),
        starter.getBlockchainProvider(),
        starter.getTransactionValidatorFabric(),
        starter.getEstimator(),
        starter.getAccountHelper(),
        starter.getTransactionMapper());
  }

  public SyncBlockListTask getSyncBlockListTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncBlockListTask(
        starter.getFork(),
        starter.getBacklog(),
        starter.getStorage(),
        starter.getExecutionContext(),
        starter.getBlockchainProvider(),
        starter.getTimeProvider(),
        starter.getLedgerProvider(),
        starter.getTransactionValidatorFabric(),
        starter.getEstimator(),
        starter.getAccountHelper(),
        starter.getTransactionMapper());
  }

  public AnalyzeTask getAnalyzeTask() throws SQLException, IOException, ClassNotFoundException {
    return new AnalyzeTask(starter.getStorage());
  }

  public BlockCleanerTask getBlockCleanerTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new BlockCleanerTask(starter.getBlockchainProvider(), starter.getStorage());
  }

  public NodesCleanupTask getNodesCleanupTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new NodesCleanupTask(starter.getStorage());
  }

  public SyncSnapshotTask getSyncSnapshotTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncSnapshotTask(
        starter.getStorage(),
        starter.getExecutionContext(),
        starter.getFork(),
        starter.getBlockchainProvider(),
        starter.getLedgerProvider(),
        starter.getAccountHelper());
  }

  public NestedTransactionCleanupTask getNestedTransactionCleanupTask()
      throws SQLException, IOException, ClassNotFoundException {
    return new NestedTransactionCleanupTask(
        starter.getBlockchainProvider(), starter.getStorage(), starter.getFork());
  }
}
