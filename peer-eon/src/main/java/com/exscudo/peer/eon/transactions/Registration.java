package com.exscudo.peer.eon.transactions;

import java.util.HashMap;
import java.util.Objects;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;

/**
 * "Registration" transaction.
 * <p>
 * Publication of the public key. The public key is used to form the recipient
 * field.
 */
public class Registration {

	public static TransactionBuilder newAccount(byte[] publicKey) {
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put(Format.ID.accountId(Format.MathID.pick(publicKey)), Format.convert(publicKey));

		return new TransactionBuilder(TransactionType.AccountRegistration, hashMap);
	}

	/**
	 * Creates an public account.
	 * <p>
	 * ATTENTION: see {@link TransactionType#AccountPublication} description.
	 * 
	 * @param seed
	 * @return
	 */
	public static TransactionBuilder newPublicAccount(String seed) {
		Objects.requireNonNull(seed);

		HashMap<String, Object> map = new HashMap<>();
		map.put("seed", seed);
		return new TransactionBuilder(TransactionType.AccountPublication, map);
	}
}
