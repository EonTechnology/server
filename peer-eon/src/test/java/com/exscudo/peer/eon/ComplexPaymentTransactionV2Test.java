package com.exscudo.peer.eon;

import java.util.HashMap;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinEmitMode;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.ComplexPaymentParserV2;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.midleware.builders.ColoredPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.ComplexPaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ComplexPaymentTransactionV2Test extends AbstractTransactionTest {
    private ComplexPaymentParserV2 parser;

    private static final long S_MONEY = 1000000L;
    private static final long SC_MONEY = 1000L;
    private static final long FEE = 100L;
    private static final long AMOUNT = 1000L;
    private static final long C_AMOUNT = 10L;
    private static final long DELIVERY = 1L;

    private ISigner signer1 = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private AccountID account1;
    private ISigner signer2 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private AccountID account2;
    private ISigner signer3 = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
    private AccountID account3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        parser = new ComplexPaymentParserV2();

        account1 = new AccountID(signer1.getPublicKey());
        account2 = new AccountID(signer2.getPublicKey());
        account3 = new AccountID(signer3.getPublicKey());

        ISigner[] signerSet = new ISigner[] {signer1, signer2, signer3};

        for (ISigner signer : signerSet) {
            Account acc = Mockito.spy(new TestAccount(new AccountID(signer.getPublicKey())));

            ColoredCoinProperty coloredCoinProperty = new ColoredCoinProperty();
            coloredCoinProperty.setAttributes(new ColoredCoinProperty.Attributes(0, 0));
            coloredCoinProperty.setEmitMode(ColoredCoinEmitMode.PRESET);
            coloredCoinProperty.setMoneySupply(SC_MONEY);

            AccountProperties.setProperty(acc, new RegistrationDataProperty(signer.getPublicKey()));
            AccountProperties.setProperty(acc, new BalanceProperty(S_MONEY));
            AccountProperties.setProperty(acc, coloredCoinProperty);
            AccountProperties.setProperty(acc, new ColoredBalanceProperty(new HashMap<String, Object>() {{
                put(new ColoredCoinID(new AccountID(signer.getPublicKey())).toString(), SC_MONEY);
            }}));

            ledger.putAccount(acc);
        }
    }

    @Test
    public void success() throws Exception {

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(C_AMOUNT, new ColoredCoinID(account3), account2)
                                                     .forFee(0L)
                                                     .refBy(nestedTx1.getID())
                                                     .build(networkID, signer3);
        Transaction nestedTx3 = PaymentBuilder.createNew(DELIVERY, account2)
                                              .forFee(0L)
                                              .refBy(nestedTx1.getID())
                                              .build(networkID, signer3);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .forFee(FEE)
                                              .build(networkID, signer1);

        validate(parser, tx);

        // validate state

        Account acc = ledger.getAccount(account1);
        Assert.assertEquals(S_MONEY - FEE, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account2);
        Assert.assertEquals(S_MONEY - AMOUNT + DELIVERY, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(C_AMOUNT, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account3);
        Assert.assertEquals(S_MONEY + AMOUNT - DELIVERY, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(SC_MONEY - C_AMOUNT,
                            AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));
    }

    @Test
    public void success_2() throws Exception {

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(C_AMOUNT, new ColoredCoinID(account3), account2)
                                                     .forFee(0L)
                                                     .payedBy(account1)
                                                     .build(networkID, signer3);
        Transaction nestedTx3 = PaymentBuilder.createNew(DELIVERY, account2)
                                              .forFee(0L)
                                              .refBy(nestedTx2.getID())
                                              .build(networkID, signer3);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .forFee(FEE)
                                              .build(networkID, signer1);

        validate(parser, tx);

        // validate state

        Account acc = ledger.getAccount(account1);
        Assert.assertEquals(S_MONEY - FEE, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account2);
        Assert.assertEquals(S_MONEY - AMOUNT + DELIVERY, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(C_AMOUNT, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account3);
        Assert.assertEquals(S_MONEY + AMOUNT - DELIVERY, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(SC_MONEY - C_AMOUNT,
                            AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));
    }

    @Test
    public void success_3() throws Exception {

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(C_AMOUNT, new ColoredCoinID(account3), account2)
                                                     .forFee(0L)
                                                     .payedBy(account1)
                                                     .build(networkID, signer3);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2})
                                              .forFee(FEE)
                                              .build(networkID, signer1);

        validate(parser, tx);

        // validate state

        Account acc = ledger.getAccount(account1);
        Assert.assertEquals(S_MONEY - FEE, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account2);
        Assert.assertEquals(S_MONEY - AMOUNT, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(C_AMOUNT, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account3);
        Assert.assertEquals(S_MONEY + AMOUNT, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(SC_MONEY - C_AMOUNT,
                            AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));
    }

    @Test
    public void success_4() throws Exception {

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);

        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1}).forFee(FEE).build(networkID, signer1);

        validate(parser, tx);

        // validate state

        Account acc = ledger.getAccount(account1);
        Assert.assertEquals(S_MONEY - FEE, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account2);
        Assert.assertEquals(S_MONEY - AMOUNT, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account3);
        Assert.assertEquals(S_MONEY + AMOUNT, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(SC_MONEY, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));
    }

    @Test
    public void error_invalid_payer() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_PAYER_ERROR);

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account3).build(networkID, signer2);

        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1}).forFee(FEE).build(networkID, signer1);

        validate(parser, tx);
    }

    @Test
    public void error_no_payer() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_PAYER_ERROR);

        Transaction nestedTx1 = PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).build(networkID, signer2);

        Transaction tx =
                ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1}).forFee(FEE).build(networkID, signer1);

        validate(parser, tx);
    }

    @Test
    public void error_invalid_sequence() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(C_AMOUNT, new ColoredCoinID(account3), account2)
                                                     .forFee(0L)
                                                     .refBy(nestedTx1.getID())
                                                     .build(networkID, signer3);
        Transaction nestedTx3 = PaymentBuilder.createNew(DELIVERY, account2)
                                              .forFee(0L)
                                              .refBy(nestedTx2.getID())
                                              .build(networkID, signer3);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2, nestedTx3})
                                              .forFee(FEE)
                                              .build(networkID, signer1);

        validate(parser, tx);
    }

    @Test
    public void error_invalid_sequence_payer() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_PAYER_SEQUENCE_ERROR);

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);
        Transaction nestedTx2 = ColoredPaymentBuilder.createNew(C_AMOUNT, new ColoredCoinID(account3), account2)
                                                     .forFee(0L)
                                                     .refBy(nestedTx1.getID())
                                                     .payedBy(account1)
                                                     .build(networkID, signer3);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1, nestedTx2})
                                              .forFee(FEE)
                                              .build(networkID, signer1);

        validate(parser, tx);
    }

    @Test
    public void error_invalid_data() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction nestedTx1 =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);

        Transaction tx = ComplexPaymentBuilder.createNew(new Transaction[] {nestedTx1})
                                              .forFee(FEE)
                                              .withParam("test", 123)
                                              .build(networkID, signer1);

        validate(parser, tx);
    }
}
