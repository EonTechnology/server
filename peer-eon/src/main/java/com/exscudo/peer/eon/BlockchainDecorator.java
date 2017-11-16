package com.exscudo.peer.eon;

import java.util.Objects;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.events.BlockEvent;
import com.exscudo.peer.core.events.DispatchableEvent;
import com.exscudo.peer.core.events.Dispatcher;
import com.exscudo.peer.core.events.IBlockEventListener;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionMapper;
import com.exscudo.peer.core.services.IUnitOfWork;

/**
 * Pre-validation of blocks before pushing to chain.
 */
public class BlockchainDecorator implements IBlockchainService {
	private final BlockEventListenerSupport blockEventSupport = new BlockEventListenerSupport();
	private final IBlockchainService blockchain;

	public BlockchainDecorator(IBlockchainService blockchain) {
		this.blockchain = blockchain;
	}

	@Override
	public IUnitOfWork beginPush(Object source, Block headBlock) {
		raiseBeforeChanging(source, headBlock);
		return new UnitOfWorkDecorator(blockchain.beginPush(source, headBlock), source);
	}

	@Override
	public Block getLastBlock() {
		return blockchain.getLastBlock();
	}

	@Override
	public Block getBlock(long blockID) {
		return blockchain.getBlock(blockID);
	}

	@Override
	public Block getBlockByHeight(int height) {
		return blockchain.getBlock(height);
	}

	@Override
	public int getBlockHeight(long id) {
		return blockchain.getBlockHeight(id);
	}

	@Override
	public long[] getLatestBlocks(int frameSize) {
		return blockchain.getLatestBlocks(frameSize);
	}

	@Override
	public ITransactionMapper transactionMapper() {
		return blockchain.transactionMapper();
	}

	@Override
	public ILedger getState(byte[] snapshot) {
		return blockchain.getState(snapshot);
	}

	public void addListener(IBlockEventListener listener) {
		Objects.requireNonNull(listener);
		blockEventSupport.addListener(listener);
	}

	public void removeListener(IBlockEventListener listener) {
		Objects.requireNonNull(listener);
		blockEventSupport.removeListener(listener);
	}

	protected void raiseLastBlockChanged(Object source, Block newLastBlock) {
		blockEventSupport.raiseEvent(
				new DispatchableEvent<IBlockEventListener, BlockEvent>(new BlockEvent(source, newLastBlock)) {
					@Override
					public void dispatch(IBlockEventListener target, BlockEvent event) {
						target.onLastBlockChanged(event);
					}
				});
	}

	protected void raiseBeforeChanging(Object source, Block oldLastBlock) {
		blockEventSupport.raiseEvent(
				new DispatchableEvent<IBlockEventListener, BlockEvent>(new BlockEvent(source, oldLastBlock)) {
					@Override
					public void dispatch(IBlockEventListener target, BlockEvent event) {
						target.onBeforeChanging(event);
					}
				});
	}

	//
	// Nested types
	//

	private static class BlockEventListenerSupport extends Dispatcher<IBlockEventListener> {
		public BlockEventListenerSupport() {
			super();
		}
	}

	private class UnitOfWorkDecorator implements IUnitOfWork {
		private final IUnitOfWork uow;
		private final Object source;

		private Block lastPushedBlock = null;

		public UnitOfWorkDecorator(IUnitOfWork uow, Object source) {
			this.uow = uow;
			this.source = source;
		}

		@Override
		public Block pushBlock(Block block) throws ValidateException {
			return (lastPushedBlock = uow.pushBlock(block));
		}

		@Override
		public void commit() {
			uow.commit();
			if (lastPushedBlock != null) {
				raiseLastBlockChanged(source, lastPushedBlock);
			}
		}

		@Override
		public void rollback() {
			uow.rollback();
		}

	}

}
