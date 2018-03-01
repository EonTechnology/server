package com.exscudo.peer.core.blockchain.events;

import java.util.EventObject;

import com.exscudo.peer.core.data.Block;

/***
 * This is the base class of all dispatched events which are associated with a
 * block.
 *
 */
public class BlockEvent extends EventObject {
    private static final long serialVersionUID = 1045860526067590795L;

    public final Block block;

    public BlockEvent(Object source, Block block) {
        super(source);

        this.block = block;
    }
}
