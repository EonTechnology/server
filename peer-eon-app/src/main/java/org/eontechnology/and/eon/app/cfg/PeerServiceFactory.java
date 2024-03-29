package org.eontechnology.and.eon.app.cfg;

import java.io.IOException;
import java.sql.SQLException;
import org.eontechnology.and.eon.app.api.peer.SyncBlockService;
import org.eontechnology.and.eon.app.api.peer.SyncMetadataService;
import org.eontechnology.and.eon.app.api.peer.SyncSnapshotService;
import org.eontechnology.and.eon.app.api.peer.SyncTransactionService;

public class PeerServiceFactory {

  private final PeerStarter starter;

  public PeerServiceFactory(PeerStarter starter) {
    this.starter = starter;
  }

  public SyncBlockService getSyncBlockService()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncBlockService(starter.getBlockchainProvider());
  }

  public SyncMetadataService getSyncMetadataService()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncMetadataService(
        starter.getFork(),
        starter.getExecutionContext(),
        starter.getStorage(),
        starter.getTimeProvider());
  }

  public SyncTransactionService getSyncTransactionService()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncTransactionService(
        starter.getFork(),
        starter.getTimeProvider(),
        starter.getBacklog(),
        starter.getBlockchainProvider());
  }

  public SyncSnapshotService getSyncSnapshotService()
      throws SQLException, IOException, ClassNotFoundException {
    return new SyncSnapshotService(starter.getBlockchainProvider(), starter.getLedgerProvider());
  }
}
