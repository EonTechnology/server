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
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.serialization.PropertyType;
import com.exscudo.peer.eon.transactions.builders.ColoredCoinSupplyBuilder;
import com.exscudo.peer.eon.transactions.builders.TransactionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinSupplyValidationRuleTest extends AbstractValidationRuleTest {
	private ColoredCoinSupplyValidationRule rule = new ColoredCoinSupplyValidationRule();

	private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
	private ISigner sender;
	private IAccount senderAccount;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		sender = new Ed25519Signer(SENDER);

		senderAccount = Mockito.spy(new Account(Format.MathID.pick(sender.getPublicKey())));
		AccountProperties.setRegistrationData(senderAccount, new RegistrationData(sender.getPublicKey()));
		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setMoneySupply(50000L);
		AccountProperties.setColoredCoinRegistrationData(senderAccount, coloredCoin);
		ColoredBalance coloredBalance = new ColoredBalance();
		coloredBalance.setBalance(10000L, senderAccount.getID());
		AccountProperties.setColoredBalance(senderAccount, coloredBalance);
		ledger.putAccount(senderAccount);

	}

	@Test
	public void invalid_attach() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		HashMap<String, Object> map = new HashMap<>();
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(sender);
		validate(tx);
	}

	@Test
	public void money_supply_invalid_format() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'moneySupply' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("moneySupply", "moneySupply");
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(sender);
		validate(tx);
	}

	@Test
	public void money_supply_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'moneySupply' field value is out of range.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("moneySupply", -1);
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinSupply, map).build(sender);
		validate(tx);
	}

	@Test
	public void illegal_account_state() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Colored coin is not associated with an account.");

		Mockito.when(senderAccount.getProperty(PropertyType.COLORED_COIN)).thenReturn(null);

		Transaction tx = ColoredCoinSupplyBuilder.createNew(10000L).build(sender);
		validate(tx);
	}

	@Test
	public void money_supply_reset() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Value already set.");

		Transaction tx = ColoredCoinSupplyBuilder.createNew(50000L).build(sender);
		validate(tx);
	}

	@Test
	public void insufficient_balance() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Insufficient number of colored coins on the balance.");

		Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(sender);
		validate(tx);
	}

	@Test
	public void insufficient_balance_1() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Insufficient number of colored coins on the balance.");

		Mockito.when(senderAccount.getProperty(PropertyType.COLORED_BALANCE)).thenReturn(null);

		Transaction tx = ColoredCoinSupplyBuilder.createNew(12000L).build(sender);
		validate(tx);
	}

	@Test
	public void illegal_set_to_zero() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The entire amount of funds must be on the balance.");

		Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(sender);
		validate(tx);
	}

	@Test
	public void success() throws Exception {
		Transaction tx = ColoredCoinSupplyBuilder.createNew(40000L).build(sender);
		validate(tx);
	}

	@Test
	public void success_1() throws Exception {
		Transaction tx = ColoredCoinSupplyBuilder.createNew(60000L).build(sender);
		validate(tx);
	}

	@Test
	public void success_2() throws Exception {
		ColoredBalance coloredBalance = new ColoredBalance();
		coloredBalance.setBalance(50000L, senderAccount.getID());
		AccountProperties.setColoredBalance(senderAccount, coloredBalance);

		Transaction tx = ColoredCoinSupplyBuilder.createNew(0L).build(sender);
		validate(tx);
	}

}
