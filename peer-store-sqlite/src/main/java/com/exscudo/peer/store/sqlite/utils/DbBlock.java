package com.exscudo.peer.store.sqlite.utils;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

import java.io.EOFException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Implementation of {@code Block} with lazy-loading transactions and account
 * properties.
 *
 * @see Block
 */
class DbBlock extends Block implements Serializable {
	private static final long serialVersionUID = -5179829262861279368L;

	private ConnectionProxy db;
	private boolean txLoaded = false;

	DbBlock(ConnectionProxy db) {

		this.db = db;
	}

	private void loadTransactions() {
		if (!txLoaded) {
			try {
				this.setTransactions(BlockHelper.getTransactions(db, this.getID()));
			} catch (SQLException | EOFException ignored) {
			}
		}
	}

	@Override
	public Collection<Transaction> getTransactions() {
		loadTransactions();
		return super.getTransactions();
	}

	@Override
	public void setTransactions(Collection<Transaction> transactions) {
		txLoaded = true;
		super.setTransactions(transactions);
	}


}
