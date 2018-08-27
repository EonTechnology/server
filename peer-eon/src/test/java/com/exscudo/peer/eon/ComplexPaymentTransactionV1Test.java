package com.exscudo.peer.eon;

import java.util.HashMap;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinEmitMode;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.ComplexPaymentParserV1;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.ComplexPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ComplexPaymentTransactionV1Test extends AbstractTransactionTest {
    private ComplexPaymentParserV1 parser;

    private ISigner signer = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private Account account;
    private ISigner signer1 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account account1;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        account = Mockito.spy(new TestAccount(new AccountID(signer.getPublicKey())));
        account1 = Mockito.spy(new TestAccount(new AccountID(signer1.getPublicKey())));

        parser = new ComplexPaymentParserV1();
    }

    @Test
    public void illegal_usage() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_ILLEGAL_USAGE);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {
                PaymentBuilder.createNew(100L, account.getID()).refBy(new TransactionID(1L)).build(networkID, signer1)
        }).build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).build(networkID, signer1);
        Transaction nestedTx2 =
                PaymentBuilder.createNew(10L, account1.getID()).refBy(nestedTx1.getID()).build(networkID, signer);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2})
                                              .withParam("key", "value")
                                              .build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void invalid_sequence_not_found_head() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);

        Transaction nestedTx1 =
                PaymentBuilder.createNew(100L, account.getID()).refBy(new TransactionID(1L)).build(networkID, signer1);
        Transaction nestedTx2 =
                PaymentBuilder.createNew(10L, account1.getID()).refBy(nestedTx1.getID()).build(networkID, signer);

        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void invalid_sequence_multily_head() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_ILLEGAL_SEQUENCE);

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).build(networkID, signer1);
        Transaction nestedTx2 = PaymentBuilder.createNew(10L, account.getID()).build(networkID, signer1);

        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void unable_to_use_lc() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_INVALID_LC);

        ISigner s = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

        Transaction nestedTx1 =
                PaymentBuilder.createNew(100L, new AccountID(s.getPublicKey())).build(networkID, signer1);
        Transaction nestedTx2 =
                ColoredPaymentBuilder.createNew(10L, new ColoredCoinID(account.getID()), account1.getID())
                                     .refBy(nestedTx1.getID())
                                     .build(networkID, signer);
        Transaction nestedTx3 =
                PaymentBuilder.createNew(1L, account1.getID()).refBy(nestedTx1.getID()).build(networkID, signer);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void nested_transaction_unknown_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TRANSACTION_TYPE_UNKNOWN);

        ISigner s = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
        Transaction nestedTx1 = RegistrationBuilder.createNew(s.getPublicKey()).build(networkID, signer1);
        Transaction nestedTx2 =
                PaymentBuilder.createNew(100L, account.getID()).refBy(nestedTx1.getID()).build(networkID, signer);
        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void nested_transaction_unknown_type_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TRANSACTION_TYPE_UNKNOWN);

        ISigner s = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).build(networkID, signer1);
        Transaction nestedTx2 =
                RegistrationBuilder.createNew(s.getPublicKey()).refBy(nestedTx1.getID()).build(networkID, signer);
        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void nested_transaction_invalid_recipient() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);

        ISigner s = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).build(networkID, signer1);
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(10L,
                                                                new ColoredCoinID(account.getID()),
                                                                new AccountID(s.getPublicKey()))
                                                     .refBy(nestedTx1.getID())
                                                     .build(networkID, signer);
        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void nested_transaction_invalid_reference() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).build(networkID, signer1);
        Transaction nestedTx2 =
                ColoredPaymentBuilder.createNew(10L, new ColoredCoinID(account.getID()), account1.getID())
                                     .refBy(nestedTx1.getID())
                                     .build(networkID, signer);
        Transaction nestedTx3 =
                PaymentBuilder.createNew(1L, account1.getID()).refBy(nestedTx2.getID()).build(networkID, signer);
        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void nested_transaction_invalid_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);

        ISigner s = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).build(networkID, signer1);
        Transaction nestedTx2 =
                ColoredPaymentBuilder.createNew(10L, new ColoredCoinID(account.getID()), account1.getID())
                                     .refBy(nestedTx1.getID())
                                     .build(networkID, s);
        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2}).build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void success() throws Exception {

        // init

        AccountID id = new AccountID(signer.getPublicKey());
        AccountID id1 = new AccountID(signer1.getPublicKey());

        Account account = new Account(id);
        account = AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        account = AccountProperties.setProperty(account, new BalanceProperty(100));
        ColoredCoinProperty coloredCoinProperty = new ColoredCoinProperty();
        coloredCoinProperty.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
        coloredCoinProperty.setEmitMode(ColoredCoinEmitMode.PRESET);
        coloredCoinProperty.setMoneySupply(1000L);
        account = AccountProperties.setProperty(account, coloredCoinProperty);
        account = AccountProperties.setProperty(account, new ColoredBalanceProperty(new HashMap<String, Object>() {{
            ColoredCoinID id = new ColoredCoinID(new AccountID(signer.getPublicKey()));
            put(id.toString(), 1000);
        }}));

        Account account1 = new Account(id1);
        account1 = AccountProperties.setProperty(account1, new RegistrationDataProperty(signer1.getPublicKey()));
        account1 = AccountProperties.setProperty(account1, new BalanceProperty(100));

        ledger.putAccount(account);
        ledger.putAccount(account1);

        // run
        Transaction nestedTx1 = PaymentBuilder.createNew(100L, account.getID()).forFee(0L).build(networkID, signer1);
        Transaction nestedTx2 =
                ColoredPaymentBuilder.createNew(10L, new ColoredCoinID(account.getID()), account1.getID())
                                     .forFee(0L)
                                     .refBy(nestedTx1.getID())
                                     .build(networkID, signer);
        Transaction nestedTx3 = PaymentBuilder.createNew(1L, account1.getID())
                                              .forFee(0L)
                                              .refBy(nestedTx1.getID())
                                              .build(networkID, signer);
        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .forFee(10)
                                              .build(networkID, signer);

        validate(parser, tx);

        // validate

        Account updatedAccount = ledger.getAccount(id);
        Assert.assertEquals(AccountProperties.getBalance(updatedAccount).getValue(), 189L);
        Assert.assertEquals(AccountProperties.getColoredBalance(updatedAccount).getBalance(new ColoredCoinID(id)),
                            990L);

        Account updatedAccount1 = ledger.getAccount(id1);
        Assert.assertEquals(AccountProperties.getBalance(updatedAccount1).getValue(), 1L);
        Assert.assertEquals(AccountProperties.getColoredBalance(updatedAccount1).getBalance(new ColoredCoinID(id)),
                            10L);
    }
}
