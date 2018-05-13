package com.exscudo.peer.eon;

import static org.mockito.ArgumentMatchers.eq;
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
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.DepositParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.DepositBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DepositTransactionTest extends AbstractTransactionTest {
    private DepositParser parser = new DepositParser();

    private ISigner senderSigner = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account sender;

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

        ledger.putAccount(sender);
    }

    @Test
    public void attachment_unknown_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction tx = new TransactionBuilder(TransactionType.Deposit).build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void deposit_refill_with_invalid_attachment_is_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_INVALID_FORMAT);

        Transaction tx = DepositBuilder.createNew(999L).withParam("amount", "test").build(networkID, senderSigner);
        validate(tx);
    }

    @Test
    public void deposit_refill_with_unknown_sender_is_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);

        Transaction tx = DepositBuilder.createNew(1L).build(networkID, senderSigner);
        when(ledger.getAccount(eq(tx.getSenderID()))).thenReturn(null);
        validate(tx);
    }

    @Test
    public void deposit_refill_with_low_balance_is_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NOT_ENOUGH_FUNDS);

        long depositAmount = 999L;
        Transaction tx = DepositBuilder.createNew(depositAmount).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(depositAmount + tx.getFee() - 1L));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(0L, 0));

        validate(tx);
    }

    @Test
    public void deposit_fee_from_deposit() throws Exception {
        Transaction tx = DepositBuilder.createNew(100L).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(0L));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(100L + tx.getFee(), 0));

        validate(tx);
    }

    @Test
    public void deposit_fee_from_low_deposit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NOT_ENOUGH_FUNDS);

        Transaction tx = DepositBuilder.createNew(0L).forFee(10L).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(0L));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(5L, 0));

        validate(tx);
    }

    @Test
    public void deposit_refill_with_arbitrary_amount_is_ok() throws Exception {
        long refillAmount = 900;

        Transaction tx = DepositBuilder.createNew(refillAmount).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(refillAmount + tx.getFee()));

        validate(tx);
    }

    @Test
    public void deposit_already_set() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

        long refillAmount = 900;

        Transaction tx = DepositBuilder.createNew(refillAmount).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(refillAmount + tx.getFee()));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(refillAmount, 0));

        validate(tx);
    }

    @Test
    public void deposit_refill_with_positive_deposit_is_ok() throws Exception {

        long refillAmount = 100500;
        long depositAmount = 1;

        Transaction tx = DepositBuilder.createNew(refillAmount).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(refillAmount + tx.getFee()));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(depositAmount, 0));

        validate(tx);
    }

    @Test
    public void deposit_reset() throws Exception {
        Transaction tx = DepositBuilder.createNew(0L).build(networkID, senderSigner);

        AccountProperties.setProperty(sender, new BalanceProperty(tx.getFee()));
        AccountProperties.setProperty(sender, new GeneratingBalanceProperty(100L, 0));

        validate(tx);
    }

    @Test
    public void deposit_less_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.AMOUNT_OUT_OF_RANGE);

        Transaction tx = DepositBuilder.createNew(-10L).build(networkID, senderSigner);
        AccountProperties.setProperty(sender, new BalanceProperty(tx.getFee()));

        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, senderSigner);
        Transaction tx = DepositBuilder.createNew(-10L).addNested(innerTx).build(networkID, senderSigner);

        validate(tx);
    }
}
