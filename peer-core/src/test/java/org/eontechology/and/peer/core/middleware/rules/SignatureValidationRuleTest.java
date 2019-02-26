package org.eontechology.and.peer.core.middleware.rules;

import java.util.HashMap;
import java.util.Map;

import org.eontechology.and.peer.core.Builder;
import org.eontechology.and.peer.core.Signer;
import org.eontechology.and.peer.core.common.Format;
import org.eontechology.and.peer.core.common.exceptions.IllegalSignatureException;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.core.middleware.AbstractValidationRuleTest;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.TestAccount;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SignatureValidationRuleTest extends AbstractValidationRuleTest {
    private SignatureValidationRule rule;
    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate1 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner delegate2 = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Account senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
        ledger.putAccount(senderAccount);

        Account delegate1Account = Mockito.spy(new TestAccount(new AccountID(delegate1.getPublicKey())));
        ledger.putAccount(delegate1Account);

        Account delegate2Account = Mockito.spy(new TestAccount(new AccountID(delegate2.getPublicKey())));
        ledger.putAccount(delegate2Account);

        rule = new SignatureValidationRule(timeProvider, accountHelper);
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success_with_confirmations() throws Exception {
        Transaction tx =
                Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {delegate1, delegate2});
        validate(tx);
    }

    @Test
    public void sender_unknown() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown sender.");

        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        Mockito.when(ledger.getAccount(tx.getSenderID())).thenReturn(null);
        validate(tx);
    }

    @Test
    public void delegate_unknown() throws Exception {
        AccountID unknownID = new AccountID(delegate2.getPublicKey());
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown account " + unknownID.toString());

        Mockito.when(ledger.getAccount(ArgumentMatchers.any())).thenAnswer(new Answer<Account>() {
            @Override
            public Account answer(InvocationOnMock invocationOnMock) throws Throwable {
                AccountID id = invocationOnMock.getArgument(0);
                if (id.equals(unknownID)) {
                    return null;
                }
                return (Account) invocationOnMock.callRealMethod();
            }
        });

        Transaction tx =
                Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {delegate1, delegate2});
        validate(tx);
    }

    @Test
    public void illegal_signature() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal signature.");

        Mockito.when(accountHelper.verifySignature(ArgumentMatchers.any(),
                                                   ArgumentMatchers.any(),
                                                   ArgumentMatchers.any(),
                                                   ArgumentMatchers.anyInt())).thenReturn(false);
        Transaction tx = Builder.newTransaction(timeProvider).build(new BlockID(100500L), sender);
        validate(tx);
    }

    @Test
    public void invalid_confirmation() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid format.");

        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("account-id", Format.convert(new byte[64]));
        tx.setConfirmations(map);
        validate(tx);
    }

    @Test
    public void duplicate_confirmation() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Duplicates sender signature.");

        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {sender});
        validate(tx);
    }

    @Test
    public void illegal_confirmation() throws Exception {
        expectedException.expect(IllegalSignatureException.class);
        Mockito.when(accountHelper.verifySignature(ArgumentMatchers.any(),
                                                   ArgumentMatchers.any(),
                                                   ArgumentMatchers.any(),
                                                   ArgumentMatchers.anyInt())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                Account account = invocationOnMock.getArgument(2);
                if (account.getID().equals(new AccountID(delegate2.getPublicKey()))) {
                    return false;
                }
                return true;
            }
        });

        Transaction tx =
                Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {delegate1, delegate2});
        validate(tx);
    }
}
