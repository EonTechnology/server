package com.exscudo.peer.eon.transactions.handlers;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class ColoredCoinRegistrationHandler implements ITransactionHandler {

	@Override
	public void run(Transaction tx, ILedger ledger, TransactionContext context) throws ValidateException {

		long moneySupply = Long.parseLong(tx.getData().get("emission").toString());
		int decimalPoint = Integer.parseInt(tx.getData().get("decimalPoint").toString());

		IAccount account = ledger.getAccount(tx.getSenderID());

		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setMoneySupply(moneySupply);
		coloredCoin.setDecimalPoint(decimalPoint);
		// Sets only on money creation
		coloredCoin.setTimestamp(context.timestamp);

		AccountProperties.setColoredCoinRegistrationData(account, coloredCoin);

		ColoredBalance coloredBalance = AccountProperties.getColoredBalance(account);
		if (coloredBalance == null) {
			coloredBalance = new ColoredBalance();
		}
		coloredBalance.setBalance(moneySupply, tx.getSenderID());
		AccountProperties.setColoredBalance(account, coloredBalance);

		ledger.putAccount(account);

	}

}
