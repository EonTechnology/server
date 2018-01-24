package com.exscudo.peer.eon.transactions.builders;

import java.util.Objects;

import com.exscudo.peer.eon.TransactionType;

/**
 * Creates an public account.
 * <p>
 * ATTENTION: see {@link TransactionType#AccountPublication} description.
 */
public class AccountPublicationBuilder extends TransactionBuilder<AccountPublicationBuilder> {

	private AccountPublicationBuilder() {
		super(TransactionType.AccountPublication);
	}

	public static AccountPublicationBuilder createNew(String seed) {
		Objects.requireNonNull(seed);
		return new AccountPublicationBuilder().withParam("seed", seed);
	}

}
