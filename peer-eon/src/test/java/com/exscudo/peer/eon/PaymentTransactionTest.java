package com.exscudo.peer.eon;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.PaymentParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PaymentTransactionTest extends AbstractTransactionTest {
    private PaymentParser parser = new PaymentParser();

    private ISigner senderSigner = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account sender;

    private ISigner recipientSigner = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private Account recipient;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        sender = Mockito.spy(new TestAccount(new AccountID(senderSigner.getPublicKey())));
        AccountProperties.setProperty(sender, new RegistrationDataProperty(senderSigner.getPublicKey()));

        recipient = Mockito.spy(new TestAccount(new AccountID(recipientSigner.getPublicKey())));
        AccountProperties.setProperty(recipient, new RegistrationDataProperty(recipientSigner.getPublicKey()));

        ledger.putAccount(sender);
        ledger.putAccount(recipient);
    }

    @Test
    public void payment_invalid_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);

        when(ledger.getAccount(eq(new AccountID(senderSigner.getPublicKey())))).thenReturn(null);

        Transaction tx = PaymentBuilder.createNew(100L, recipient.getID())
                                       .forFee(1L)
                                       .validity(timeProvider.get(), 3600)
                                       .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void payment_invalid_recipient_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_INVALID_FORMAT);

        AccountProperties.setProperty(sender, new BalanceProperty(101L));

        Transaction tx = new TransactionBuilder(TransactionType.Payment).withParam("amount", 100L)
                                                                        .withParam("recipient", "recipient")
                                                                        .forFee(1L)
                                                                        .validity(timeProvider.get(), 3600)
                                                                        .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void amount_less_than_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_OUT_OF_RANGE);

        AccountProperties.setProperty(sender, new BalanceProperty(101L));

        Transaction tx = new TransactionBuilder(TransactionType.Payment).withParam("amount", -1L)
                                                                        .withParam("recipient", new AccountID(12345L))
                                                                        .forFee(1L)
                                                                        .validity(timeProvider.get(), 3600)
                                                                        .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void amount_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_INVALID_FORMAT);

        AccountProperties.setProperty(sender, new BalanceProperty(101L));

        Transaction tx = new TransactionBuilder(TransactionType.Payment).withParam("amount", "amount")
                                                                        .withParam("recipient", new AccountID(12345L))
                                                                        .forFee(1L)
                                                                        .validity(timeProvider.get(), 3600)
                                                                        .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void payment_invalid_recipient() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.RECIPIENT_ACCOUNT_NOT_FOUND);

        AccountProperties.setProperty(sender, new BalanceProperty(101L));

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(12345L))
                                       .forFee(1L)
                                       .validity(timeProvider.get(), 3600)
                                       .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void payment_invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction tx = spy(PaymentBuilder.createNew(100L, new AccountID(12345L))
                                           .withParam("param", "value")
                                           .forFee(1L)
                                           .validity(timeProvider.get(), 3600)
                                           .build(networkID, senderSigner));
        validate(tx);
    }

    @Test
    public void payment_invalid_balance() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NOT_ENOUGH_FUNDS);

        AccountProperties.setProperty(sender, new BalanceProperty(103L));

        Transaction tx = PaymentBuilder.createNew(100L, recipient.getID())
                                       .forFee(5L)
                                       .validity(timeProvider.get(), 3600)
                                       .build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, senderSigner);
        Transaction tx = PaymentBuilder.createNew(100L, recipient.getID())
                                       .forFee(5L)
                                       .validity(timeProvider.get(), 3600)
                                       .addNested(innerTx)
                                       .build(networkID, senderSigner);

        validate(tx);
    }
}
