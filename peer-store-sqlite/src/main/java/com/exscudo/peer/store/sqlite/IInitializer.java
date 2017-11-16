package com.exscudo.peer.store.sqlite;

import java.io.IOException;
import java.sql.Connection;

/**
 * This interface defines the sequence of actions for preparing the data and the
 * required objects initialization.
 */
public interface IInitializer {

	void initialize(Connection connection) throws IOException;

}
