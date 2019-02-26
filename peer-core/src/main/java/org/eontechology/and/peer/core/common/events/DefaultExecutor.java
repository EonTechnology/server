package org.eontechology.and.peer.core.common.events;

import java.util.concurrent.Executor;

/**
 * The default class to start a task.
 * <p>
 * Describes default mechanics of how each task will be run.
 */
class DefaultExecutor implements Executor {

    public void execute(Runnable r) {
        r.run();
    }
}
