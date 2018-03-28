package com.exscudo.peer.core;

import java.io.IOException;

import com.exscudo.peer.core.storage.Storage;

/**
 * This interface defines the sequence of actions for preparing the data and the
 * required objects initialization.
 */
public interface IInitializer {

    void initialize(Storage storage) throws IOException;
}
