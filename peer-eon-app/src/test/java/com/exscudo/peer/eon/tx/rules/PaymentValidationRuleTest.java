package com.exscudo.peer.eon.tx.rules;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.exscudo.peer.eon.tx.parsers.PaymentParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PaymentValidationRuleTest extends AbstractParserTest {
    private PaymentParser parser = new PaymentParser();

    private ISigner senderSigner =
            new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account sender;

    private ISigner recipientSigner =
            new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private Account recipient;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        sender = Mockito.spy(new DefaultAccount(new AccountID(senderSigner.getPublicKey())));
        AccountProperties.setProperty(sender, new RegistrationDataProperty(senderSigner.getPublicKey()));

        recipient = Mockito.spy(new DefaultAccount(new AccountID(recipientSigner.getPublicKey())));
        AccountProperties.setProperty(recipient, new RegistrationDataProperty(recipientSigner.getPublicKey()));

        ledger = spy(new DefaultLedger());
        ledger.putAccount(sender);
        ledger.putAccount(recipient);
    }

    @Test
    public void payment_invalid_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account.");

        when(ledger.getAccount(eq(new AccountID(senderSigner.getPublicKey())))).thenReturn(null);

        Transaction tx = PaymentBuilder.createNew(100L, recipient.getID())
                                       .forFee(1L)
                                       .validity(timeProvider.get(), 3600)
                                       .build(senderSigner);
        validate(tx);
    }

    @Test
    public void payment_invalid_recipient() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown recipient.");

        AccountProperties.setProperty(sender, new BalanceProperty(101L));

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(12345L))
                                       .forFee(1L)
                                       .validity(timeProvider.get(), 3600)
                                       .build(senderSigner);
        validate(tx);
    }

    @Test
    public void payment_invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        Transaction tx = spy(PaymentBuilder.createNew(100L, new AccountID(12345L))
                                           .forFee(1L)
                                           .validity(timeProvider.get(), 3600)
                                           .build(senderSigner));
        when(tx.getData()).thenReturn(new HashMap<>());
        resolveSignature(tx);

        validate(tx);
    }

    @Test
    public void payment_invalid_balance() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Not enough funds.");

        AccountProperties.setProperty(sender, new BalanceProperty(103L));

        Transaction tx = PaymentBuilder.createNew(100L, recipient.getID())
                                       .forFee(5L)
                                       .validity(timeProvider.get(), 3600)
                                       .build(senderSigner);
        validate(tx);
    }

    private void resolveSignature(Transaction tx) {
        byte[] bytes = tx.getBytes();
        byte[] signature = senderSigner.sign(bytes);
        tx.setSignature(signature);
    }
}
