package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Block;

/**
 * Defines a block built into the chain of the blocks.
 * <p>
 * Each block in the chain corresponds to a certain state (set of properties of
 * accounts). The state is described using a specific implementation
 * {@code ILedger}. The transition between states is done using the
 * {@code ISandbox}, which provides access to the modified set of properties.
 */
public abstract class LinkedBlock extends Block {

	private static final long serialVersionUID = 6766771972020755228L;

	public abstract ILedger getState();

	public abstract ISandbox createSandbox(ITransactionHandler handler);

}
