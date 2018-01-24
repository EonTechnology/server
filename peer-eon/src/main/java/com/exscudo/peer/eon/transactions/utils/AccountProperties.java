package com.exscudo.peer.eon.transactions.utils;

import java.io.IOException;
import java.util.Objects;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.AccountPropertyMapper;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.state.Voter;
import com.exscudo.peer.eon.state.serialization.BalancePropertyDeserializer;
import com.exscudo.peer.eon.state.serialization.BalancePropertySerializer;
import com.exscudo.peer.eon.state.serialization.ColoredBalanceDeserializer;
import com.exscudo.peer.eon.state.serialization.ColoredBalanceSerializer;
import com.exscudo.peer.eon.state.serialization.ColoredCoinDeserializer;
import com.exscudo.peer.eon.state.serialization.ColoredCoinSerializer;
import com.exscudo.peer.eon.state.serialization.GeneratingBalancePropertyDeserializer;
import com.exscudo.peer.eon.state.serialization.GeneratingBalancePropertySerializer;
import com.exscudo.peer.eon.state.serialization.RegistrationDataPropertyDeserializer;
import com.exscudo.peer.eon.state.serialization.RegistrationDataPropertySerializer;
import com.exscudo.peer.eon.state.serialization.ValidationModePropertyDeserializer;
import com.exscudo.peer.eon.state.serialization.ValidationModePropertySerializer;
import com.exscudo.peer.eon.state.serialization.VoterPropertyDeserializer;
import com.exscudo.peer.eon.state.serialization.VoterPropertySerializer;

public class AccountProperties {

	private static AccountPropertyMapper properties = new AccountPropertyMapper()
			.addDeserializer(Balance.class, new BalancePropertyDeserializer())
			.addDeserializer(GeneratingBalance.class, new GeneratingBalancePropertyDeserializer())
			.addDeserializer(RegistrationData.class, new RegistrationDataPropertyDeserializer())
			.addDeserializer(ValidationMode.class, new ValidationModePropertyDeserializer())
			.addDeserializer(Voter.class, new VoterPropertyDeserializer())
			.addDeserializer(ColoredCoin.class, new ColoredCoinDeserializer())
			.addDeserializer(ColoredBalance.class, new ColoredBalanceDeserializer())
			.addSerializer(new BalancePropertySerializer())
			.addSerializer(new GeneratingBalancePropertySerializer())
			.addSerializer(new RegistrationDataPropertySerializer())
			.addSerializer(new ValidationModePropertySerializer())
			.addSerializer(new VoterPropertySerializer())
			.addSerializer(new ColoredCoinSerializer())
			.addSerializer(new ColoredBalanceSerializer());

	static <TValue> TValue get(IAccount account, Class<TValue> clazz) {
		AccountPropertyDeserializer deserializer = properties.findDeserializer(clazz);
		if (deserializer == null) {
			throw new UnsupportedOperationException();
		}
		try {
			return clazz.cast(deserializer.deserialize(account));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static <TValue> void set(IAccount account, TValue value) {
		Objects.requireNonNull(value);

		AccountPropertySerializer<?> serializer = properties.findSerializer(value.getClass());
		if (serializer == null) {
			throw new UnsupportedOperationException();
		}
		try {
			serializer.serialize(value, account);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void setPublicKey(IAccount account, byte[] publicKey) {
		Objects.requireNonNull(publicKey);
		AccountProperties.set(account, new RegistrationData(publicKey));
	}

	public static byte[] getPublicKey(IAccount account) {
		return AccountProperties.get(account, RegistrationData.class).getPublicKey();
	}

	public static void setRegistrationData(IAccount account, RegistrationData rd) {
		AccountProperties.set(account, rd);
	}

	public static void depositRefill(IAccount account, long amount, int height) {
		GeneratingBalance deposit = AccountProperties.get(account, GeneratingBalance.class);
		if (deposit == null) {
			deposit = new GeneratingBalance();
		}
		deposit.refill(amount);
		deposit.setHeight(height);
		AccountProperties.set(account, deposit);
	}

	public static void depositWithdraw(IAccount account, long amount, int height) {
		GeneratingBalance deposit = AccountProperties.get(account, GeneratingBalance.class);
		deposit.withdraw(amount);
		deposit.setHeight(height);
		AccountProperties.set(account, deposit);
	}

	public static GeneratingBalance getDeposit(IAccount account) {
		return AccountProperties.get(account, GeneratingBalance.class);
	}

	public static void setDeposit(IAccount account, GeneratingBalance generatingBalance) {
		set(account, generatingBalance);
	}

	public static Balance getBalance(IAccount account) {
		return AccountProperties.get(account, Balance.class);
	}

	public static void setBalance(IAccount account, Balance balance) {
		AccountProperties.set(account, balance);
	}

	public static void balanceRefill(IAccount account, long amount) {
		Balance balance = AccountProperties.get(account, Balance.class);
		if (balance == null) {
			balance = new Balance(amount);
		} else {
			balance.refill(amount);
		}
		AccountProperties.set(account, balance);
	}

	public static void balanceWithdraw(IAccount account, long amount) {
		Balance balance = AccountProperties.get(account, Balance.class);
		balance.withdraw(amount);
		AccountProperties.set(account, balance);
	}

	public static ValidationMode getValidationMode(IAccount account) {
		return get(account, ValidationMode.class);
	}

	public static void setValidationMode(IAccount account, ValidationMode mode) {
		set(account, mode);
	}

	public static void setVoter(IAccount account, Voter v) {
		set(account, v);
	}

	public static Voter getVoter(IAccount account) {
		return get(account, Voter.class);
	}

	public static ColoredCoin getColoredCoinRegistrationData(IAccount account) {
		ColoredCoin coin = AccountProperties.get(account, ColoredCoin.class);
		return coin;
	}

	public static void setColoredCoinRegistrationData(IAccount account, ColoredCoin coloredCoin) {
		AccountProperties.set(account, coloredCoin);
	}

	public static ColoredBalance getColoredBalance(IAccount account) {
		return AccountProperties.get(account, ColoredBalance.class);
	}

	public static void setColoredBalance(IAccount account, ColoredBalance coloredBalance ) {
		set(account, coloredBalance);
	}
}
