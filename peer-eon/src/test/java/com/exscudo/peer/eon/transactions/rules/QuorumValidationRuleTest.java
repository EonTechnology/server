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
import com.exscudo.peer.eon.transactions.builders.QuorumBuilder;
import com.exscudo.peer.eon.transactions.builders.TransactionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class QuorumValidationRuleTest extends AbstractValidationRuleTest {
	private QuorumValidationRule rule = new QuorumValidationRule();

	private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
	private ISigner delegate_1 = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
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
		validationMode.setWeightForAccount(Format.MathID.pick(delegate_1.getPublicKey()), 10);
		AccountProperties.setValidationMode(senderAccount, validationMode);

		Account delegateAccount1 = new Account(Format.MathID.pick(delegate_1.getPublicKey()));
		AccountProperties.setRegistrationData(delegateAccount1, new RegistrationData(delegate_1.getPublicKey()));

		ledger.putAccount(senderAccount);
		ledger.putAccount(delegateAccount1);
	}

	@Test
	public void unknown_sender() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		when(ledger.getAccount(Format.MathID.pick(sender.getPublicKey()))).thenReturn(null);
		Transaction tx = QuorumBuilder.createNew(70).build(sender);
		validate(tx);
	}

	@Test
	public void unset_default_quorum() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, new HashMap<>()).build(sender);
		validate(tx);
	}

	@Test
	public void quorum_not_int() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", 50);
		map.put("*", 70);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void default_quorum_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal quorum.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", ValidationMode.MAX_QUORUM + 1);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void default_quorum_out_of_range_1() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal quorum.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", ValidationMode.MIN_QUORUM - 1);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void default_quorum_invalid_value() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unable to set quorum.");

		Transaction tx = QuorumBuilder.createNew(90).build(sender);
		validate(tx);
	}

	@Test
	public void quorum_for_unknown_transaction_type() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown transaction type 100500");

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", 50);
		map.put(String.valueOf(TransactionType.OrdinaryPayment), 70);
		map.put(String.valueOf(100500), 30);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void quorum_for_all_typed() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Use all quorum for transaction type 200");

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", 50);
		map.put(String.valueOf(TransactionType.OrdinaryPayment), 50);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void quorum_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal quorum for transaction type " + TransactionType.OrdinaryPayment);

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", 50);
		map.put(String.valueOf(TransactionType.OrdinaryPayment), ValidationMode.MAX_QUORUM + 1);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void quorum_out_of_range_1() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal quorum for transaction type " + TransactionType.OrdinaryPayment);

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", 50);
		map.put(String.valueOf(TransactionType.OrdinaryPayment), ValidationMode.MIN_QUORUM - 1);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

	@Test
	public void quorum_invalid_value() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unable to set quorum for transaction type " + TransactionType.OrdinaryPayment);

		HashMap<String, Object> map = new HashMap<>();
		map.put("all", 50);
		map.put(String.valueOf(TransactionType.OrdinaryPayment), 90);

		Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
		validate(tx);
	}

}
