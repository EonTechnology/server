package com.exscudo.peer.eon.tx.rules;

import java.util.HashMap;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.ColoredPaymentBuilder;
import com.exscudo.peer.eon.tx.builders.TransactionBuilder;
import com.exscudo.peer.eon.tx.parsers.ColoredCoinPaymentParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinPaymentValidationRuleTest extends AbstractParserTest {
    private static final String SENDER = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private static final String RECIPIENT = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
    private static final String COLORED_COIN = "2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011";
    private ColoredCoinPaymentParser parser = new ColoredCoinPaymentParser();
    private ISigner sender;

    private Account recipientAccount;
    private Account coloredCoinAccount;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        byte[] publicKey = new Ed25519Signer(COLORED_COIN).getPublicKey();
        coloredCoinAccount = Mockito.spy(new DefaultAccount(new AccountID(publicKey)));
        AccountProperties.setProperty(coloredCoinAccount, new RegistrationDataProperty(publicKey));
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(50000L);
        AccountProperties.setProperty(coloredCoinAccount, coloredCoin);

        sender = new Ed25519Signer(SENDER);
        Account senderAccount = Mockito.spy(new DefaultAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));
        ColoredBalanceProperty senderColoredBalance = new ColoredBalanceProperty();
        senderColoredBalance.setBalance(10000L, new ColoredCoinID(coloredCoinAccount.getID()));
        AccountProperties.setProperty(senderAccount, senderColoredBalance);

        ISigner recipient = new Ed25519Signer(RECIPIENT);
        recipientAccount = Mockito.spy(new DefaultAccount(new AccountID(recipient.getPublicKey())));
        AccountProperties.setProperty(recipientAccount, new RegistrationDataProperty(recipient.getPublicKey()));

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
        map.put("recipient", recipientAccount.getID().toString());
        map.put("amount", "amount");
        map.put("color", new ColoredCoinID(coloredCoinAccount.getID()).toString());
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
        validate(tx);
    }

    @Test
    public void amount_invalid_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'amount' field value is out of range.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("recipient", recipientAccount.getID().toString());
        map.put("amount", -1L);
        map.put("color", new ColoredCoinID(coloredCoinAccount.getID()).toString());
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
        validate(tx);
    }

    @Test
    public void color_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'color' field value has a unsupported format.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("recipient", recipientAccount.getID().toString());
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
        map.put("color", new ColoredCoinID(coloredCoinAccount.getID()).toString());
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(sender);
        validate(tx);
    }

    @Test
    public void unknown_color() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown colored coin.");

        Mockito.when(ledger.getAccount(coloredCoinAccount.getID())).thenReturn(null);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(sender);
        validate(tx);
    }

    @Test
    public void illegal_account_state() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Account is not associated with a colored coin.");

        coloredCoinAccount.removeProperty(PropertyType.COLORED_COIN);
        ledger.putAccount(coloredCoinAccount);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(sender);
        validate(tx);
    }

    @Test
    public void amount_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Insufficient funds.");

        Transaction tx = ColoredPaymentBuilder.createNew(10001L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(sender);
        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(sender);
        validate(tx);
    }
}
