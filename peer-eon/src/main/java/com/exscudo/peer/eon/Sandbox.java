package com.exscudo.peer.eon;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;

/**
 * Allows you to get state changes when navigating between two blocks by
 * sequentially executing transactions.
 */
public class Sandbox {

	private final BufferedLedger bufferedLedger;
	private final int height;
	private final int timestamp;

	public Sandbox(ILedger ledger, int timestamp, int height) {
		this.bufferedLedger = new BufferedLedger(ledger);
		this.timestamp = timestamp;
		this.height = height;
	}

	public ILedger getLedger() {
		return bufferedLedger;
	}

	public void execute(Transaction transaction) throws ValidateException {
		TransactionContext ctx = new TransactionContext(timestamp, height);
		Fork currentFork = ForkProvider.getInstance();
		ITransactionHandler handler = currentFork.getTransactionExecutor(timestamp);
		handler.run(transaction, bufferedLedger, ctx);
	}

	public static Sandbox getInstance(IBlockchainService blockchain) {
		Block block = blockchain.getLastBlock();
		return getInstance(blockchain, block);
	}

	public static Sandbox getInstance(IBlockchainService blockchain, Block block) {
		ILedger inner = blockchain.getState(block.getSnapshot());
		if (inner == null) {
			throw new IllegalStateException();
		}
		return new Sandbox(inner, block.getTimestamp(), block.getHeight() + 1);
	}

	private static class BufferedLedger implements ILedger {

		private final ILedger ledger;
		private final Map<Long, IAccount> accounts;

		public BufferedLedger(ILedger ledger) {
			this.ledger = ledger;
			this.accounts = new HashMap<Long, IAccount>();
			this.accounts.put(Constant.DUMMY_ACCOUNT_ID, new Account(Constant.DUMMY_ACCOUNT_ID));
		}

		@Override
		public IAccount getAccount(long accountID) {

			IAccount wrapper = accounts.get(accountID);
			if (wrapper != null) {
				return wrapper;
			}

			wrapper = ledger.getAccount(accountID);
			if (wrapper != null) {
				accounts.put(accountID, wrapper);
			}

			return wrapper;

		}

		@Override
		public void putAccount(IAccount account) {
			accounts.put(account.getID(), account);
		}

		@Override
		public byte[] getHash() {
			// protect
			throw new UnsupportedOperationException();
		}

		public byte[] applyChanges(long creatorID) {

			IAccount dummyAccount = getAccount(Constant.DUMMY_ACCOUNT_ID);
			long totalFee = AccountBalance.getBalance(dummyAccount);
			if (totalFee != 0) {
				IAccount creator = getAccount(creatorID);
				AccountBalance.refill(creator, totalFee);
			}
			for (IAccount account : accounts.values()) {
				if (account.getID() != Constant.DUMMY_ACCOUNT_ID) {
					ledger.putAccount(account);
				}
			}
			return ledger.getHash();
		}
	}

	public byte[] createSnapshot(long creator) {
		return bufferedLedger.applyChanges(creator);
	}

}
