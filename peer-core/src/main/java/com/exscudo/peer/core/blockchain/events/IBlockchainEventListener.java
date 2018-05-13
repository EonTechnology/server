package com.exscudo.peer.core.blockchain.events;

import java.util.EventListener;

/**
 * This interface defines the list of events that occur when interacting with
 * block.
 */
public interface IBlockchainEventListener extends EventListener {

    /**
     * Gate to get a notification about the synchronization start.
     *
     * @param event
     */
    void onChanging(BlockchainEvent event);

    /**
     * Gate to get a notification that the last block has changed.
     *
     * @param event
     */
    void onChanged(UpdatedBlockchainEvent event);
}
