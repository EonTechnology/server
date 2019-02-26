package org.eontechology.and.peer.eon;

import java.util.HashMap;
import java.util.LinkedList;

import org.eontechology.and.peer.Signer;
import org.eontechology.and.peer.TestAccount;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.parsers.ComplexPaymentParserV3;
import org.eontechology.and.peer.tx.ColoredCoinID;
import org.eontechology.and.peer.tx.midleware.builders.ColoredPaymentBuilder;
import org.eontechology.and.peer.tx.midleware.builders.ComplexPaymentBuilder;
import org.eontechology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ComplexPaymentTransactionV3Test extends AbstractTransactionTest {
    private ComplexPaymentParserV3 parser;

    private static final long S_MONEY = 1000000L;
    private static final long SC_MONEY = 1000000L;
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

        parser = new ComplexPaymentParserV3();

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
    public void success_5() throws Exception {

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
    public void success_6() throws Exception {

        int TX_COUNT = 100;

        Transaction tx =
                PaymentBuilder.createNew(AMOUNT, account3).forFee(0L).payedBy(account1).build(networkID, signer2);

        LinkedList<Transaction> txSet = new LinkedList<>();
        txSet.add(tx);
        txSet.add(PaymentBuilder.createNew(DELIVERY, account2).forFee(0L).refBy(tx.getID()).build(networkID, signer3));

        for (int i = 0; i < TX_COUNT; i++) {
            tx = ColoredPaymentBuilder.createNew(C_AMOUNT, new ColoredCoinID(account3), account2)
                                      .forFee(0L)
                                      .refBy(tx.getID())
                                      .build(networkID, signer3);
            txSet.add(tx);
        }
        tx = ComplexPaymentBuilder.createNew(txSet.toArray(new Transaction[] {})).forFee(FEE).build(networkID, signer1);

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
        Assert.assertEquals(C_AMOUNT * TX_COUNT,
                            AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));

        acc = ledger.getAccount(account3);
        Assert.assertEquals(S_MONEY + AMOUNT - DELIVERY, AccountProperties.getBalance(acc).getValue());
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account1)));
        Assert.assertEquals(0L, AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account2)));
        Assert.assertEquals(SC_MONEY - C_AMOUNT * TX_COUNT,
                            AccountProperties.getColoredBalance(acc).getBalance(new ColoredCoinID(account3)));
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
        nestedTx2.setReference(nestedTx3.getID());

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
