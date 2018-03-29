package com.exscudo.eon.cfg;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.exscudo.peer.core.common.tasks.TimedTask;
import com.exscudo.peer.core.crypto.CryptoProvider;

/**
 * Initializer peer tasks
 */
public class Engine {

    private ScheduledExecutorService scheduledThreadPool;

    private Engine() {
    }

    public static Engine init(PeerStarter starter) {

        try {

            starter.getExecutionContext().addListener(starter.getBlockGenerator());
            starter.getBlockEventManager().addListener(starter.getBlockGenerator());
            starter.getBlockEventManager().addListener(starter.getCleaner());

            TaskFactory taskFactory = new TaskFactory(starter);

            CryptoProvider.init(starter.getCryptoProvider());

            Engine engine = new Engine();

            engine.scheduledThreadPool = Executors.newScheduledThreadPool(12);

            engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getSyncPeerListTask()),
                                                           0,
                                                           5,
                                                           TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getPeerRemoveTask()),
                                                              0,
                                                              1,
                                                              TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getPeerDistributeTask()),
                                                           0,
                                                           30,
                                                           TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getPeerConnectTask()),
                                                           0,
                                                           5,
                                                           TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getSyncTimeTask()),
                                                           0,
                                                           60,
                                                           TimeUnit.MINUTES);
            engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getSyncTransactionListTask()),
                                                           0,
                                                           1,
                                                           TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getGenerateBlockTask()),
                                                              0,
                                                              1,
                                                              TimeUnit.SECONDS);
            // Disabled SyncForkedTransactionListTask
            // engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getSyncForkedTransactionListTask()),0,3,TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getSyncBlockListTask()),
                                                              0,
                                                              1,
                                                              TimeUnit.SECONDS);
            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getSyncSnapshotTask()),
                                                              0,
                                                              1,
                                                              TimeUnit.SECONDS);

            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getAnalyzeTask()),
                                                              15,
                                                              60,
                                                              TimeUnit.MINUTES);
            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getBlockCleanerTask()),
                                                              30,
                                                              60,
                                                              TimeUnit.MINUTES);
//            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getNodesCleanupTask()),
//                                                              45,
//                                                              60,
//                                                              TimeUnit.MINUTES);

            return engine;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Runnable timed(Runnable main) {
        return new TimedTask(main);
    }

    public void destory() {
        scheduledThreadPool.shutdownNow();
    }
}
