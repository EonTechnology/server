package org.eontechnology.and.peer.eon;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eontechnology.and.peer.Signer;
import org.eontechnology.and.peer.TestAccount;
import org.eontechnology.and.peer.TestSignature;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechnology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechnology.and.peer.eon.ledger.state.VotePollsProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.parsers.PublicationParser;
import org.eontechnology.and.peer.tx.TransactionType;
import org.eontechnology.and.peer.tx.midleware.builders.PublicationBuilder;
import org.eontechnology.and.peer.tx.midleware.builders.TransactionBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PublicationTransactionTest extends AbstractTransactionTest {
    private PublicationParser parser;

    private String seed = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
    private String seed_1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";

    private ISigner signer = new Signer(seed);
    private ISigner signer_1 = new Signer(seed_1);

    private Account account;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        parser = new PublicationParser(new TestSignature());

        account = Mockito.spy(new TestAccount(new AccountID(signer.getPublicKey())));
        AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        AccountProperties.setProperty(account, new BalanceProperty(100L));

        TestAccount account_1 = new TestAccount(new AccountID(signer_1.getPublicKey()));
        AccountProperties.setProperty(account_1, new RegistrationDataProperty(signer_1.getPublicKey()));
        AccountProperties.setProperty(account_1, new BalanceProperty(100L));

        ledger.putAccount(account);
        ledger.putAccount(account_1);
    }

    @Test
    public void unspecified_seed() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The 'seed' field is not specified.");

        Map<String, Object> map = new HashMap<>();
        map.put("param", "value");
        Transaction tx = new TransactionBuilder(TransactionType.Registration, map).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void invalid_argument_number_in_attach() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.ATTACHMENT_UNKNOWN_TYPE);

        Transaction tx = new TransactionBuilder(TransactionType.Registration, new HashMap<>()).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void invalid_seed() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_INVALID_SEED);

        Map<String, Object> map = new HashMap<>();
        map.put("seed", null);
        Transaction tx = new TransactionBuilder(TransactionType.Registration, map).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void invalid_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_SEED_NOT_MATCH);

        Transaction tx = PublicationBuilder.createNew(seed).build(networkID, signer_1);
        validate(parser, tx);
    }

    @Test
    public void validation_mode_property_undefined() throws Exception {
        expectedException.expect(ValidateException.class);
        // expectedException.expectMessage("Invalid use of transaction.");
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_INVALID_WEIGHT);

        Transaction tx = PublicationBuilder.createNew(seed).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void already_public_mode() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.VALUE_ALREADY_SET);

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode(seed);
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        AccountProperties.setProperty(account, validationMode);

        Transaction tx = PublicationBuilder.createNew(seed).build(networkID, signer);
        validate(parser, tx);
    }

    @Test
    public void invalid_uses_too_early() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_RECENTLY_CHANGED);

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        validationMode.setTimestamp(timestamp);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx =
                PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(networkID, signer);

        when(timeProvider.get()).thenReturn(timestamp1 - 1);
        validate(parser, tx);
    }

    @Test
    public void invalid_uses_empty_delegate_list() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_INVALID_WEIGHT);

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setTimestamp(timestamp);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx =
                PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(networkID, signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(parser, tx);
    }

    @Test
    public void invalid_uses_sender_weight_not_equal_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_INVALID_WEIGHT);

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(70);
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        validationMode.setTimestamp(timestamp);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx =
                PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(networkID, signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(parser, tx);
    }

    @Test
    public void account_is_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.PUBLIC_ACCOUNT_PARTICIPATES_IN_VOTE_POLLS);

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        validationMode.setTimestamp(timestamp);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(account, validationMode);
        VotePollsProperty voter = new VotePollsProperty();
        voter.setPoll(new AccountID(signer_1.getPublicKey()), 10);
        AccountProperties.setProperty(account, voter);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx =
                PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(networkID, signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(parser, tx);
    }

    @Test
    public void invalid_nested_transaction() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);

        Transaction innerTx = new TransactionBuilder(1).build(networkID, signer);
        Transaction tx = PublicationBuilder.createNew(seed)
                                           .validity(timeProvider.get(), 60 * 60)
                                           .addNested(innerTx)
                                           .build(networkID, signer);

        validate(parser, tx);
    }

    @Test
    public void success() throws Exception {
        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        validationMode.setTimestamp(timestamp);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx =
                PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(networkID, signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(parser, tx);
    }
}
