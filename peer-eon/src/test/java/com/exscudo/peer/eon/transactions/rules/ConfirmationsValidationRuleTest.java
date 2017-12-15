package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.Payment;
import com.exscudo.peer.eon.transactions.Registration;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ConfirmationsValidationRuleTest extends AbstractValidationRuleTest {
	private ConfirmationsValidationRule rule = new ConfirmationsValidationRule();

	private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
	private ISigner delegate_1 = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
	private ISigner delegate_2 = new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

	private IAccount senderAccount;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		senderAccount = Mockito.spy(new Account(Format.MathID.pick(sender.getPublicKey())));
		AccountProperties.setRegistrationData(senderAccount, new RegistrationData(sender.getPublicKey()));
		AccountProperties.setBalance(senderAccount, new Balance(5000L));

		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(60);
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 5);
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_2.getPublicKey()), 15);
		validationMode.setQuorum(50);
		validationMode.setQuorum(TransactionType.OrdinaryPayment, 70);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		Account delegateAccount1 = new Account(Format.MathID.pick(delegate_1.getPublicKey()));
		AccountProperties.setRegistrationData(delegateAccount1, new RegistrationData(delegate_1.getPublicKey()));

		Account delegateAccount2 = new Account(Format.MathID.pick(delegate_2.getPublicKey()));
		AccountProperties.setRegistrationData(delegateAccount2, new RegistrationData(delegate_2.getPublicKey()));

		ledger.putAccount(senderAccount);
		ledger.putAccount(delegateAccount1);
		ledger.putAccount(delegateAccount2);
	}

	@Test
	public void unset_mfa_property() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid use of the confirmation field.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(ValidationMode.MAX_WEIGHT);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		Transaction tx = Registration.newAccount(new byte[32]).build(sender, new ISigner[] { delegate_1, delegate_2 });
		validate(tx);

	}

	@Test
	public void duplicate_confirmation() throws Exception {

		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Duplicates.");

		Transaction tx = Registration.newAccount(new byte[32]).build(sender, new ISigner[] { sender });
		validate(tx);

	}

	@Test
	public void unknown_delegate() throws Exception {

		long id = Format.MathID.pick(delegate_1.getPublicKey());
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown account " + Format.ID.accountId(id));

		when(ledger.getAccount(id)).thenReturn(null);

		Transaction tx = Registration.newAccount(new byte[32]).build(sender, new ISigner[] { delegate_1 });
		validate(tx);

	}

	@Test
	public void unspecified_delegate() throws Exception {

		long id = Format.MathID.pick(delegate_2.getPublicKey());
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Account '" + Format.ID.accountId(id) + "' can not sign transaction.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(60);
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 20);
		validationMode.setQuorum(50);
		validationMode.setQuorum(TransactionType.OrdinaryPayment, 70);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		Transaction tx = Registration.newAccount(new byte[32]).build(sender, new ISigner[] { delegate_1, delegate_2 });
		validate(tx);

	}

	@Test
	public void illegal_confirmation() throws Exception {
		expectedException.expect(IllegalSignatureException.class);

		Transaction tx = Registration.newAccount(new byte[32]).build(sender, new ISigner[] { delegate_1, delegate_2 });
		String key = tx.getConfirmations().keySet().iterator().next();
		tx.getConfirmations().put(key, Format.convert(new byte[32]));
		validate(tx);

	}

	@Test
	public void invalid_quorum_without_confirmation() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The quorum is not exist.");

		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(12345L)).thenReturn(mockAccount);

		Transaction tx = Payment.newPayment(100, 12345L).build(sender, new ISigner[] {});
		validate(tx);
	}

	@Test
	public void invalid_quorum_with_confirmation() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The quorum is not exist.");

		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(12345L)).thenReturn(mockAccount);

		Transaction tx = Payment.newPayment(100, 12345L).build(sender, new ISigner[] { delegate_1 });
		validate(tx);
	}

	@Test
	public void quorum_with_confirmation() throws Exception {
		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(12345L)).thenReturn(mockAccount);

		Transaction tx = Payment.newPayment(100, 12345L).build(sender, new ISigner[] { delegate_1, delegate_2 });
		validate(tx);
	}

	@Test
	public void quorum_with_partial_confirmation() throws Exception {

		IAccount mockAccount = mock(IAccount.class);
		when(ledger.getAccount(12345L)).thenReturn(mockAccount);

		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(60);
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 5);
		validationMode.setQuorum(50);
		validationMode.setQuorum(TransactionType.OrdinaryPayment, 70);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		Transaction tx = Payment.newPayment(100, 12345L).build(sender, new ISigner[] { delegate_1 });
		validate(tx);
	}

	@Test
	public void quorum_without_confirmation() throws Exception {
		Transaction tx = Registration.newAccount(new byte[32]).build(sender, new ISigner[] {});
		validate(tx);
	}

}
