package org.eontechnology.and.peer.core.blockchain.events;

import java.util.List;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;

/** This is the base event which is associated with a last block updated.. */
public class UpdatedBlockchainEvent extends BlockchainEvent {

  /** List of transactions that are outside the main chain after the block is accepted. */
  public final List<Transaction> forked;

  public UpdatedBlockchainEvent(Object source, Block block, List<Transaction> forked) {
    super(source, block);

    this.forked = forked;
  }
}
