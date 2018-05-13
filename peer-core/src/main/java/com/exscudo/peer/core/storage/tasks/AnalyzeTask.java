package com.exscudo.peer.core.storage.tasks;

import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.storage.Storage;

/**
 * Management of top-level transaction in DB.
 * <p>
 * SQLite speed up when transaction active.
 */
public class AnalyzeTask implements Runnable {
    private final Storage storage;

    public AnalyzeTask(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void run() {

        try {
            storage.analyze();
        } catch (Exception e) {
            Loggers.error(AnalyzeTask.class, "Unable to perform task.", e);
        }
    }
}
