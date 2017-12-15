package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.transactions.Deposit;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DepositRefillValidationRuleTest extends AbstractValidationRuleTest {
	private DepositRefillValidationRule rule = new DepositRefillValidationRule();

	private ISigner senderSigner = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
	private IAccount sender;

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

		ledger = spy(new DefaultLedger());
		ledger.putAccount(sender);
	}

	@Test
	public void deposit_refill_with_invalid_fee_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The field value Fee is not valid.");

		Transaction tx = spy(Deposit.refill(999L).build(senderSigner));
		when(tx.getFee()).thenReturn(5L);
		resolveSignature(tx);

		validate(tx);
	}

	@Test
	public void deposit_refill_with_invalid_attachment_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(Deposit.refill(999L).build(senderSigner));
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("amount", "test");
		when(tx.getData()).thenReturn(map);
		resolveSignature(tx);

		validate(tx);
	}

	@Test
	public void deposit_refill_with_unknown_sender_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		Transaction tx = Deposit.refill(1L).build(senderSigner);
		when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(null);
		validate(tx);
	}

	@Test
	public void deposit_refill_with_low_balance_is_error() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");

		long depositAmount = 999L;
		AccountProperties.setBalance(sender, new Balance(depositAmount + Deposit.DEPOSIT_TRANSACTION_FEE - 1));
		AccountProperties.setDeposit(sender, new GeneratingBalance(0L, 0));
		Transaction tx = Deposit.refill(depositAmount).build(senderSigner);

		validate(tx);
	}

	@Test
	public void deposit_refill_with_arbitrary_amount_is_ok() throws Exception {
		long refillAmount = 900;
		AccountProperties.setBalance(sender, new Balance(refillAmount + Deposit.DEPOSIT_TRANSACTION_FEE));
		Transaction tx = Deposit.refill(refillAmount).build(senderSigner);

		validate(tx);
	}

	@Test
	public void deposit_refill_with_positive_deposit_is_ok() throws Exception {

		long refillAmount = 100500;
		long depositAmount = 1;

		AccountProperties.setBalance(sender, new Balance(refillAmount + Deposit.DEPOSIT_TRANSACTION_FEE));
		AccountProperties.setDeposit(sender, new GeneratingBalance(depositAmount, 0));
		Transaction tx = Deposit.refill(refillAmount).build(senderSigner);

		validate(tx);

	}

	private void resolveSignature(Transaction tx) {
		byte[] bytes = tx.getBytes();
		byte[] signature = senderSigner.sign(bytes);
		tx.setSignature(signature);
	}

}
