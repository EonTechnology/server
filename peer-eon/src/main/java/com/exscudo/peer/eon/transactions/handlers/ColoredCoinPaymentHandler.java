package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.eon.utils.ColoredCoinId;

public class ColoredCoinPaymentHandler implements ITransactionHandler {

	@Override
	public void run(Transaction tx, ILedger ledger, TransactionContext context) throws ValidateException {

		Map<String, Object> data = tx.getData();

		long recipientID = Format.ID.accountId(data.get("recipient").toString());
		long amount = Long.parseLong(data.get("amount").toString());
		long color = ColoredCoinId.convert(data.get("color").toString());

		IAccount sender = ledger.getAccount(tx.getSenderID());
		ColoredBalance senderColoredBalance = AccountProperties.getColoredBalance(sender);
		senderColoredBalance.withdraw(amount, color);
		AccountProperties.setColoredBalance(sender, senderColoredBalance);
		ledger.putAccount(sender);

		IAccount recipient = ledger.getAccount(recipientID);
		ColoredBalance recipientColoredBalance = AccountProperties.getColoredBalance(recipient);
		if (recipientColoredBalance == null) {
			recipientColoredBalance = new ColoredBalance();
		}
		recipientColoredBalance.refill(amount, color);
		AccountProperties.setColoredBalance(recipient, recipientColoredBalance);
		ledger.putAccount(recipient);

	}

}
