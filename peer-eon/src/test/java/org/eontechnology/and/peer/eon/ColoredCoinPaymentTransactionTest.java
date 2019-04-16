package org.eontechnology.and.peer.eon;

import java.util.HashMap;

import org.eontechnology.and.peer.Signer;
import org.eontechnology.and.peer.TestAccount;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import org.eontechnology.and.peer.tx.ColoredCoinID;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.ColoredPaymentBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
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

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        byte[] publicKey = new Signer(COLORED_COIN).getPublicKey();
        coloredCoinAccount = Mockito.spy(new TestAccount(new AccountID(publicKey)));
        AccountProperties.setProperty(coloredCoinAccount, new RegistrationDataProperty(publicKey));
        AccountProperties.setProperty(coloredCoinAccount, new BalanceProperty(10L));
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
        coloredCoin.setEmitMode(ColoredCoinEmitMode.PRESET);
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
        validate(parser, tx);
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
        validate(parser, tx);
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
        validate(parser, tx);
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
        validate(parser, tx);
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
        validate(parser, tx);
    }

    @Test
    public void unknown_color() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_ACCOUNT_NOT_FOUND);

        Mockito.when(ledger.getAccount(coloredCoinAccount.getID())).thenReturn(null);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(parser, tx);
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
        validate(parser, tx);
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
        validate(parser, tx);
    }

    @Test
    public void not_enough_colored_coun() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

        Transaction tx = ColoredPaymentBuilder.createNew(10001L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void auto_emit_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);

        ISigner s = new Signer(COLORED_COIN);

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(coloredCoinAccount);
        coloredCoin.setEmitMode(ColoredCoinEmitMode.AUTO);
        coloredCoin.setMoneySupply(Long.MAX_VALUE - 10000L);
        AccountProperties.setProperty(coloredCoinAccount, coloredCoin);

        Transaction tx = ColoredPaymentBuilder.createNew(10001L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, s);
        validate(parser, tx);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void success_auto_emission() throws Exception {
        ISigner s = new Signer(COLORED_COIN);

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(coloredCoinAccount);
        coloredCoin.setEmitMode(ColoredCoinEmitMode.AUTO);
        coloredCoin.setMoneySupply(10000L);
        AccountProperties.setProperty(coloredCoinAccount, coloredCoin);

        Transaction tx = ColoredPaymentBuilder.createNew(2000L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, s);
        validate(parser, tx);
        Assert.assertEquals(12000L, AccountProperties.getColoredCoin(coloredCoinAccount).getMoneySupply());
    }

    @Test
    public void success_auto_withdrawl() throws Exception {
        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(coloredCoinAccount);
        coloredCoin.setEmitMode(ColoredCoinEmitMode.AUTO);
        coloredCoin.setMoneySupply(10000L);
        AccountProperties.setProperty(coloredCoinAccount, coloredCoin);

        Transaction tx = ColoredPaymentBuilder.createNew(10000L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         coloredCoinAccount.getID()).build(networkID, sender);
        validate(parser, tx);
        Assert.assertTrue(AccountProperties.getColoredCoin(coloredCoinAccount).isIssued());
        Assert.assertEquals(0L, AccountProperties.getColoredCoin(coloredCoinAccount).getMoneySupply());
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
        validate(parser, tx);
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
        validate(parser, tx);
    }

    @Test
    public void amount_error_null() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_INVALID_FORMAT);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);

        tx.getData().put("amount", null);
        validate(parser, tx);
    }

    @Test
    public void amount_error_string() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_INVALID_FORMAT);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);

        tx.getData().put("amount", "100");
        validate(parser, tx);
    }

    @Test
    public void amount_error_decimal() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_INVALID_FORMAT);

        Transaction tx = ColoredPaymentBuilder.createNew(9999L,
                                                         new ColoredCoinID(coloredCoinAccount.getID()),
                                                         recipientAccount.getID()).build(networkID, sender);

        tx.getData().put("amount", 100.001);
        validate(parser, tx);
    }
}
