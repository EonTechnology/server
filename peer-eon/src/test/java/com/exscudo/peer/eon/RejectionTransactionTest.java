package com.exscudo.peer.eon;

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
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.RejectionParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.RejectionBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RejectionTransactionTest extends AbstractTransactionTest {
    private ISigner base = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

    private RejectionParser parser = new RejectionParser();
    private Account baseAccount;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        AccountID baseAccountID = new AccountID(base.getPublicKey());
        AccountID delegateID = new AccountID(delegate.getPublicKey());
        baseAccount = Mockito.spy(new TestAccount(baseAccountID));
        AccountProperties.setProperty(baseAccount, new RegistrationDataProperty(base.getPublicKey()));
        AccountProperties.setProperty(baseAccount, new BalanceProperty(100L));

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(delegateID, 5);
        validationMode.setQuorum(50);
        validationMode.setQuorum(TransactionType.Payment, 70);
        AccountProperties.setProperty(baseAccount, validationMode);

        TestAccount delegateAccount = new TestAccount(delegateID);
        AccountProperties.setProperty(delegateAccount, new RegistrationDataProperty(delegate.getPublicKey()));
        AccountProperties.setProperty(delegateAccount, new BalanceProperty(100L));
        VotePollsProperty voter = new VotePollsProperty();
        voter.setPoll(baseAccountID, 5);
        AccountProperties.setProperty(delegateAccount, voter);

        TestAccount delegateAccount1 = new TestAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new BalanceProperty(100L));
        AccountProperties.setProperty(delegateAccount, new RegistrationDataProperty(delegate_1.getPublicKey()));

        ledger.putAccount(baseAccount);
        ledger.putAccount(delegateAccount);
        ledger.putAccount(delegateAccount1);
    }

    @Test
    public void unknown_account() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);
        when(ledger.getAccount(new AccountID(delegate.getPublicKey()))).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void invalid_attchment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction tx =
                RejectionBuilder.createNew(baseAccount.getID()).withParam("param", "value").build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void target_account_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID())
                                         .withParam("account", "value")
                                         .build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void target_account_not_exist() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TARGET_ACCOUNT_NOT_FOUND);
        when(ledger.getAccount(baseAccount.getID())).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void target_account_not_in_mfa() throws Exception {
        expectedException.expect(ValidateException.class);
        // expectedException.expectMessage("The delegates list is not specified.");
        expectedException.expectMessage(Resources.ACCOUNT_NOT_IN_VOTE_POLL);
        when(baseAccount.getProperty(PropertyType.MODE)).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void unknown_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);

        when(ledger.getAccount(new AccountID(delegate_1.getPublicKey()))).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate_1);
        validate(tx);
    }

    @Test
    public void invalid_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_NOT_IN_VOTE_POLL);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate_1);
        validate(tx);
    }

    @Test
    public void reject() throws Exception {
        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void reject_itself() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_NOT_MATCH_DATA);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, base);
        validate(tx);
    }

    @Test
    public void rejection_impossible() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.REJECTION_NOT_POSSIBLE);

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
        validationMode.setWeightForAccount(new AccountID(delegate.getPublicKey()), 70);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(baseAccount, validationMode);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void rejection_impossible_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.REJECTION_NOT_POSSIBLE);

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setWeightForAccount(new AccountID(delegate.getPublicKey()), 70);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(baseAccount, validationMode);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(networkID, delegate);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID())
                                         .addNested(new TransactionBuilder(1).build(networkID, delegate))
                                         .build(networkID, delegate);

        validate(tx);
    }
}
