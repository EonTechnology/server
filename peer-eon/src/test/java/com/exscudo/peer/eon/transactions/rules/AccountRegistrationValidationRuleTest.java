package com.exscudo.peer.eon.transactions.rules;

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
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.transactions.Registration;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AccountRegistrationValidationRuleTest extends AbstractValidationRuleTest {
	private AccountRegistrationValidationRule rule = new AccountRegistrationValidationRule();

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
	public void account_re_registration() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Account already exists.");

		AccountProperties.setBalance(sender, new Balance(1000L));
		Transaction tx = Registration.newAccount(senderSigner.getPublicKey()).build(senderSigner);
		validate(tx);
	}

	@Test
	public void account_invalid_attachment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		Transaction tx = spy(Registration.newAccount(new byte[0]).build(senderSigner));
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("data", "test");
		when(tx.getData()).thenReturn(map);
		resolveSignature(tx);

		validate(tx);
	}

	private void resolveSignature(Transaction tx) {
		byte[] bytes = tx.getBytes();
		byte[] signature = senderSigner.sign(bytes);
		tx.setSignature(signature);
	}
}
