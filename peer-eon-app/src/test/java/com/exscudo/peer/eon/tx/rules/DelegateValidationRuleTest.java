package com.exscudo.peer.eon.tx.rules;

import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.DelegateBuilder;
import com.exscudo.peer.eon.tx.builders.TransactionBuilder;
import com.exscudo.peer.eon.tx.parsers.DelegateParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegateValidationRuleTest extends AbstractParserTest {
    private DelegateParser parser = new DelegateParser();

    private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate_2 = new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
    private Account senderAccount;
    private Account delegateAccount1;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        senderAccount = Mockito.spy(new DefaultAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 50);
        AccountProperties.setProperty(senderAccount, validationMode);

        delegateAccount1 = new DefaultAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new RegistrationDataProperty(delegate_1.getPublicKey()));

        ledger.putAccount(senderAccount);
        ledger.putAccount(delegateAccount1);
    }

    @Test
    public void duplicate_account() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Value already set.");

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 60).build(sender);
        validate(tx);
    }

    @Test
    public void weight_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid " + new AccountID(sender.getPublicKey()) + " account weight.");

        AccountID id = new AccountID(sender.getPublicKey());
        HashMap<String, Object> map = new HashMap<>();
        map.put(id.toString(), ValidationModeProperty.MAX_WEIGHT + 1);
        Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(sender);
        validate(tx);
    }

    @Test
    public void weight_out_of_range_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid " + new AccountID(sender.getPublicKey()) + " account weight.");

        AccountID id = new AccountID(sender.getPublicKey());
        HashMap<String, Object> map = new HashMap<>();
        map.put(id.toString(), ValidationModeProperty.MIN_WEIGHT - 1);
        Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(sender);
        validate(tx);
    }

    @Test
    public void enable_mfa() throws Exception {
        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 50).build(sender);
        validate(tx);
    }

    @Test
    public void invalid_weight() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Incorrect distribution of votes.");

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void public_mode() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Changing rights is prohibited.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 50);
        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 0).build(sender);
        validate(tx);
    }

    @Test
    public void unknown_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account.");

        when(ledger.getAccount(new AccountID(sender.getPublicKey()))).thenReturn(null);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 50).build(sender);
        validate(tx);
    }

    @Test
    public void unknown_account() throws Exception {
        AccountID id = new AccountID(delegate_2.getPublicKey());
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account " + id);

        Transaction tx = DelegateBuilder.createNew(id, 50).build(sender);
        validate(tx);
    }

    @Test
    public void delegate_invalid_weight() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Incorrect distribution of votes.");

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 20).build(sender);
        validate(tx);
    }

    @Test
    public void add_delegate() throws Exception {
        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void remove_delegate() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 50);
        validationMode.setWeightForAccount(new AccountID(delegate_2.getPublicKey()), 50);
        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id).build(sender);
        validate(tx);
    }

    @Test
    public void public_account_as_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("A public account can not act as a delegate.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
        validationMode.setWeightForAccount(new AccountID(delegate_2.getPublicKey()), 100);
        AccountProperties.setProperty(delegateAccount1, validationMode);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void delegates_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The number of delegates has reached the limit.");

        ValidationModeProperty validationMode = new ValidationModeProperty();

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < ValidationModeProperty.MAX_DELEGATES) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void delegates_pre_limit() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();

        AccountID id = new AccountID(delegate_2.getPublicKey());
        DefaultAccount delegateAccount2 = new DefaultAccount(id);
        AccountProperties.setProperty(delegateAccount2, new RegistrationDataProperty(delegate_2.getPublicKey()));

        ledger.putAccount(delegateAccount2);

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < ValidationModeProperty.MAX_DELEGATES - 1) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void delegates_limit_self() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < ValidationModeProperty.MAX_DELEGATES) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void delegates_limit_exist() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();

        AccountID id = new AccountID(delegate_1.getPublicKey());
        validationMode.setWeightForAccount(id, 50);

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < ValidationModeProperty.MAX_DELEGATES) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = DelegateBuilder.createNew(id, 40).build(sender);
        validate(tx);
    }

    @Test
    public void delegates_same_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Value already set.");

        ValidationModeProperty validationMode = new ValidationModeProperty();

        AccountID id = new AccountID(delegate_1.getPublicKey());
        validationMode.setWeightForAccount(id, 50);

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = DelegateBuilder.createNew(id, 50).build(sender);
        validate(tx);
    }

    @Test
    public void delegates_same_value_new() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Value already set.");

        AccountID id = new AccountID(delegate_2.getPublicKey());
        DefaultAccount delegateAccount2 = new DefaultAccount(id);
        AccountProperties.setProperty(delegateAccount2, new RegistrationDataProperty(delegate_2.getPublicKey()));

        ledger.putAccount(delegateAccount2);

        Transaction tx = DelegateBuilder.createNew(id, 0).build(sender);
        validate(tx);
    }
}
