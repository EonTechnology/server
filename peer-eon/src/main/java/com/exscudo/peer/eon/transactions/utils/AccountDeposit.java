package com.exscudo.peer.eon.transactions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.EonConstant;

/**
 * Deposit account manager
 */
public class AccountDeposit {
	public static final UUID ID = UUID.fromString("256d84f8-b272-4dcc-a7de-e36b7b8a0da6");

	private final long accountID;
	private long deposit;
	private int height;

	public AccountDeposit(long accountID, long deposit, int height) {
		this.accountID = accountID;
		this.deposit = deposit;
		this.height = height;
	}

	public long getValue() {
		return deposit;
	}

	public int getHeight() {
		return height;
	}

	public void refill(long amount) {
		long newDeposit = deposit + amount;
		if (amount <= 0 || newDeposit < 0 || newDeposit > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException();
		}
		deposit = newDeposit;
	}

	public void withdraw(long amount) {
		long newDeposit = deposit - amount;
		if (deposit <= 0 || amount <= 0 || newDeposit < 0 || amount > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException();
		}
		deposit = newDeposit;
	}

	public AccountProperty asProperty() {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("amount", deposit);

		return new AccountProperty(accountID, ID, data);
	}

	public static AccountDeposit parse(IAccount account) {
		AccountProperty prop = account.getProperty(ID);
		if (prop == null) {
			return new AccountDeposit(account.getID(), 0L, 0);
		}

		long value = 0;
		Map<String, Object> data = prop.getData();
		Object amountObj = data.get("amount");
		if (amountObj instanceof Long || amountObj instanceof Integer) {
			value = Long.parseLong(amountObj.toString());
		}
		return new AccountDeposit(account.getID(), value, prop.getHeight());
	}

	public static void refill(IAccount account, long amount) {
		AccountDeposit deposit = AccountDeposit.parse(account);
		deposit.refill(amount);
		account.putProperty(deposit.asProperty());
	}

	public static void withdraw(IAccount account, long amount) {
		AccountDeposit deposit = AccountDeposit.parse(account);
		deposit.withdraw(amount);
		account.putProperty(deposit.asProperty());
	}

	public static long getDeposit(IAccount account) {
		return parse(account).getValue();
	}

	public static void setDeposit(IAccount account, long deposit, int height) {
		if (deposit < 0 || deposit > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("Illegal balance.");
		}
		account.putProperty(new AccountDeposit(account.getID(), deposit, height).asProperty());
	}

}
