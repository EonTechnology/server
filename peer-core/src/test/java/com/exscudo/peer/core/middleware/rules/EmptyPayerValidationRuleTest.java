package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;

public class EmptyPayerValidationRuleTest extends AbstractValidationRuleTest {
    private EmptyPayerValidationRule rule;
    private ISigner sender = new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    private ISigner payer = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private AccountID payerAccount = new AccountID(payer.getPublicKey());

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new EmptyPayerValidationRule();
    }

    @Test
    public void payer_error() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Forbidden.");

        Transaction tx = Builder.newTransaction(timeProvider)
                                .payedBy(payerAccount)
                                .build(networkID, sender, new ISigner[] {payer});
        validate(tx);
    }

    @Test
    public void payer_success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }
}
