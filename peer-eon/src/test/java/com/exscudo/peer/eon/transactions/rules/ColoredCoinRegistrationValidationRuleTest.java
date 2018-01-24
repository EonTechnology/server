package com.exscudo.peer.eon.transactions.rules;

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
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.transactions.builders.ColoredCoinRegistrationBuilder;
import com.exscudo.peer.eon.transactions.builders.TransactionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinRegistrationValidationRuleTest extends AbstractValidationRuleTest {
	private ColoredCoinRegistrationValidationRule rule = new ColoredCoinRegistrationValidationRule();

	private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
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

		ledger.putAccount(senderAccount);
	}

	@Test
	public void invalid_attachment() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		HashMap<String, Object> map = new HashMap<>();
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
		validate(tx);
	}

	@Test
	public void invalid_emission_format() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'emission' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("emission", "werwerwer");
		map.put("decimalPoint", 2);
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
		validate(tx);
	}

	@Test
	public void invalid_emission_format_1() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'emission' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("emission", "18446744073709551616");
		map.put("decimalPoint", 2);
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
		validate(tx);
	}

	@Test
	public void emission_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'emission' field value out of range.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("emission", -1);
		map.put("decimalPoint", 2);
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
		validate(tx);
	}

	@Test
	public void decimal_point_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'decimalPoint' field value is out of range.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("emission", 100L);
		map.put("decimalPoint", 100L);
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
		validate(tx);
	}

	@Test
	public void decimal_point_not_specified() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'decimalPoint' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("emission", 100L);
		map.put("field", 100L);
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinRegistration, map).build(sender);
		validate(tx);
	}

	@Test
	public void re_enable() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Account is already associated with a color coin.");

		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setMoneySupply(1000L);
		AccountProperties.setColoredCoinRegistrationData(senderAccount, coloredCoin);

		Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(sender);
		validate(tx);
	}

	@Test
	public void success() throws Exception {
		Transaction tx = ColoredCoinRegistrationBuilder.createNew(1000000L, 1).build(sender);
		validate(tx);
	}

}
