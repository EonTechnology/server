package org.eontechology.and.eon.app.cfg;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eontechology.and.peer.core.common.tasks.SequencedTask;
import org.eontechology.and.peer.core.common.tasks.TimedTask;

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
            starter.getBlockchainEventManager().addListener(starter.getBlockGenerator());
            starter.getBlockchainEventManager().addListener(starter.getCleaner());

            TaskFactory taskFactory = new TaskFactory(starter);

            Engine engine = new Engine();

            engine.scheduledThreadPool = Executors.newScheduledThreadPool(14);

            if (!starter.getConfig().isInner()) {
                engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getSyncPeerListTask()),
                                                               0,
                                                               5,
                                                               TimeUnit.SECONDS);
                engine.scheduledThreadPool.scheduleAtFixedRate(timed(taskFactory.getPeerDistributeTask()),
                                                               0,
                                                               30,
                                                               TimeUnit.SECONDS);
            }
            engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getPeerRemoveTask()),
                                                              0,
                                                              1,
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

            if (starter.getConfig().isUseCleaner()) {
                SequencedTask cleanTask = new SequencedTask(new Runnable[] {
                        timed(taskFactory.getAnalyzeTask()),
                        timed(taskFactory.getBlockCleanerTask()),
                        timed(taskFactory.getNodesCleanupTask()),
                        timed(taskFactory.getNestedTransactionCleanupTask()),
                        timed(taskFactory.getAnalyzeTask())
                }, 10 * 60 * 1000);

                engine.scheduledThreadPool.scheduleWithFixedDelay(timed(cleanTask), 30, 60, TimeUnit.MINUTES);
            } else {
                engine.scheduledThreadPool.scheduleWithFixedDelay(timed(taskFactory.getAnalyzeTask()),
                                                                  30,
                                                                  60,
                                                                  TimeUnit.MINUTES);
            }

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
