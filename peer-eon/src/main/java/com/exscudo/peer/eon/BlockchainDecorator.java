package com.exscudo.peer.eon;

import java.util.Arrays;
import java.util.Objects;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.events.BlockEvent;
import com.exscudo.peer.core.events.DispatchableEvent;
import com.exscudo.peer.core.events.Dispatcher;
import com.exscudo.peer.core.events.IBlockEventListener;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.*;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;

/**
 * Pre-validation of blocks before pushing to chain.
 */
public class BlockchainDecorator implements IBlockchainService {
	private final BlockEventListenerSupport blockEventSupport = new BlockEventListenerSupport();
	private final IBlockchainService blockchain;

	private ITransactionHandler handler;

	public BlockchainDecorator(IBlockchainService blockchain, ITransactionHandler handler) {
		this.blockchain = blockchain;

		this.handler = handler;
	}

	@Override
	public IUnitOfWork beginPush(Object source, Block headBlock) {
		raiseBeforeChanging(source, headBlock);
		return new UnitOfWorkDecorator(blockchain.beginPush(source, headBlock), source);
	}

	@Override
	public LinkedBlock getLastBlock() {
		return blockchain.getLastBlock();
	}

	@Override
	public LinkedBlock getBlock(long blockID) {
		return blockchain.getBlock(blockID);
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

		public UnitOfWorkDecorator(IUnitOfWork uow, Object source) {
			this.uow = uow;
			this.source = source;
		}

		@Override
		public LinkedBlock getLastBlock() {
			return uow.getLastBlock();
		}

		@Override
		public LinkedBlock getBlock(long blockID) {
			return uow.getBlock(blockID);
		}

		@Override
		public Block pushBlock(Block block) throws ValidateException {
			block = ensureValidBlock(block);
			return uow.pushBlock(block);
		}

		private Block ensureValidBlock(Block newBlock) throws ValidateException {

			if (newBlock.getVersion() != 1) {
				throw new ValidateException("Invalid block version.");
			}

			Block prevBlock = uow.getLastBlock();
			if (newBlock.getPreviousBlock() == 0) {
				throw new ValidateException("Previous block is not specified.");
			} else if (prevBlock.getID() != newBlock.getPreviousBlock()) {
				throw new ValidateException(
						"Unexpected block. Expected - " + Format.ID.blockId(newBlock.getPreviousBlock())
								+ ", current - " + Format.ID.blockId(prevBlock.getID()));
			} else if (newBlock.getTimestamp() <= prevBlock.getTimestamp()
					|| newBlock.getTimestamp() - prevBlock.getTimestamp() != Constant.BLOCK_PERIOD) {
				throw new LifecycleException();
			}

			newBlock.setHeight(prevBlock.getHeight() + 1);

			IAccount generator = uow.getLastBlock().getState().getAccount(newBlock.getSenderID());
			if (generator == null) {
				throw new ValidateException("Invalid generator. " + Format.ID.accountId(newBlock.getSenderID()));
			}

			long generatingBalance = 0;
			AccountDeposit deposit = AccountDeposit.parse(generator);
			if (deposit.getValue() >= EonConstant.MIN_DEPOSIT_SIZE
					&& (newBlock.getHeight() - deposit.getHeight() >= Constant.BLOCK_IN_DAY
							|| deposit.getHeight() == 0)) {
				generatingBalance = deposit.getValue();
			}

			newBlock.setCumulativeDifficulty(
					DifficultyHelper.calculateDifficulty(newBlock, prevBlock, generatingBalance));

			byte[] generatorPublicKey = AccountAttributes.getPublicKey(generator);
			if (!newBlock.verifySignature(generatorPublicKey)) {
				throw new IllegalSignatureException();
			}

			int height = newBlock.getHeight() - EonConstant.DIFFICULTY_DELAY;
			Block baseBlock = prevBlock;
			if (height > 0) {
				while (baseBlock.getHeight() > height) {
					baseBlock = uow.getBlock(baseBlock.getPreviousBlock());
				}
			}

			if (!CryptoProvider.getInstance().verifySignature(baseBlock.getGenerationSignature(),
					newBlock.getGenerationSignature(), generatorPublicKey)) {
				throw new IllegalSignatureException("The field Generation Signature is incorrect.");
			}

			TransactionContext context = new TransactionContext(newBlock.getTimestamp(), generator.getID());

			ISandbox sandbox = uow.getLastBlock().createSandbox(handler);

			Transaction[] sortedTransactions = newBlock.getTransactions().toArray(new Transaction[0]);
			Arrays.sort(sortedTransactions, new TransactionComparator());

			for (Transaction tx : sortedTransactions) {
				sandbox.execute(tx, context);
			}

			AccountProperty[] properties = sandbox.getProperties();
			for (AccountProperty property : properties) {
				property.setHeight(newBlock.getHeight());
			}
			newBlock.setAccProps(properties);

			return newBlock;

		}

		@Override
		public void commit() {

			synchronized (blockchain) {

				Difficulty diffNew = new Difficulty(uow.getLastBlock());
				Difficulty currNew = new Difficulty(blockchain.getLastBlock());

				if (diffNew.compareTo(currNew) > 0) {
					uow.commit();
					raiseLastBlockChanged(source, blockchain.getLastBlock());
				} else {
					throw new IllegalStateException();
				}

			}

		}

		@Override
		public void rollback() {
			uow.rollback();
		}

	}

}
