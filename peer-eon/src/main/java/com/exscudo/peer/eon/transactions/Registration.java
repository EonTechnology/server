package com.exscudo.peer.eon.transactions;

import java.util.HashMap;

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
}
