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
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.DepositBuilder;
import com.exscudo.peer.eon.tx.parsers.DepositParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DepositValidationRuleTest extends AbstractParserTest {
    private DepositParser parser = new DepositParser();

    private ISigner senderSigner =
            new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account sender;

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

        ledger = spy(new DefaultLedger());
        ledger.putAccount(sender);
    }

    @Test
    public void deposit_refill_with_invalid_attachment_is_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        Transaction tx = spy(DepositBuilder.createNew(999L).build(senderSigner));
        HashMap<String, Object> map = new HashMap<>();
        map.put("amount", "test");
        when(tx.getData()).thenReturn(map);
        resolveSignature(tx);

        validate(tx);
    }

    @Test
    public void deposit_refill_with_unknown_sender_is_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown sender.");

        Transaction tx = DepositBuilder.createNew(1L).build(senderSigner);
        when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(null);
        validate(tx);
    }

    @Test
    public void deposit_refill_with_low_balance_is_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Not enough funds.");

        long depositAmount = 999L;
        Transaction tx = DepositBuilder.createNew(depositAmount).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(depositAmount + tx.getFee() - 1L));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(0L, 0));

        validate(tx);
    }

    @Test
    public void deposit_fee_from_deposit() throws Exception {
        Transaction tx = DepositBuilder.createNew(100L).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(0L));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(100L + tx.getFee(), 0));

        validate(tx);
    }

    @Test
    public void deposit_fee_from_low_deposit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Not enough funds.");

        Transaction tx = DepositBuilder.createNew(0L).forFee(10L).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(0L));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(5L, 0));

        validate(tx);
    }

    @Test
    public void deposit_refill_with_arbitrary_amount_is_ok() throws Exception {
        long refillAmount = 900;

        Transaction tx = DepositBuilder.createNew(refillAmount).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(refillAmount + tx.getFee()));

        validate(tx);
    }

    @Test
    public void deposit_already_set() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Value already set.");

        long refillAmount = 900;

        Transaction tx = DepositBuilder.createNew(refillAmount).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(refillAmount + tx.getFee()));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(refillAmount, 0));

        validate(tx);
    }

    @Test
    public void deposit_refill_with_positive_deposit_is_ok() throws Exception {

        long refillAmount = 100500;
        long depositAmount = 1;

        Transaction tx = DepositBuilder.createNew(refillAmount).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(refillAmount + tx.getFee()));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(depositAmount, 0));

        validate(tx);
    }

    @Test
    public void deposit_reset() throws Exception {
        Transaction tx = DepositBuilder.createNew(0L).build(senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(tx.getFee()));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(100L, 0));

        validate(tx);
    }

    @Test
    public void deposit_less_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid amount.");

        Transaction tx = DepositBuilder.createNew(-10L).build(senderSigner);
        AccountProperties.setProperty(sender, new BalanceProperty(tx.getFee()));

        validate(tx);
    }

    private void resolveSignature(Transaction tx) {
        byte[] bytes = tx.getBytes();
        byte[] signature = senderSigner.sign(bytes);
        tx.setSignature(signature);
    }
}
