package com.exscudo.peer.eon.tx.rules;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ConfirmationsValidationRuleTest extends AbstractValidationRuleTest {
    private ConfirmationsValidationRule rule = new ConfirmationsValidationRule();

    private ISigner sender = new Ed25519Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner delegate_1 = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate_2 = new Ed25519Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

    private Account senderAccount;

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
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
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 5);
        validationMode.setWeightForAccount(new AccountID(delegate_2.getPublicKey()), 15);
        validationMode.setQuorum(50);
        validationMode.setQuorum(TransactionType.Payment, 70);
        AccountProperties.setProperty(senderAccount, validationMode);

        DefaultAccount delegateAccount1 = new DefaultAccount(new AccountID(delegate_1.getPublicKey()));
        AccountProperties.setProperty(delegateAccount1, new RegistrationDataProperty(delegate_1.getPublicKey()));

        DefaultAccount delegateAccount2 = new DefaultAccount(new AccountID(delegate_2.getPublicKey()));
        AccountProperties.setProperty(delegateAccount2, new RegistrationDataProperty(delegate_2.getPublicKey()));

        ledger.putAccount(senderAccount);
        ledger.putAccount(delegateAccount1);
        ledger.putAccount(delegateAccount2);
    }

    @Test
    public void unset_mfa_property() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid use of the confirmation field.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(ValidationModeProperty.MAX_WEIGHT);
        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, new ISigner[] {
                delegate_1, delegate_2
        });
        validate(tx);
    }

    @Test
    public void duplicate_confirmation() throws Exception {

        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Duplicates sender signature.");

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, new ISigner[] {sender});
        validate(tx);
    }

    @Test
    public void unknown_delegate() throws Exception {

        AccountID id = new AccountID(delegate_1.getPublicKey());
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account " + id);

        when(ledger.getAccount(id)).thenReturn(null);

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, new ISigner[] {delegate_1});
        validate(tx);
    }

    @Test
    public void unspecified_delegate() throws Exception {

        AccountID id = new AccountID(delegate_2.getPublicKey());
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Account '" + id + "' can not sign transaction.");

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 20);
        validationMode.setQuorum(50);
        validationMode.setQuorum(TransactionType.Payment, 70);
        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, new ISigner[] {
                delegate_1, delegate_2
        });
        validate(tx);
    }

    @Test
    public void illegal_confirmation() throws Exception {
        expectedException.expect(IllegalSignatureException.class);

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, new ISigner[] {
                delegate_1, delegate_2
        });
        String key = tx.getConfirmations().keySet().iterator().next();
        tx.getConfirmations().put(key, Format.convert(new byte[32]));
        validate(tx);
    }

    @Test
    public void invalid_quorum_without_confirmation() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The quorum is not exist.");

        Account mockAccount = mock(Account.class);
        when(ledger.getAccount(new AccountID(12345L))).thenReturn(mockAccount);

        Transaction tx = PaymentBuilder.createNew(100, new AccountID(12345L)).build(sender, new ISigner[] {});
        validate(tx);
    }

    @Test
    public void invalid_quorum_with_confirmation() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("The quorum is not exist.");

        Account mockAccount = mock(Account.class);
        when(ledger.getAccount(new AccountID(12345L))).thenReturn(mockAccount);

        Transaction tx = PaymentBuilder.createNew(100, new AccountID(12345L)).build(sender, new ISigner[] {delegate_1});
        validate(tx);
    }

    @Test
    public void quorum_with_confirmation() throws Exception {
        Account mockAccount = mock(Account.class);
        when(ledger.getAccount(new AccountID(12345L))).thenReturn(mockAccount);

        Transaction tx = PaymentBuilder.createNew(100, new AccountID(12345L)).build(sender, new ISigner[] {
                delegate_1, delegate_2
        });
        validate(tx);
    }

    @Test
    public void quorum_with_partial_confirmation() throws Exception {

        Account mockAccount = mock(Account.class);
        when(ledger.getAccount(new AccountID(12345L))).thenReturn(mockAccount);

        ValidationModeProperty validationMode = new ValidationModeProperty();
        validationMode.setBaseWeight(60);
        validationMode.setWeightForAccount(new AccountID(delegate_1.getPublicKey()), 5);
        validationMode.setQuorum(50);
        validationMode.setQuorum(TransactionType.Payment, 70);
        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = PaymentBuilder.createNew(100, new AccountID(12345L)).build(sender, new ISigner[] {delegate_1});
        validate(tx);
    }

    @Test
    public void quorum_without_confirmation() throws Exception {
        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, new ISigner[] {});
        validate(tx);
    }

    @Test
    public void confirmation_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid use of the confirmation field.");

        ValidationModeProperty validationMode = new ValidationModeProperty();

        Random r = new Random(123);
        ISigner[] signers = new ISigner[ValidationModeProperty.MAX_DELEGATES + 1];

        for (int i = 0; i < ValidationModeProperty.MAX_DELEGATES + 1; i++) {

            byte[] seed = new byte[32];
            r.nextBytes(seed);
            ISigner signer = new Ed25519Signer(Format.convert(seed));

            DefaultAccount account = new DefaultAccount(new AccountID(signer.getPublicKey()));
            AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));

            when(ledger.getAccount(account.getID())).thenReturn(account);
            validationMode.setWeightForAccount(account.getID(), 50);

            signers[i] = signer;
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, signers);

        validate(tx);
    }

    @Test
    public void confirmation_limit_pre() throws Exception {

        ValidationModeProperty validationMode = new ValidationModeProperty();

        Random r = new Random(123);
        ISigner[] signers = new ISigner[ValidationModeProperty.MAX_DELEGATES];

        for (int i = 0; i < ValidationModeProperty.MAX_DELEGATES; i++) {

            byte[] seed = new byte[32];
            r.nextBytes(seed);
            ISigner signer = new Ed25519Signer(Format.convert(seed));

            DefaultAccount account = new DefaultAccount(new AccountID(signer.getPublicKey()));
            AccountProperties.setProperty(account, new RegistrationDataProperty(signer.getPublicKey()));

            when(ledger.getAccount(account.getID())).thenReturn(account);
            validationMode.setWeightForAccount(account.getID(), 50);

            signers[i] = signer;
        }

        AccountProperties.setProperty(senderAccount, validationMode);

        Transaction tx = RegistrationBuilder.createNew(new byte[32]).build(sender, signers);

        validate(tx);
    }
}
