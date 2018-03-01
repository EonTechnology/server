package com.exscudo.peer.core.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public interface IStorageAction {
    void run(Connection connection, Storage.Metadata metadata) throws SQLException, IOException;
}
