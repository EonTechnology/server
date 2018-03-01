package com.exscudo.peer.core.storage;

import java.io.IOException;

/**
 * This interface defines the sequence of actions for preparing the data and the
 * required objects initialization.
 */
public interface IInitializer {

    void initialize(Storage storage) throws IOException;
}
