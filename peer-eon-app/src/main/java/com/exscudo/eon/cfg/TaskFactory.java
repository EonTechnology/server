package com.exscudo.eon.cfg;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import com.exscudo.peer.core.backlog.tasks.SyncTransactionListTask;
import com.exscudo.peer.core.common.tasks.SyncTimeTask;
import com.exscudo.peer.core.env.tasks.PeerConnectTask;
import com.exscudo.peer.core.env.tasks.PeerDistributeTask;
import com.exscudo.peer.core.env.tasks.PeerRemoveTask;
import com.exscudo.peer.core.env.tasks.SyncPeerListTask;
import com.exscudo.peer.core.importer.tasks.GenerateBlockTask;
import com.exscudo.peer.core.importer.tasks.SyncBlockListTask;
import com.exscudo.peer.core.ledger.tasks.NodesCleanupTask;
import com.exscudo.peer.core.storage.tasks.AnalyzeTask;
import com.exscudo.peer.core.storage.tasks.Cleaner;

public class TaskFactory {

    private final PeerStarter starter;

    public TaskFactory(PeerStarter starter) {
        this.starter = starter;
    }

    public PeerConnectTask getPeerConnectTask() throws SQLException, IOException, ClassNotFoundException {
        return new PeerConnectTask(starter.getFork(),
                                   starter.getExecutionContext(),
                                   starter.getTimeProvider(),
                                   starter.getBlockchain());
    }

    public PeerDistributeTask getPeerDistributeTask() throws SQLException, IOException, ClassNotFoundException {
        return new PeerDistributeTask(starter.getExecutionContext());
    }

    public PeerRemoveTask getPeerRemoveTask() throws SQLException, IOException, ClassNotFoundException {
        return new PeerRemoveTask(starter.getExecutionContext());
    }

    public SyncForkedTransactionListTask getSyncForkedTransactionListTask() throws SQLException, IOException, ClassNotFoundException {
        return new SyncForkedTransactionListTask(starter.getFork(),
                                                 starter.getExecutionContext(),
                                                 starter.getTimeProvider(),
                                                 starter.getBacklog(),
                                                 starter.getBlockchain());
    }

    public SyncPeerListTask getSyncPeerListTask() throws SQLException, IOException, ClassNotFoundException {
        return new SyncPeerListTask(starter.getExecutionContext());
    }

    public SyncTimeTask getSyncTimeTask() {
        return new SyncTimeTask(starter.getTimeProvider());
    }

    public SyncTransactionListTask getSyncTransactionListTask() throws SQLException, IOException, ClassNotFoundException {
        return new SyncTransactionListTask(starter.getFork(),
                                           starter.getExecutionContext(),
                                           starter.getTimeProvider(),
                                           starter.getBacklog(),
                                           starter.getBlockchain());
    }

    public GenerateBlockTask getGenerateBlockTask() throws SQLException, IOException, ClassNotFoundException {
        return new GenerateBlockTask(starter.getFork(),
                                     starter.getBlockGenerator(),
                                     starter.getTimeProvider(),
                                     starter.getLedgerProvider(),
                                     starter.getBlockEventManager(),
                                     starter.getBlockchain());
    }

    public SyncBlockListTask getSyncBlockListTask() throws SQLException, IOException, ClassNotFoundException {
        return new SyncBlockListTask(starter.getFork(),
                                     starter.getExecutionContext(),
                                     starter.getBlockchain(),
                                     starter.getTimeProvider(),
                                     starter.getLedgerProvider(),
                                     starter.getBlockEventManager());
    }

    public AnalyzeTask getAnalyzeTask() throws SQLException, IOException, ClassNotFoundException {
        return new AnalyzeTask(starter.getBlockchain(), starter.getStorage());
    }

    public Cleaner getDBCleaner() throws SQLException, IOException, ClassNotFoundException {
        return new Cleaner(starter.getBlockchain(), starter.getStorage());
    }

    public NodesCleanupTask getNodesCleanupTask() throws SQLException, IOException, ClassNotFoundException {
        return new NodesCleanupTask(starter.getStorage());
    }
}
