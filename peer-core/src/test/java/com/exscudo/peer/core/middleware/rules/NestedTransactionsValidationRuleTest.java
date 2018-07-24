package com.exscudo.peer.core.middleware.rules;

import java.util.HashSet;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.TestAccount;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class NestedTransactionsValidationRuleTest extends AbstractValidationRuleTest {
    private NestedTransactionsValidationRule rule;

    private ISigner signer = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner signer1 = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private ISigner signer2 = new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ledger.putAccount(new TestAccount(new AccountID(signer.getPublicKey())));
        ledger.putAccount(new TestAccount(new AccountID(signer1.getPublicKey())));
        ledger.putAccount(new TestAccount(new AccountID(signer2.getPublicKey())));

        Mockito.when(accountHelper.getConfirmingAccounts(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
               .then(new Answer<Object>() {
                   @Override
                   public Object answer(InvocationOnMock invocation) throws Throwable {
                       return null;
                   }
               });
        rule = new NestedTransactionsValidationRule(new HashSet<Integer>() {{
            add(1);
        }}, timeProvider, accountHelper);
    }

    @Test
    public void one_nested() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider)
                                .addNested(Builder.newTransaction(timestamp - 1).forFee(0L).build(networkID, signer1))
                                .build(networkID, signer);
        validate(tx);
    }

    @Test
    public void illegal_usage() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal usage.");

        Transaction tx = Builder.newTransaction(timeProvider)
                                .addNested(Builder.newTransaction(timestamp - 1).build(networkID, signer1))
                                .build(networkID, signer);

        tx.getNestedTransactions().clear();
        validate(tx);
    }

    @Test
    public void nested_transaction_older_than() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid nested transaction. Invalid timestamp.");

        Transaction tx = Builder.newTransaction(timestamp)
                                .addNested(Builder.newTransaction(timestamp - 1).forFee(0L).build(networkID, signer1))
                                .addNested(Builder.newTransaction(timestamp + 2).forFee(0L).build(networkID, signer2))
                                .build(networkID, signer);
        validate(tx);
    }

    @Test
    public void nested_transaction_non_zero_fee() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid nested transaction. A fee must be equal a zero.");

        Transaction nestedTx1 = Builder.newTransaction(timestamp - 1).forFee(0L).build(networkID, signer1);
        Transaction nestedTx2 =
                Builder.newTransaction(timestamp - 1).refBy(nestedTx1.getID()).build(networkID, signer2);

        Transaction tx =
                Builder.newTransaction(timestamp).addNested(nestedTx1).addNested(nestedTx2).build(networkID, signer);
        validate(tx);
    }

    @Test
    public void nested_transaction_has_nested_transactions() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage(
                "Invalid nested transaction. Nested transactions are allowed only at the top level.");

        Transaction nestedTx1 = Builder.newTransaction(timestamp - 1)
                                       .forFee(0L)
                                       .addNested(Builder.newTransaction(timestamp).build(networkID, signer))
                                       .build(networkID, signer1);
        Transaction nestedTx2 =
                Builder.newTransaction(timestamp - 1).forFee(0L).refBy(nestedTx1.getID()).build(networkID, signer2);

        Transaction tx =
                Builder.newTransaction(timestamp).addNested(nestedTx1).addNested(nestedTx2).build(networkID, signer);
        validate(tx);
    }

    @Test
    public void success() throws Exception {

        Transaction nestedTx1 = Builder.newTransaction(timestamp - 1).forFee(0L).build(networkID, signer1);
        Transaction nestedTx2 =
                Builder.newTransaction(timestamp - 1).forFee(0L).refBy(nestedTx1.getID()).build(networkID, signer2);
        Transaction nestedTx3 =
                Builder.newTransaction(timestamp - 1).forFee(0L).refBy(nestedTx2.getID()).build(networkID, signer1);

        Transaction tx = Builder.newTransaction(timestamp)
                                .addNested(nestedTx1)
                                .addNested(nestedTx2)
                                .addNested(nestedTx3)
                                .build(networkID, signer);
        validate(tx);
    }

    @Test
    public void success_without_nested_transaction() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, signer);
        validate(tx);
    }

    @Test
    public void add_payer() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider)
                                .addNested(Builder.newTransaction(timestamp - 1)
                                                  .forFee(0L)
                                                  .payedBy(new AccountID(signer.getPublicKey()))
                                                  .build(networkID, signer1))
                                .addNested(Builder.newTransaction(timestamp).forFee(0L).build(networkID, signer))
                                .build(networkID, signer);
        validate(tx);
    }

    @Test
    public void illegal_payer() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid nested transaction. Invalid payer.");

        Transaction tx = Builder.newTransaction(timeProvider)
                                .addNested(Builder.newTransaction(timestamp - 1)
                                                  .forFee(0L)
                                                  .payedBy(new AccountID(signer2.getPublicKey()))
                                                  .build(networkID, signer1))
                                .addNested(Builder.newTransaction(timestamp).forFee(0L).build(networkID, signer))
                                .build(networkID, signer);
        validate(tx);
    }

    @Test
    public void illegal_payer_confirmations() throws Exception {
        AccountID id = new AccountID(signer.getPublicKey());

        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid nested transaction. Account '" + id + "' can not sign transaction.");

        Transaction tx = Builder.newTransaction(timeProvider)
                                .addNested(Builder.newTransaction(timestamp - 1)
                                                  .forFee(0L)
                                                  .payedBy(id)
                                                  .build(networkID, signer1, new ISigner[] {signer}))
                                .addNested(Builder.newTransaction(timestamp).forFee(0L).build(networkID, signer))
                                .build(networkID, signer);
        validate(tx);
    }

    @Test
    public void nested_transaction_reference_ok() throws Exception {
        Transaction nestedTx1 = Builder.newTransaction(timestamp - 1).forFee(0L).build(networkID, signer1);
        Transaction nestedTx2 =
                Builder.newTransaction(timestamp - 1).forFee(0L).refBy(nestedTx1.getID()).build(networkID, signer2);

        Transaction tx =
                Builder.newTransaction(timestamp).addNested(nestedTx1).addNested(nestedTx2).build(networkID, signer);
        validate(tx);
    }

    @Test
    public void nested_transaction_reference_fail() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid nested transaction. Invalid reference.");

        Transaction nestedTx1 = Builder.newTransaction(timestamp - 1).forFee(0L).build(networkID, signer1);
        Transaction nestedTx2 = Builder.newTransaction(timestamp - 1)
                                       .forFee(0L)
                                       .refBy(new TransactionID(123L))
                                       .build(networkID, signer2);

        Transaction tx =
                Builder.newTransaction(timestamp).addNested(nestedTx1).addNested(nestedTx2).build(networkID, signer);
        validate(tx);
    }
}
