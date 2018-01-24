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

public class ColoredCoinSupplyHandler implements ITransactionHandler {

	@Override
	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

		IAccount sender = ledger.getAccount(transaction.getSenderID());
		ColoredCoin coin = AccountProperties.getColoredCoinRegistrationData(sender);
		ColoredBalance balance = AccountProperties.getColoredBalance(sender);
		if (balance == null) {
			balance = new ColoredBalance();
		}

		long coinID = transaction.getSenderID();
		long newMoneySupply = Long.parseLong(String.valueOf(transaction.getData().get("moneySupply")));
		long newBalance = balance.getBalance(coinID) + (newMoneySupply - coin.getMoneySupply());

		coin.setMoneySupply(newMoneySupply);
		balance.setBalance(newBalance, coinID);

		AccountProperties.setColoredCoinRegistrationData(sender, coin);
		AccountProperties.setColoredBalance(sender, balance);
		ledger.putAccount(sender);

	}

}
