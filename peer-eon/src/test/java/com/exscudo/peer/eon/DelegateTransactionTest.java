package com.exscudo.peer.eon;

import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.Signer;
import com.exscudo.peer.TestAccount;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.DelegateParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.DelegateBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegateTransactionTest extends AbstractTransactionTest {
    private DelegateParser parser = new DelegateParser();

    private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate_2 = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
    private Account senderAccount;
    private Account delegateAccount1;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 50);
        AccountProperties.setProperty(senderAccount, validationMode);

        delegateAccount1 = new TestAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new RegistrationDataProperty(delegate_1.getPublicKey()));

        ledger.putAccount(senderAccount);
        ledger.putAccount(delegateAccount1);
    }

    @Test
    public void attachment_unknown_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction tx = new TransactionBuilder(TransactionType.Delegate).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void duplicate_account() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 60).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegate_id_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ACCOUNT_ID_INVALID_FORMAT);

        AccountID id = new AccountID(sender.getPublicKey());
        HashMap<String, Object> map = new HashMap<>();
        map.put("delegate id", 50);
        Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegate_weight_invalid_format() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_INVALID_FORMAT);

        AccountID id = new AccountID(sender.getPublicKey());
        HashMap<String, Object> map = new HashMap<>();
        map.put(id.toString(), "weight");
        Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void weight_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_OUT_OF_RANGE);

        AccountID id = new AccountID(sender.getPublicKey());
        HashMap<String, Object> map = new HashMap<>();
        map.put(id.toString(), ValidationModeProperty.MAX_WEIGHT + 1L);
        Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void weight_out_of_range_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_OUT_OF_RANGE);

        AccountID id = new AccountID(sender.getPublicKey());
        HashMap<String, Object> map = new HashMap<>();
        map.put(id.toString(), ValidationModeProperty.MIN_WEIGHT - 1L);
        Transaction tx = new TransactionBuilder(TransactionType.Delegate, map).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void enable_mfa() throws Exception {
        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 50).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void invalid_weight() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VOTES_INCORRECT_DISTRIBUTION);

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void public_mode() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_PROHIBITED_ACTION);

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 50);
        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 0).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void unknown_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);

        when(ledger.getAccount(new AccountID(sender.getPublicKey()))).thenReturn(null);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 50).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void unknown_account() throws Exception {
        AccountID id = new AccountID(delegate_2.getPublicKey());
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.DELEGATE_ACCOUNT_NOT_FOUND);

        Transaction tx = DelegateBuilder.createNew(id, 50).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegate_invalid_weight() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VOTES_INCORRECT_DISTRIBUTION);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 20).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void add_delegate() throws Exception {
        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void remove_delegate() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 50);
        validationMode.setWeightForAccount(new AccountID(delegate_2.getPublicKey()), 50);
        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 0).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void public_account_as_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_PROHIBITED_ACTION);

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
        validationMode.setWeightForAccount(new AccountID(delegate_2.getPublicKey()), 100);
        AccountProperties.setProperty(delegateAccount1, validationMode);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegates_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TOO_MACH_SIZE);

        ValidationModeProperty validationMode = new ValidationModeProperty();

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegates_pre_limit() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();

        AccountID id = new AccountID(delegate_2.getPublicKey());
        TestAccount delegateAccount2 = new TestAccount(id);
        AccountProperties.setProperty(delegateAccount2, new RegistrationDataProperty(delegate_2.getPublicKey()));

        ledger.putAccount(delegateAccount2);

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE - 1) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegates_limit_self() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        AccountID id = new AccountID(sender.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegates_limit_exist() throws Exception {
        ValidationModeProperty validationMode = new ValidationModeProperty();

        AccountID id = new AccountID(delegate_1.getPublicKey());
        validationMode.setWeightForAccount(id, 50);

        int i = 0;
        while (validationMode.delegatesEntrySet().size() < Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE) {
            validationMode.setWeightForAccount(new AccountID(i++), 50);
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = DelegateBuilder.createNew(id, 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegates_same_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

        ValidationModeProperty validationMode = new ValidationModeProperty();

        AccountID id = new AccountID(delegate_1.getPublicKey());
        validationMode.setWeightForAccount(id, 50);

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = DelegateBuilder.createNew(id, 50).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegates_same_value_new() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

        AccountID id = new AccountID(delegate_2.getPublicKey());
        TestAccount delegateAccount2 = new TestAccount(id);
        AccountProperties.setProperty(delegateAccount2, new RegistrationDataProperty(delegate_2.getPublicKey()));

        ledger.putAccount(delegateAccount2);

        Transaction tx = DelegateBuilder.createNew(id, 0).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, sender);
        AccountID id = new AccountID(delegate_1.getPublicKey());
        Transaction tx = DelegateBuilder.createNew(id, 0).addNested(innerTx).build(networkID, sender);

        validate(parser, tx);
    }

    @Test
    public void exceed_vote_polls_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TOO_MACH_SIZE);

        VotePollsProperty votePollsProperty = new VotePollsProperty();
        int index = 0;
        while (!votePollsProperty.isFull()) {
            votePollsProperty.setPoll(new AccountID(index++), 10);
        }
        AccountProperties.setProperty(delegateAccount1, votePollsProperty);

        Transaction tx = DelegateBuilder.createNew(delegateAccount1.getID(), 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void full_vote_polls_limit() throws Exception {
        VotePollsProperty votePollsProperty = new VotePollsProperty();
        int index = 0;
        votePollsProperty.setPoll(new AccountID(sender.getPublicKey()), 10);
        while (!votePollsProperty.isFull()) {
            votePollsProperty.setPoll(new AccountID(index++), 10);
        }
        AccountProperties.setProperty(delegateAccount1, votePollsProperty);

        Transaction tx = DelegateBuilder.createNew(delegateAccount1.getID(), 40).build(networkID, sender);
        validate(parser, tx);
    }

    @Test
    public void delegate_error_null() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_INVALID_FORMAT);

        Transaction tx = DelegateBuilder.createNew(delegateAccount1.getID(), 40).build(networkID, sender);

        tx.getData().put(delegateAccount1.getID().toString(), null);
        validate(parser, tx);
    }

    @Test
    public void delegate_error_string() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_INVALID_FORMAT);

        Transaction tx = DelegateBuilder.createNew(delegateAccount1.getID(), 40).build(networkID, sender);

        tx.getData().put(delegateAccount1.getID().toString(), "10");
        validate(parser, tx);
    }

    @Test
    public void delegate_error_decimal() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_INVALID_FORMAT);

        Transaction tx = DelegateBuilder.createNew(delegateAccount1.getID(), 40).build(networkID, sender);

        tx.getData().put(delegateAccount1.getID().toString(), 10.001);
        validate(parser, tx);
    }

    @Test
    public void delegate_error_over() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.WEIGHT_OUT_OF_RANGE);

        Transaction tx = DelegateBuilder.createNew(delegateAccount1.getID(), 40).build(networkID, sender);

        tx.getData().put(delegateAccount1.getID().toString(), 50 + 0xFFFFFFFFL);
        validate(parser, tx);
    }
}
