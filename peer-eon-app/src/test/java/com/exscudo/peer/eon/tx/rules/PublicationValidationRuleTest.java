package com.exscudo.peer.eon.tx.rules;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.Constant;
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
import com.exscudo.peer.eon.tx.builders.PublicationBuilder;
import com.exscudo.peer.eon.tx.builders.TransactionBuilder;
import com.exscudo.peer.eon.tx.parsers.PublicationParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PublicationValidationRuleTest extends AbstractParserTest {
    private PublicationParser parser = new PublicationParser();

    private String seed = "112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
    private String seed_1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";

    private ISigner signer = new Ed25519Signer(seed);
    private ISigner signer_1 = new Ed25519Signer(seed_1);

    private Account account;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        account = Mockito.spy(new DefaultAccount(new AccountID(signer.getPublicKey())));
        AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));
        AccountProperties.setProperty(account, new BalanceProperty(100L));

        DefaultAccount account_1 = new DefaultAccount(new AccountID(signer_1.getPublicKey()));
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
        Transaction tx = new TransactionBuilder(TransactionType.Registration, map).build(signer);
        validate(tx);
    }

    @Test
    public void invalid_argument_number_in_attach() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        Transaction tx = new TransactionBuilder(TransactionType.Registration, new HashMap<>()).build(signer);
        validate(tx);
    }

    @Test
    public void invalid_seed() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid seed.");

        Map<String, Object> map = new HashMap<>();
        map.put("seed", "seedseedseedseed");
        Transaction tx = new TransactionBuilder(TransactionType.Registration, map).build(signer);
        validate(tx);
    }

    @Test
    public void invalid_sender() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Seed for sender account must be specified in attachment.");

        Transaction tx = PublicationBuilder.createNew(seed).build(signer_1);
        validate(tx);
    }

    @Test
    public void validation_mode_property_undefined() throws Exception {
        expectedException.expect(ValidateException.class);
        // expectedException.expectMessage("Invalid use of transaction.");
        expectedException.expectMessage("Illegal validation mode. Do not use this seed more for personal operations.");

        Transaction tx = PublicationBuilder.createNew(seed).build(signer);
        validate(tx);
    }

    @Test
    public void already_public_mode() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Already public.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setPublicMode(seed);
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        AccountProperties.setProperty(account, validationMode);

        Transaction tx = PublicationBuilder.createNew(seed).build(signer);
        validate(tx);
    }

    @Test
    public void invalid_uses_too_early() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The confirmation mode were changed earlier than a day ago." +
                                                " Do not use this seed more for personal operations.");

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        validationMode.setTimestamp(timestamp);
        validationMode.setBaseWeight(0);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx = PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(signer);

        when(timeProvider.get()).thenReturn(timestamp1 - 1);
        validate(tx);
    }

    @Test
    public void invalid_uses_empty_delegate_list() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal validation mode. Do not use this seed more for personal operations.");

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setTimestamp(timestamp);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx = PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(tx);
    }

    @Test
    public void invalid_uses_sender_weight_not_equal_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal validation mode." +
                                                " Do not use this seed more for personal operations.");

        int timestamp = Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(70);
        validationMode.setWeightForAccount(new AccountID(signer_1.getPublicKey()), 100);
        validationMode.setTimestamp(timestamp);
        AccountProperties.setProperty(account, validationMode);

        int timestamp1 = timestamp + Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
        Transaction tx = PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(tx);
    }

    @Test
    public void account_is_delegate() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("A public account must not confirm transactions of other accounts." +
                                                " Do not use this seed more for personal operations.");

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
        Transaction tx = PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(tx);
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
        Transaction tx = PublicationBuilder.createNew(seed).validity(timestamp1 - 30 * 60, 60 * 60).build(signer);

        when(timeProvider.get()).thenReturn(timestamp1 + 1);
        validate(tx);
    }
}
