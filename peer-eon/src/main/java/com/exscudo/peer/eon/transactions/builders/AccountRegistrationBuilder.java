package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;

/**
 * "Registration" transaction.
 * <p>
 * Publication of the public key. The public key is used to form the recipient
 * field.
 */
public class AccountRegistrationBuilder extends TransactionBuilder<AccountRegistrationBuilder> {

	private AccountRegistrationBuilder() {
		super(TransactionType.AccountRegistration);
	}

	public static AccountRegistrationBuilder createNew(byte[] publicKey) {
		return new AccountRegistrationBuilder().withParam(Format.ID.accountId(Format.MathID.pick(publicKey)),
				Format.convert(publicKey));
	}

}
