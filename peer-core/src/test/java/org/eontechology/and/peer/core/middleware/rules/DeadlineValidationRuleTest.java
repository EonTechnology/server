package org.eontechology.and.peer.core.middleware.rules;

import org.eontechology.and.peer.core.Builder;
import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.Signer;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.middleware.AbstractValidationRuleTest;
import org.eontechology.and.peer.core.middleware.IValidationRule;
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
