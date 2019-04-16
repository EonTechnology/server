package org.eontechnology.and.peer.core.blockchain.events;

import java.util.EventObject;

import org.eontechnology.and.peer.core.data.Block;

/***
 * This is the base class of all dispatched events which are associated with a
 * Blockchain.
 *
 */
public class BlockchainEvent extends EventObject {
    private static final long serialVersionUID = 1045860526067590795L;

    public final Block block;

    public BlockchainEvent(Object source, Block block) {
        super(source);

        this.block = block;
    }
}
