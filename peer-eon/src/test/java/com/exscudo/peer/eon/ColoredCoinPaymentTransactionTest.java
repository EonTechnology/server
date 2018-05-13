package com.exscudo.peer.eon;

import java.util.HashMap;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ColoredCoinPaymentTransactionTest extends AbstractTransactionTest {
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

        byte[] publicKey = new Signer(COLORED_COIN).getPublicKey();
        coloredCoinAccount = Mockito.spy(new TestAccount(new AccountID(publicKey)));
        AccountProperties.setProperty(coloredCoinAccount, new RegistrationDataProperty(publicKey));
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(50000L);
        AccountProperties.setProperty(coloredCoinAccount, coloredCoin);

        sender = new Signer(SENDER);
        Account senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));
        ColoredBalanceProperty senderColoredBalance = new ColoredBalanceProperty();
        senderColoredBalance.setBalance(10000L, new ColoredCoinID(coloredCoinAccount.getID()));
        AccountProperties.setProperty(senderAccount, senderColoredBalance);

        ISigner recipient = new Signer(RECIPIENT);
        recipientAccount = Mockito.spy(new TestAccount(new AccountID(recipient.getPublicKey())));
        AccountProperties.setProperty(recipientAccount, new RegistrationDataProperty(recipient.getPublicKey()));

        ledger.putAccount(senderAccount);
        ledger.putAccount(recipientAccount);
        ledger.putAccount(coloredCoinAccount);
    }

    @Test
    public void invalid_attach() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        HashMap<String, Object> map = new HashMap<>();
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void amount_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("recipient", recipientAccount.getID().toString());
        map.put("amount", "amount");
        map.put("color", new ColoredCoinID(coloredCoinAccount.getID()).toString());
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void amount_invalid_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("recipient", recipientAccount.getID().toString());
        map.put("amount", -1L);
        map.put("color", new ColoredCoinID(coloredCoinAccount.getID()).toString());
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void color_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLOR_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("recipient", recipientAccount.getID().toString());
        map.put("amount", 9999L);
        map.put("color", "color");
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void recipient_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("recipient", "recipient");
        map.put("amount", 9999L);
        map.put("color", new ColoredCoinID(coloredCoinAccount.getID()).toString());
        Transaction tx = new TransactionBuilder(TransactionType.ColoredCoinPayment, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void unknown_color() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_ACCOUNT_NOT_FOUND);

        Mockito.when(ledger.getAccount(coloredCoinAccount.getID())).thenReturn(null);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void illegal_account_state() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_EXISTS);

        coloredCoinAccount.removeProperty(PropertyType.COLORED_COIN);
        ledger.putAccount(coloredCoinAccount);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void amount_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

        Transaction tx = ColoredPaymentBuilder.createNew(10001L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID())
                                              .addNested(innerTx)
                                              .build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void exceeding_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TOO_MACH_SIZE);

        long index = 0;
        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        while (!coloredBalance.isFull()) {
            coloredBalance.setBalance(1, new ColoredCoinID(index++));
        }
        AccountProperties.setProperty(recipientAccount, coloredBalance);

        //recipient

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void full_limit() throws Exception {
        long index = 0;
        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        coloredBalance.setBalance(1, new ColoredCoinID(coloredCoinAccount.getID()));
        while (!coloredBalance.isFull()) {
            coloredBalance.setBalance(1, new ColoredCoinID(index++));
        }
        AccountProperties.setProperty(recipientAccount, coloredBalance);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(tx);
    }
}
