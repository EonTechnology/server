package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;

public class ReferencedTransactionValidationRuleTest extends AbstractValidationRuleTest {
    private ReferencedTransactionValidationRule rule;
    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new ReferencedTransactionValidationRule();
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void success_without_note() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal reference.");

        Transaction tx = Builder.newTransaction(timeProvider).refBy(new TransactionID(12345L)).build(networkID, sender);
        validate(tx);
    }
}
