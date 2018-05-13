package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;

public class DeadlineValidationRuleTest extends AbstractValidationRuleTest {
    private DeadlineValidationRule rule;

    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new DeadlineValidationRule();
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void deadline_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid timestamp or other params for set the time.");

        Transaction tx = Builder.newTransaction(timestamp).deadline((short) 0).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void deadline_max() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid timestamp or other params for set the time.");

        Transaction tx = Builder.newTransaction(timeProvider)
                                .deadline((short) (Constant.TRANSACTION_MAX_LIFETIME + 1))
                                .build(networkID, sender);
        validate(tx);
    }
}
