package com.exscudo.peer.store.sqlite.utils;

import java.io.EOFException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

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
	private boolean propLoaded = false;

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

	private void loadProperties() {
		if (!propLoaded) {
			try {
				this.setAccProps(BlockHelper.findAccProps(db, this.getID()));
			} catch (SQLException ignored) {
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

	@Override
	public AccountProperty[] getAccProps() {
		loadProperties();
		return super.getAccProps();
	}

	@Override
	public void setAccProps(AccountProperty[] accProps) {
		propLoaded = true;
		super.setAccProps(accProps);
	}

	boolean isTransactionLoaded() {
		return txLoaded;
	}

	boolean isAccPropsLoaded() {
		return propLoaded;
	}
}
