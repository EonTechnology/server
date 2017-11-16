package com.exscudo.peer.eon.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISignatureVerifier;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.transactions.rules.BaseValidationRule;
import com.exscudo.peer.eon.transactions.rules.ValidationResult;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;
import org.junit.Before;
import org.junit.Test;

public class BaseValidationRuleTest {

	private TransactionContext validationContext;
	private Transaction transaction;
	private ILedger ledger;

	@Before
	public void setUp() throws Exception {

		Fork fork = mock(Fork.class);
		when(fork.getGenesisBlockID()).thenReturn(0L);
		when(fork.isSupportedTran(any(), anyInt())).thenReturn(true);
		ForkProvider.init(fork);

		ledger = mock(ILedger.class);

		IAccount account = mock(IAccount.class);
		when(account.getProperty(AccountAttributes.ID)).thenReturn(new AccountAttributes(new byte[32]).asProperty());
		when(account.getProperty(AccountBalance.ID)).thenReturn(new AccountBalance(5L).asProperty());
		when(ledger.getAccount(anyLong())).thenReturn(account);

		validationContext = new TransactionContext(1000, 0);

		CryptoProvider.getInstance().addProvider(new ISignatureVerifier() {
			@Override
			public String getName() {
				return "test";
			}

			@Override
			public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
				return true;
			}
		});
		CryptoProvider.getInstance().setDefaultProvider("test");

		transaction = new Transaction();
		transaction.setFee(1L);
		transaction.setSenderID(1L);
		transaction.setDeadline((short) 1);
		transaction.setTimestamp(1000);

	}

	@Test
	public void validate_OK() {
		BaseValidationRule rule = new BaseValidationRule();

		ValidationResult result = rule.validate(transaction, ledger, validationContext);
		assertFalse(result.hasError);
	}

	@Test
	public void validate_experied() {
		BaseValidationRule rule = new BaseValidationRule();

		transaction.setTimestamp(5);
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Invalid timestamp or other params for set the time.");
	}

	@Test
	public void validate_deadline_zero() {
		BaseValidationRule rule = new BaseValidationRule();

		transaction.setDeadline((short) 0);
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Invalid timestamp or other params for set the time.");
	}

	@Test
	public void validate_deadline_max() {
		BaseValidationRule rule = new BaseValidationRule();

		transaction.setDeadline((short) (EonConstant.TRANSACTION_MAX_LIFETIME + 1));
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Invalid timestamp or other params for set the time.");
	}

	@Test
	public void validate_fee_zero() {
		BaseValidationRule rule = new BaseValidationRule();

		transaction.setFee(0L);
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Invalid fee.");
	}

	@Test
	public void validate_fee_max() {
		BaseValidationRule rule = new BaseValidationRule();

		transaction.setFee(EonConstant.MAX_MONEY + 1L);
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Invalid fee.");
	}

	@Test
	public void validate_fee_too_mach() {
		BaseValidationRule rule = new BaseValidationRule();

		transaction.setFee(100L);
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Not enough funds.");
	}

	@Test
	public void validate_sender_unknown() {
		BaseValidationRule rule = new BaseValidationRule();

		when(ledger.getAccount(anyLong())).thenReturn(null);
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Unknown sender.");
	}

	@Test
	public void validate_sender_illegal_signature() {
		BaseValidationRule rule = new BaseValidationRule();

		CryptoProvider.getInstance().addProvider(new ISignatureVerifier() {
			@Override
			public String getName() {
				return "test";
			}

			@Override
			public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
				return false;
			}
		});
		CryptoProvider.getInstance().setDefaultProvider("test");
		ValidationResult result = rule.validate(transaction, ledger, validationContext);

		checkError(result, "Illegal signature.");
	}

	private void checkError(ValidationResult result, String msg) {
		assertTrue(result.hasError);
		assertEquals(msg, result.cause.getMessage());
	}
}
