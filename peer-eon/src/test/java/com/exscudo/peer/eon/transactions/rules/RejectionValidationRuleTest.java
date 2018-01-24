package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.Mockito.when;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.state.Voter;
import com.exscudo.peer.eon.state.serialization.PropertyType;
import com.exscudo.peer.eon.transactions.builders.RejectionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RejectionValidationRuleTest extends AbstractValidationRuleTest {
	private ISigner base = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
	private ISigner delegate = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
	private ISigner delegate_1 = new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

	private RejectionValidationRule rule = new RejectionValidationRule();
	private IAccount baseAccount;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		long baseAccountID = Format.MathID.pick(base.getPublicKey());
		long delegateID = Format.MathID.pick(delegate.getPublicKey());
		baseAccount = Mockito.spy(new Account(baseAccountID));
		AccountProperties.setRegistrationData(baseAccount, new RegistrationData(base.getPublicKey()));

		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(60);
		validationMode.setWeightForAccount(delegateID, 5);
		validationMode.setQuorum(50);
		validationMode.setQuorum(TransactionType.OrdinaryPayment, 70);
		AccountProperties.setValidationMode(baseAccount, validationMode);

		Account delegateAccount = new Account(delegateID);
		AccountProperties.setRegistrationData(delegateAccount, new RegistrationData(delegate.getPublicKey()));
		Voter voter = new Voter();
		voter.setPoll(baseAccountID, 5);
		AccountProperties.setVoter(delegateAccount, voter);

		Account delegateAccount1 = new Account(Format.MathID.pick(delegate_1.getPublicKey()));
		AccountProperties.setRegistrationData(delegateAccount, new RegistrationData(delegate_1.getPublicKey()));

		ledger.putAccount(baseAccount);
		ledger.putAccount(delegateAccount);
		ledger.putAccount(delegateAccount1);
	}

	@Test
	public void unknown_account() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");
		when(ledger.getAccount(Format.MathID.pick(delegate.getPublicKey()))).thenReturn(null);

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
		validate(tx);
	}

	@Test
	public void target_account_not_exist() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown account.");
		when(ledger.getAccount(baseAccount.getID())).thenReturn(null);

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
		validate(tx);
	}

	@Test
	public void target_account_not_in_mfa() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The delegates list is not specified.");
		when(baseAccount.getProperty(PropertyType.MODE)).thenReturn(null);

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
		validate(tx);
	}

	@Test
	public void unknown_delegate() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Account does not participate in transaction confirmation.");

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate_1);
		validate(tx);
	}

	@Test
	public void reject() throws Exception {
		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
		validate(tx);
	}

	@Test
	public void reject_itself() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal account.");

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(base);
		validate(tx);
	}

	@Test
	public void rejection_impossible() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Rejection is not possible.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setPublicMode("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
		validationMode.setWeightForAccount(Format.MathID.pick(delegate.getPublicKey()), 70);
		AccountProperties.setValidationMode(baseAccount, validationMode);

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
		validate(tx);
	}

	@Test
	public void rejection_impossible_1() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Rejection is not possible.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setWeightForAccount(Format.MathID.pick(delegate.getPublicKey()), 70);
		AccountProperties.setValidationMode(baseAccount, validationMode);

		Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
		validate(tx);
	}

}
