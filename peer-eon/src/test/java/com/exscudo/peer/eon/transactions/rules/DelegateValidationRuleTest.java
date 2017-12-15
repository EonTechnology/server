package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.core.data.Transaction;
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
import com.exscudo.peer.eon.transactions.Delegate;
import com.exscudo.peer.eon.transactions.TransactionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegateValidationRuleTest extends AbstractValidationRuleTest {
	private DelegateValidationRule rule = new DelegateValidationRule();

	private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
	private ISigner delegate_1 = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
	private ISigner delegate_2 = new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
	private IAccount senderAccount;
	private IAccount delegateAccount1;

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
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 50);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		delegateAccount1 = new Account(Format.MathID.pick(delegate_1.getPublicKey()));
		AccountProperties.setRegistrationData(delegateAccount1, new RegistrationData(delegate_1.getPublicKey()));

		ledger.putAccount(senderAccount);
		ledger.putAccount(delegateAccount1);
	}

	@Test
	public void duplicate_account() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Value already set.");

		long id = Format.MathID.pick(sender.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 60).build(sender);
		validate(tx);
	}

	@Test
	public void weight_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage(
				"Invalid " + Format.ID.accountId(Format.MathID.pick(sender.getPublicKey())) + " account weight.");

		long id = Format.MathID.pick(sender.getPublicKey());
		HashMap<String, Object> map = new HashMap<>();
		map.put(Format.ID.accountId(id), ValidationMode.MAX_WEIGHT + 1);
		Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(sender);
		validate(tx);
	}

	@Test
	public void weight_out_of_range_1() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage(
				"Invalid " + Format.ID.accountId(Format.MathID.pick(sender.getPublicKey())) + " account weight.");

		long id = Format.MathID.pick(sender.getPublicKey());
		HashMap<String, Object> map = new HashMap<>();
		map.put(Format.ID.accountId(id), ValidationMode.MIN_WEIGHT - 1);
		Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(sender);
		validate(tx);
	}

	@Test
	public void enable_mfa() throws Exception {
		long id = Format.MathID.pick(sender.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 50).build(sender);
		validate(tx);
	}

	@Test
	public void invalid_weight() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Incorrect distribution of votes.");

		long id = Format.MathID.pick(sender.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 40).build(sender);
		validate(tx);
	}

	@Test
	public void public_mode() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Changing rights is prohibited.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setPublicMode("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 50);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		long id = Format.MathID.pick(sender.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 0).build(sender);
		validate(tx);
	}

	@Test
	public void unknown_sender() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		when(ledger.getAccount(Format.MathID.pick(sender.getPublicKey()))).thenReturn(null);

		long id = Format.MathID.pick(delegate_1.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 50).build(sender);
		validate(tx);
	}

	@Test
	public void unknown_account() throws Exception {
		long id = Format.MathID.pick(delegate_2.getPublicKey());
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown account " + Format.ID.accountId(id));

		Transaction tx = Delegate.addAccount(id, 50).build(sender);
		validate(tx);
	}

	@Test
	public void delegate_invalid_weight() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Incorrect distribution of votes.");

		long id = Format.MathID.pick(delegate_1.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 20).build(sender);
		validate(tx);
	}

	@Test
	public void add_delegate() throws Exception {
		long id = Format.MathID.pick(delegate_1.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 40).build(sender);
		validate(tx);
	}

	@Test
	public void remove_delegate() throws Exception {
		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(60);
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 50);
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_2.getPublicKey()), 50);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		long id = Format.MathID.pick(delegate_1.getPublicKey());
		Transaction tx = Delegate.removeAccount(id).build(sender);
		validate(tx);
	}

	@Test
	public void public_account_as_delegate() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("A public account can not act as a delegate.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setPublicMode("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_2.getPublicKey()), 100);
		AccountProperties.setValidationMode(delegateAccount1, validationMode);

		long id = Format.MathID.pick(delegate_1.getPublicKey());
		Transaction tx = Delegate.addAccount(id, 40).build(sender);
		validate(tx);
	}

}
