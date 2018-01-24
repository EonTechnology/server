package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.eon.transactions.builders.PaymentBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class OrdinaryPaymentValidationRuleTest extends AbstractValidationRuleTest {
	private OrdinaryPaymentValidationRule rule = new OrdinaryPaymentValidationRule();

	private ISigner senderSigner = new Ed25519Signer(
			"112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
	private IAccount sender;

	private ISigner recipientSigner = new Ed25519Signer(
			"00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
	private IAccount recipient;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		sender = Mockito.spy(new Account(Format.MathID.pick(senderSigner.getPublicKey())));
		AccountProperties.setRegistrationData(sender, new RegistrationData(senderSigner.getPublicKey()));

		recipient = Mockito.spy(new Account(Format.MathID.pick(recipientSigner.getPublicKey())));
		AccountProperties.setRegistrationData(recipient, new RegistrationData(recipientSigner.getPublicKey()));

		ledger = spy(new DefaultLedger());
		ledger.putAccount(sender);
		ledger.putAccount(recipient);
	}

	@Test
	public void payment_invalid_sender() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		when(ledger.getAccount(eq(Format.MathID.pick(senderSigner.getPublicKey())))).thenReturn(null);

		Transaction tx = PaymentBuilder.createNew(100L, 12345L).forFee(1L).validity(timeProvider.get(), (short) 60, 1)
				.build(senderSigner);
		validate(tx);
	}

	@Test
	public void payment_invalid_recipient() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown recipient.");

		Transaction tx = PaymentBuilder.createNew(100L, 12345L).forFee(1L).validity(timeProvider.get(), (short) 60, 1)
				.build(senderSigner);
		validate(tx);
	}

	@Test
	public void payment_invalid_attachment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(PaymentBuilder.createNew(100L, 12345L).forFee(1L)
				.validity(timeProvider.get(), (short) 60, 1).build(senderSigner));
		when(tx.getData()).thenReturn(new HashMap<>());
		resolveSignature(tx);

		validate(tx);
	}

	@Test
	public void payment_invalid_balance() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		AccountProperties.setBalance(sender, new Balance(103L));

		Transaction tx = PaymentBuilder.createNew(100L, recipient.getID()).forFee(5L)
				.validity(timeProvider.get(), (short) 60, 1).build(senderSigner);
		validate(tx);
	}

	private void resolveSignature(Transaction tx) {
		byte[] bytes = tx.getBytes();
		byte[] signature = senderSigner.sign(bytes);
		tx.setSignature(signature);
	}
}
