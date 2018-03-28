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
import com.exscudo.peer.eon.tx.builders.QuorumBuilder;
import com.exscudo.peer.eon.tx.builders.TransactionBuilder;
import com.exscudo.peer.eon.tx.parsers.QuorumParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class QuorumValidationRuleTest extends AbstractParserTest {
    private QuorumParser parser = new QuorumParser();

    private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account senderAccount;

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
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 10);
        AccountProperties.setProperty(senderAccount, validationMode);

        DefaultAccount delegateAccount1 = new DefaultAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new RegistrationDataProperty(delegate_1.getPublicKey()));

        ledger.putAccount(senderAccount);
        ledger.putAccount(delegateAccount1);
    }

    @Test
    public void unknown_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account.");

        when(ledger.getAccount(new AccountID(sender.getPublicKey()))).thenReturn(null);
        Transaction tx = QuorumBuilder.createNew(70).build(sender);
        validate(tx);
    }

    @Test
    public void unset_default_quorum() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, new HashMap<>()).build(sender);
        validate(tx);
    }

    @Test
    public void quorum_not_int() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put("*", 70);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void default_quorum_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal quorum.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", ValidationModeProperty.MAX_QUORUM + 1);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void default_quorum_out_of_range_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal quorum.");

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", ValidationModeProperty.MIN_QUORUM - 1);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void default_quorum_invalid_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unable to set quorum.");

        Transaction tx = QuorumBuilder.createNew(90).build(sender);
        validate(tx);
    }

    @Test
    public void quorum_for_unknown_transaction_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown transaction type 100500");

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put(String.valueOf(TransactionType.Payment), 70);
        map.put(String.valueOf(100500), 30);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void quorum_for_all_typed() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Use all quorum for transaction type 200");

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put(String.valueOf(TransactionType.Payment), 50);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void quorum_out_of_range() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal quorum for transaction type " + TransactionType.Payment);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put(String.valueOf(TransactionType.Payment), ValidationModeProperty.MAX_QUORUM + 1);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void quorum_out_of_range_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal quorum for transaction type " + TransactionType.Payment);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put(String.valueOf(TransactionType.Payment), ValidationModeProperty.MIN_QUORUM - 1);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }

    @Test
    public void quorum_invalid_value() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unable to set quorum for transaction type " + TransactionType.Payment);

        HashMap<String, Object> map = new HashMap<>();
        map.put("all", 50);
        map.put(String.valueOf(TransactionType.Payment), 90);

        Transaction tx = new TransactionBuilder(TransactionType.Quorum, map).build(sender);
        validate(tx);
    }
}
