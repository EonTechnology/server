package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.Mockito.spy;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.crypto.SignedObjectMapper;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.Ed25519SignatureVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class AbstractValidationRuleTest {
	protected TimeProvider timeProvider = spy(new TimeProvider());
	protected DefaultLedger ledger = spy(new DefaultLedger());

	protected abstract IValidationRule getValidationRule();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		Ed25519SignatureVerifier signatureVerifier = new Ed25519SignatureVerifier();
		CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(0L));
		cryptoProvider.addProvider(signatureVerifier);
		cryptoProvider.setDefaultProvider(signatureVerifier.getName());
		CryptoProvider.init(cryptoProvider);
	}

	protected void validate(Transaction tx) throws Exception {
		ValidationResult r = getValidationRule().validate(tx, ledger, new TransactionContext(timeProvider.get(), 0));
		if (r.hasError) {
			throw r.cause;
		}
	}
}
