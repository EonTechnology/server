package com.exscudo.peer.eon;

import static org.mockito.Mockito.when;

import java.util.HashMap;

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
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.parsers.QuorumParser;
import com.exscudo.peer.tx.TransactionType;
import com.exscudo.peer.tx.midleware.builders.QuorumBuilder;
import com.exscudo.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class QuorumTransactionTest extends AbstractTransactionTest {
    private QuorumParser parser = new QuorumParser();

    private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account senderAccount;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
        AccountProperties.setProperty(senderAccount, new RegistrationDataProperty(sender.getPublicKey()));
        AccountProperties.setProperty(senderAccount, new BalanceProperty(5000L));

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 10);
        AccountProperties.setProperty(senderAccount, validationMode);

        TestAccount delegateAccount1 = new TestAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new RegistrationDataProperty(delegate_1.getPublicKey()));

        ledger.putAccount(senderAccount);
        ledger.putAccount(delegateAccount1);
    }

    @Test
    public void attachment_unknown_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);
        Transaction tx = new TransactionBuilder(TransactionType.Quorum, null).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void unknown_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.SENDER_ACCOUNT_NOT_FOUND);

        when(ledger.getAccount(new AccountID(sender.getPublicKey()))).thenReturn(null);
        Transaction tx = QuorumBuilder.createNew(70).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void unset_default_quorum() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_INVALID_FORMAT);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, new HashMap<>()).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_type_not_int() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TRANSACTION_TYPE_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50L);
        map.put("*", 70L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void default_quorum_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", ValidationModeProperty.MAX_QUORUM + 1L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void default_quorum_out_of_range_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", ValidationModeProperty.MIN_QUORUM - 1L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void default_quorum_invalid_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_CAN_NOT_BE_CHANGED);

        Transaction tx = QuorumBuilder.createNew(90).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_for_unknown_transaction_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.TRANSACTION_TYPE_UNKNOWN);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50L);
        map.put(String.valueOf(TransactionType.Payment), 70L);
        map.put(String.valueOf(100500), 30L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_for_all_typed() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_ILLEGAL_USAGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50L);
        map.put(String.valueOf(TransactionType.Payment), 50L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_not_int() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_INVALID_FORMAT);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put(String.valueOf(TransactionType.Payment), "value");
        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50L);
        map.put(String.valueOf(TransactionType.Payment), ValidationModeProperty.MAX_QUORUM + 1L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_out_of_range_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_OUT_OF_RANGE);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50L);
        map.put(String.valueOf(TransactionType.Payment), ValidationModeProperty.MIN_QUORUM - 1L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void quorum_invalid_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.QUORUM_FOR_TYPE_CAN_NOT_BE_CHANGED);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50L);
        map.put(String.valueOf(TransactionType.Payment), 90L);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction tx = QuorumBuilder.createNew(90)
                                      .addNested(new TransactionBuilder(1).build(networkID, sender))
                                      .build(networkID, sender);

        validate(tx);
    }

    @Test
    public void success() throws Exception {

        ISigner accountSigner = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
        TestAccount account = new TestAccount(new AccountID(accountSigner.getPublicKey()));
        AccountProperties.setProperty(account, new RegistrationDataProperty(delegate_1.getPublicKey()));
        AccountProperties.setProperty(account, new BalanceProperty(1000L));
        ledger.putAccount(account);

        Transaction tx = QuorumBuilder.createNew(50).build(networkID, accountSigner);
        validate(tx);

        Account a = ledger.getAccount(account.getID());
        Assert.assertEquals(AccountProperties.getValidationMode(a).getBaseQuorum(), 50);
    }
}
