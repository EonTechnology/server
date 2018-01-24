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
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.state.RegistrationData;
import com.exscudo.peer.eon.state.serialization.PropertyType;
import com.exscudo.peer.eon.transactions.builders.ColoredPaymentBuilder;
import com.exscudo.peer.eon.transactions.builders.TransactionBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.eon.utils.ColoredCoinId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinPaymentValidationRuleTest extends AbstractValidationRuleTest {
	private ColoredCoinPaymentValidationRule rule = new ColoredCoinPaymentValidationRule();

	private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
	private static final String RECIPIENT = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
	private static final String COLORED_COIN = "2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011";

	private ISigner sender;

	private IAccount recipientAccount;
	private IAccount coloredCoinAccount;

	@Override
	protected IValidationRule getValidationRule() {
		return rule;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		byte[] publicKey = new Ed25519Signer(COLORED_COIN).getPublicKey();
		coloredCoinAccount = Mockito.spy(new Account(Format.MathID.pick(publicKey)));
		AccountProperties.setRegistrationData(coloredCoinAccount, new RegistrationData(publicKey));
		ColoredCoin coloredCoin = new ColoredCoin();
		coloredCoin.setMoneySupply(50000L);
		AccountProperties.setColoredCoinRegistrationData(coloredCoinAccount, coloredCoin);

		sender = new Ed25519Signer(SENDER);
		IAccount senderAccount = Mockito.spy(new Account(Format.MathID.pick(sender.getPublicKey())));
		AccountProperties.setRegistrationData(senderAccount, new RegistrationData(sender.getPublicKey()));
		AccountProperties.setBalance(senderAccount, new Balance(5000L));
		ColoredBalance senderColoredBalance = new ColoredBalance();
		senderColoredBalance.setBalance(10000L, coloredCoinAccount.getID());
		AccountProperties.setColoredBalance(senderAccount, senderColoredBalance);

		ISigner recipient = new Ed25519Signer(RECIPIENT);
		recipientAccount = Mockito.spy(new Account(Format.MathID.pick(recipient.getPublicKey())));
		AccountProperties.setRegistrationData(recipientAccount, new RegistrationData(recipient.getPublicKey()));

		ledger.putAccount(senderAccount);
		ledger.putAccount(recipientAccount);
		ledger.putAccount(coloredCoinAccount);

	}

	@Test
	public void invalid_attach() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Attachment of unknown type.");

		HashMap<String, Object> map = new HashMap<>();
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
		validate(tx);
	}

	@Test
	public void amount_invalid_format() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'amount' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("recipient", Format.ID.accountId(recipientAccount.getID()));
		map.put("amount", "amount");
		map.put("color", ColoredCoinId.convert(coloredCoinAccount.getID()));
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
		validate(tx);
	}

	@Test
	public void amount_invalid_value() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'amount' field value is out of range.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("recipient", Format.ID.accountId(recipientAccount.getID()));
		map.put("amount", -1L);
		map.put("color", ColoredCoinId.convert(coloredCoinAccount.getID()));
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
		validate(tx);
	}

	@Test
	public void color_invalid_format() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'color' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("recipient", Format.ID.accountId(recipientAccount.getID()));
		map.put("amount", 9999L);
		map.put("color", "color");
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
		validate(tx);
	}

	@Test
	public void recipient_invalid_format() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("The 'recipient' field value has a unsupported format.");

		HashMap<String, Object> map = new HashMap<>();
		map.put("recipient", "recipient");
		map.put("amount", 9999L);
		map.put("color", ColoredCoinId.convert(coloredCoinAccount.getID()));
		Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
		validate(tx);
	}

	@Test
	public void unknown_color() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Unknown colored coin.");

		Mockito.when(ledger.getAccount(coloredCoinAccount.getID())).thenReturn(null);

		Transaction tx = ColoredPaymentBuilder.createNew(9999L, coloredCoinAccount.getID(), recipientAccount.getID())
				.build(sender);
		validate(tx);
	}

	@Test
	public void illegal_account_state() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Account is not associated with a colored coin.");

		coloredCoinAccount.removeProperty(PropertyType.COLORED_COIN);
		ledger.putAccount(coloredCoinAccount);

		Transaction tx = ColoredPaymentBuilder.createNew(9999L, coloredCoinAccount.getID(), recipientAccount.getID())
				.build(sender);
		validate(tx);
	}

	@Test
	public void amount_out_of_range() throws Exception {
		expectedException.expect(ValidateException.class);
		expectedException.expectMessage("Insufficient funds.");

		Transaction tx = ColoredPaymentBuilder.createNew(10001L, coloredCoinAccount.getID(), recipientAccount.getID())
				.build(sender);
		validate(tx);
	}

	@Test
	public void success() throws Exception {
		Transaction tx = ColoredPaymentBuilder.createNew(9999L, coloredCoinAccount.getID(), recipientAccount.getID())
				.build(sender);
		validate(tx);
	}

}
