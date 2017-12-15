package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.Constant;
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
import com.exscudo.peer.eon.transactions.Registration;
import com.exscudo.peer.eon.transactions.TransactionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AccountPublicationValidationRuleTest extends AbstractValidationRuleTest {
	private AccountPublicationValidationRule rule = new AccountPublicationValidationRule();

	private String seed = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
	private String seed_1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";

	private ISigner signer = new Ed25519Signer(seed);
	private ISigner signer_1 = new Ed25519Signer(seed_1);

	private IAccount account;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		account = Mockito.spy(new Account( Format.MathID.pick(signer.getPublicKey())));
		AccountProperties.setRegistrationData(account, new RegistrationData(signer.getPublicKey()));

		Account account_1 = new Account(Format.MathID.pick(signer_1.getPublicKey()));
		AccountProperties.setRegistrationData(account_1, new RegistrationData(signer_1.getPublicKey()));

		ledger.putAccount(account);
		ledger.putAccount(account_1);
	}

	@Test
	public void illegal_sender() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		Transaction tx = new TransactionBuilder(TransactionType.AccountRegistration, new HashMap<>())
				.build(new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011"));
		validate(tx);
	}

	@Test
	public void invalid_argument_number_in_attach() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = new TransactionBuilder(TransactionType.AccountRegistration, new HashMap<>()).build(signer);
		validate(tx);
	}


	@Test
	public void invalid_seed() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid seed.");

		Map<String, Object> map = new HashMap<>();
		map.put("seed", "seedseedseedseed");
		Transaction tx = new TransactionBuilder(TransactionType.AccountRegistration, map).build(signer);
		validate(tx);
	}

	@Test
	public void invalid_sender() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Seed for sender account must be specified in attachment.");

		Transaction tx = Registration.newPublicAccount(seed).build(signer_1);
		validate(tx);
	}

	@Test
	public void validation_mode_property_undefined() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Invalid use of transaction.");

		Transaction tx = Registration.newPublicAccount(seed).build(signer);
		validate(tx);
	}

	@Test
	public void already_public_mode() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Already public.");

		ValidationMode validationMode = new ValidationMode();
		validationMode.setPublicMode(seed);
		validationMode.setWeightForAccount(Format.MathID.pick(signer_1.getPublicKey()), 100);
		AccountProperties.setValidationMode(account, validationMode);

		Transaction tx = Registration.newPublicAccount(seed).build(signer);
		validate(tx);
	}

	@Test
	public void invalid_uses_too_early() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The confirmation mode were changed earlier than a day ago."
				+ " Do not use this seed more for personal operations.");

		int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

		ValidationMode validationMode = new ValidationMode();
		validationMode.setWeightForAccount(Format.MathID.pick(signer_1.getPublicKey()), 100);
		validationMode.setTimestamp(timestamp);
		AccountProperties.setValidationMode(account, validationMode);

		int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
		Transaction tx = Registration.newPublicAccount(seed).validity( timestamp1 - 30 * 60,60 * 60).build(signer);

		when(timeProvider.get()).thenReturn(timestamp1 - 1);
		validate(tx);
	}

	@Test
	public void invalid_uses_empty_delegate_list() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal validation mode."
				+ " Do not use this seed more for personal operations.");

		int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

		ValidationMode validationMode = new ValidationMode();
		validationMode.setTimestamp(timestamp);
		AccountProperties.setValidationMode(account, validationMode);

		int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
		Transaction tx = Registration.newPublicAccount(seed).validity( timestamp1 - 30 * 60,60 * 60).build(signer);

		when(timeProvider.get()).thenReturn(timestamp1 + 1);
		validate(tx);
	}

	@Test
	public void invalid_uses_sender_weight_not_equal_zero() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal validation mode."
				+ " Do not use this seed more for personal operations.");

		int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

		ValidationMode validationMode = new ValidationMode();
		validationMode.setBaseWeight(70);
		validationMode.setWeightForAccount(Format.MathID.pick(signer_1.getPublicKey()), 100);
		validationMode.setTimestamp(timestamp);
		AccountProperties.setValidationMode(account, validationMode);

		int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
		Transaction tx = Registration.newPublicAccount(seed).validity( timestamp1 - 30 * 60,60 * 60).build(signer);

		when(timeProvider.get()).thenReturn(timestamp1 + 1);
		validate(tx);
	}

	@Test
	public void account_is_delegate() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("A public account must not confirm transactions of other accounts."
				+ " Do not use this seed more for personal operations.");

		int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

		ValidationMode validationMode = new ValidationMode();
		validationMode.setWeightForAccount(Format.MathID.pick(signer_1.getPublicKey()), 100);
		validationMode.setTimestamp(timestamp);
		AccountProperties.setValidationMode(account, validationMode);
		Voter voter = new Voter();
		voter.setPoll(Format.MathID.pick(signer_1.getPublicKey()), 10);
		AccountProperties.setVoter(account, voter);

		int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
		Transaction tx = Registration.newPublicAccount(seed).validity( timestamp1 - 30 * 60,60 * 60).build(signer);

		when(timeProvider.get()).thenReturn(timestamp1 + 1);
		validate(tx);
	}

	@Test
	public void success() throws Exception {
		int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

		ValidationMode validationMode = new ValidationMode();
		validationMode.setWeightForAccount(Format.MathID.pick(signer_1.getPublicKey()), 100);
		validationMode.setTimestamp(timestamp);
		AccountProperties.setValidationMode(account, validationMode);

		int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
		Transaction tx = Registration.newPublicAccount(seed).validity( timestamp1 - 30 * 60,60 * 60).build(signer);

		when(timeProvider.get()).thenReturn(timestamp1 + 1);
		validate(tx);
	}


}
