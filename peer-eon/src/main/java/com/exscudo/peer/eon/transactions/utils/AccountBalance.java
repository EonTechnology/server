package com.exscudo.peer.eon.transactions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.EonConstant;

/**
 * Balance account manager
 */
public class AccountBalance {
	public static final UUID ID = UUID.fromString("5afe47d6-6233-11e7-907b-a6006ad3dba0");

	private final long accountID;
	private long value;

	public AccountBalance(long accountID, long balance) {
		this.value = balance;
		this.accountID = accountID;
	}

	public long getValue() {
		return value;
	}

	public void refill(long amount) {
		long balance = value + amount;
		if (balance < 0 || balance > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("Illegal balance.");
		}
		value = balance;
	}

	public void withdraw(long amount) {
		long balance = value - amount;
		if (balance < 0 || balance > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("Illegal balance.");
		}
		value = balance;
	}

	public AccountProperty asProperty() {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("amount", value);
		return new AccountProperty(accountID, ID, data);
	}

	private static AccountBalance parse(IAccount account) {
		AccountProperty prop = account.getProperty(ID);
		long balance = 0;
		if (prop != null) {
			Map<String, Object> data = prop.getData();
			Object amountObj = data.get("amount");
			if (amountObj instanceof Long || amountObj instanceof Integer) {
				balance = Long.parseLong(amountObj.toString());
			}
		}
		return new AccountBalance(account.getID(), balance);
	}

	public static void setBalance(IAccount account, long balance) {
		if (balance < 0 || balance > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("Illegal balance.");
		}
		account.putProperty(new AccountBalance(account.getID(), balance).asProperty());
	}

	public static long getBalance(IAccount account) {
		return AccountBalance.parse(account).getValue();
	}

	public static void refill(IAccount account, long amount) {
		AccountBalance balance = parse(account);
		balance.refill(amount);
		account.putProperty(balance.asProperty());
	}

	public static void withdraw(IAccount account, long amount) {
		AccountBalance balance = parse(account);
		balance.withdraw(amount);
		account.putProperty(balance.asProperty());
	}

}
