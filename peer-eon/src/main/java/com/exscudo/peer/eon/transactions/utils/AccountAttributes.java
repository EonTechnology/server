package com.exscudo.peer.eon.transactions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;

/**
 * Account properties manager
 */
public class AccountAttributes {
	public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private byte[] publicKey;

	public AccountAttributes(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public AccountProperty asProperty() {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("publicKey", Format.convert(publicKey));
		return new AccountProperty(ID, data);
	}

	private static AccountAttributes parse(AccountProperty p) {
		Objects.requireNonNull(p);
		Map<String, Object> data = p.getData();
		Object amountObj = data.get("publicKey");

		return new AccountAttributes(Format.convert(amountObj.toString()));
	}

	private static AccountAttributes parse(IAccount account) {
		AccountProperty p = account.getProperty(ID);
		return parse(p);
	}

	public static void setPublicKey(IAccount account, byte[] publicKey) {
		Objects.requireNonNull(publicKey);
		account.putProperty(new AccountAttributes(publicKey).asProperty());
	}

	public static byte[] getPublicKey(IAccount account) {
		return AccountAttributes.parse(account).getPublicKey();
	}

}
