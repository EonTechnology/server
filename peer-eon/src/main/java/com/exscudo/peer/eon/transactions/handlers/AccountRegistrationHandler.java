package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class AccountRegistrationHandler implements ITransactionHandler {

	public void run(Transaction tx, ILedger ledger, TransactionContext context) {

		final Map<String, Object> data = tx.getData();

		for (Map.Entry<String, Object> entry : data.entrySet()) {
			byte[] publicKey = Format.convert(entry.getValue().toString());

			IAccount account = new Account(Format.MathID.pick(publicKey));
			AccountProperties.setPublicKey(account, publicKey);
			ledger.putAccount(account);
		}

	}

}
