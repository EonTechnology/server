package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.transactions.rules.AccountRegistrationValidationRule;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;

public class AccountRegistrationHandler extends BaseHandler {

	public AccountRegistrationHandler() {
		super(new AccountRegistrationValidationRule());
	}

	protected void doRun(Transaction tx, ILedger ledger, TransactionContext context) {
		super.doRun(tx, ledger, context);

		final Map<String, Object> data = tx.getData();

		for (Map.Entry<String, Object> entry : data.entrySet()) {
			byte[] publicKey = Format.convert(entry.getValue().toString());

			IAccount account = new Account(Format.MathID.pick(publicKey));
			AccountAttributes.setPublicKey(account, publicKey);
			ledger.putAccount(account);
		}
	}

}
