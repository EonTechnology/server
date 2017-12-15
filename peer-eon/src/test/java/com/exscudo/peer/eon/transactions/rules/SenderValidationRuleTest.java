package com.exscudo.peer.eon.transactions.rules;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISignatureVerifier;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SenderValidationRuleTest extends AbstractValidationRuleTest {
	private SenderValidationRule rule = new SenderValidationRule();
	private Transaction transaction;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		IAccount account = Mockito.spy(new Account(Format.MathID.pick(new byte[32])));
		AccountProperties.setRegistrationData(account, new RegistrationData(new byte[32]));
		AccountProperties.setBalance(account, new Balance(5L));
		when(ledger.getAccount(anyLong())).thenReturn(account);

		transaction = new Transaction();
		transaction.setFee(1L);
		transaction.setVersion(1);
		transaction.setSenderID(1L);
		transaction.setDeadline((short) 1);
		transaction.setTimestamp(1000);
	}

	@Test
	public void validate_fee_too_mach() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Not enough funds.");
		transaction.setFee(100L);

		validate(transaction);
	}

	@Test
	public void validate_sender_unknown() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown sender.");

		when(ledger.getAccount(anyLong())).thenReturn(null);
		validate(transaction);
	}

	@Test
	public void validate_sender_illegal_signature() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Illegal signature.");

		CryptoProvider.getInstance().addProvider(new DummyVerifier(false));
		CryptoProvider.getInstance().setDefaultProvider("test");

		validate(transaction);
	}

	private static class DummyVerifier implements ISignatureVerifier {
		private final boolean value;

		public DummyVerifier(boolean value) {
			this.value = value;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
			return value;
		}
	}

}
