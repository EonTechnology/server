package com.exscudo.peer.eon.transactions.rules;

import java.util.HashMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import org.junit.Before;
import org.junit.Test;

public class AttachmentValidationRuleTest extends AbstractValidationRuleTest {
	private AttachmentValidationRule rule = new AttachmentValidationRule();
	private Transaction tx;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		ISigner signer = new Ed25519Signer(
				"112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

		tx = new Transaction();
		tx.setType(10);
		tx.setVersion(1);
		tx.setTimestamp(timeProvider.get());
		tx.setDeadline((short) 60);
		tx.setReference(0);
		tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
		tx.setFee(1L);
		tx.setData(new HashMap<>());
		tx.setSignature(signer.sign(tx.getBytes()));
	}

	@Test
	public void unknown_transaction_type() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid transaction type. Type :10");

		validate(tx);
	}

	@Test
	public void success() throws Exception {
		rule.bind(10, new IValidationRule() {
			@Override
			public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {
				return ValidationResult.success;
			}
		});

		validate(tx);
	}
}
