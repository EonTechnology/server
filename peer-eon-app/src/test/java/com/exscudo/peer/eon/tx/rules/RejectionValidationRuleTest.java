package com.exscudo.peer.eon.tx.rules;

import static org.mockito.Mockito.when;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.RejectionBuilder;
import com.exscudo.peer.eon.tx.parsers.RejectionParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RejectionValidationRuleTest extends AbstractParserTest {
    private ISigner base = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

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
        baseAccount = Mockito.spy(new DefaultAccount(baseAccountID));
        AccountProperties.setProperty(baseAccount, new RegistrationDataProperty(base.getPublicKey()));
        AccountProperties.setProperty(baseAccount, new BalanceProperty(100L));

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(delegateID, 5);
        validationMode.setQuorum(50);
        validationMode.setQuorum(TransactionType.Payment, 70);
        AccountProperties.setProperty(baseAccount, validationMode);

        DefaultAccount delegateAccount = new DefaultAccount(delegateID);
        AccountProperties.setProperty(delegateAccount, new RegistrationDataProperty(delegate.getPublicKey()));
        AccountProperties.setProperty(delegateAccount, new BalanceProperty(100L));
        VotePollsProperty voter = new VotePollsProperty();
        voter.setPoll(baseAccountID, 5);
        AccountProperties.setProperty(delegateAccount, voter);

        DefaultAccount delegateAccount1 = new DefaultAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new BalanceProperty(100L));
        AccountProperties.setProperty(delegateAccount, new RegistrationDataProperty(delegate_1.getPublicKey()));

        ledger.putAccount(baseAccount);
        ledger.putAccount(delegateAccount);
        ledger.putAccount(delegateAccount1);
    }

    @Test
    public void unknown_account() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account.");
        when(ledger.getAccount(new AccountID(delegate.getPublicKey()))).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
        validate(tx);
    }

    @Test
    public void target_account_not_exist() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown target account.");
        when(ledger.getAccount(baseAccount.getID())).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
        validate(tx);
    }

    @Test
    public void target_account_not_in_mfa() throws Exception {
        expectedException.expect(ValidateException.class);
        // expectedException.expectMessage("The delegates list is not specified.");
        expectedException.expectMessage("Account does not participate in transaction confirmation.");
        when(baseAccount.getProperty(PropertyType.MODE)).thenReturn(null);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
        validate(tx);
    }

    @Test
    public void unknown_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Account does not participate in transaction confirmation.");

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate_1);
        validate(tx);
    }

    @Test
    public void reject() throws Exception {
        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
        validate(tx);
    }

    @Test
    public void reject_itself() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal account.");

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(base);
        validate(tx);
    }

    @Test
    public void rejection_impossible() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Rejection is not possible.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
        validationMode.setWeightForAccount(new AccountID(delegate.getPublicKey()), 70);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(baseAccount, validationMode);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
        validate(tx);
    }

    @Test
    public void rejection_impossible_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Rejection is not possible.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setWeightForAccount(new AccountID(delegate.getPublicKey()), 70);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(baseAccount, validationMode);

        Transaction tx = RejectionBuilder.createNew(baseAccount.getID()).build(delegate);
        validate(tx);
    }
}
