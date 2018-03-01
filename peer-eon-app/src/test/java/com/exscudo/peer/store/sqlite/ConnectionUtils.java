package com.exscudo.peer.store.sqlite;

import java.io.IOException;

import com.exscudo.peer.core.storage.IInitializer;
import com.exscudo.peer.core.storage.Storage;

class ConnectionUtils {

    private static int DB = 1;

    public static Storage create(String url) throws Exception {
        Storage storage =
                Storage.create("jdbc:sqlite:file:memTestDB" + DB + "?mode=memory&cache=shared", new Initializer(url));
        DB++;
        return storage;
    }

    private static class Initializer implements IInitializer {
        private final String url;

        public Initializer(String url) {
            this.url = url;
        }

        @Override
        public void initialize(Storage storage) throws IOException {
            storage.run(new String[] {
                    "/com/exscudo/peer/store/sqlite/MigrateV1.sql", url
            });
        }
    }
}
