package org.eontechnology.and.peer.core.blockchain.events;

import java.util.List;
import java.util.Objects;
import org.eontechnology.and.peer.core.common.events.DispatchableEvent;
import org.eontechnology.and.peer.core.common.events.Dispatcher;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;

/** Basic implementation of the manager for Blockchain events. */
public class BlockchainEventManager {

  private final BlockEventListenerSupport blockEventSupport = new BlockEventListenerSupport();

  public void addListener(IBlockchainEventListener listener) {
    Objects.requireNonNull(listener);
    blockEventSupport.addListener(listener);
  }

  public void removeListener(IBlockchainEventListener listener) {
    Objects.requireNonNull(listener);
    blockEventSupport.removeListener(listener);
  }

  public void raiseBlockchainChanged(Object source, Block newLastBlock, List<Transaction> forked) {
    blockEventSupport.raiseEvent(
        new DispatchableEvent<IBlockchainEventListener, UpdatedBlockchainEvent>(
            new UpdatedBlockchainEvent(source, newLastBlock, forked)) {
          @Override
          public void dispatch(IBlockchainEventListener target, UpdatedBlockchainEvent event) {
            target.onChanged(event);
          }
        });
  }

  public void raiseBlockchainChanging(Object source, Block oldLastBlock) {
    blockEventSupport.raiseEvent(
        new DispatchableEvent<IBlockchainEventListener, BlockchainEvent>(
            new BlockchainEvent(source, oldLastBlock)) {
          @Override
          public void dispatch(IBlockchainEventListener target, BlockchainEvent event) {
            target.onChanging(event);
          }
        });
  }

  static class BlockEventListenerSupport extends Dispatcher<IBlockchainEventListener> {
    BlockEventListenerSupport() {
      super();
    }
  }
}
