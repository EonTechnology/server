package com.exscudo.peer.core.blockchain.events;

import java.util.Objects;

import com.exscudo.peer.core.common.events.DispatchableEvent;
import com.exscudo.peer.core.common.events.Dispatcher;
import com.exscudo.peer.core.data.Block;

public class BlockEventManager {

    private final BlockEventListenerSupport blockEventSupport = new BlockEventListenerSupport();

    public void addListener(IBlockEventListener listener) {
        Objects.requireNonNull(listener);
        blockEventSupport.addListener(listener);
    }

    public void removeListener(IBlockEventListener listener) {
        Objects.requireNonNull(listener);
        blockEventSupport.removeListener(listener);
    }

    public void raiseLastBlockChanged(Object source, Block newLastBlock) {
        blockEventSupport.raiseEvent(new DispatchableEvent<IBlockEventListener, BlockEvent>(new BlockEvent(source,
                                                                                                           newLastBlock)) {
            @Override
            public void dispatch(IBlockEventListener target, BlockEvent event) {
                target.onLastBlockChanged(event);
            }
        });
    }

    public void raiseBeforeChanging(Object source, Block oldLastBlock) {
        blockEventSupport.raiseEvent(new DispatchableEvent<IBlockEventListener, BlockEvent>(new BlockEvent(source,
                                                                                                           oldLastBlock)) {
            @Override
            public void dispatch(IBlockEventListener target, BlockEvent event) {
                target.onBeforeChanging(event);
            }
        });
    }

    private static class BlockEventListenerSupport extends Dispatcher<IBlockEventListener> {
        public BlockEventListenerSupport() {
            super();
        }
    }
}
