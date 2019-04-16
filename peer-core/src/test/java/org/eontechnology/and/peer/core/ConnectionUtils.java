package org.eontechnology.and.peer.core;

import java.io.IOException;

import org.eontechnology.and.peer.core.storage.IInitializer;
import org.eontechnology.and.peer.core.storage.Storage;

public class ConnectionUtils {

    private static int DB = 1;

    public static Storage create() throws Exception {
        Storage storage = Storage.create("jdbc:sqlite:file:memTestDB" + DB + "?mode=memory&cache=shared");
        Initializer initializer = new Initializer();
        initializer.initialize(storage);
        DB++;
        return storage;
    }

    private static class Initializer implements IInitializer {
        @Override
        public void initialize(Storage storage) throws IOException {
            storage.run(new String[] {
                    "/org/eontechnology/and/peer/store/sqlite/MigrateV1.sql"
            });
        }
    }
}
