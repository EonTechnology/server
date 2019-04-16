package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Signer;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.middleware.AbstractValidationRuleTest;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;

public class FeePerByteValidationRuleTest extends AbstractValidationRuleTest {
    private FeePerByteValidationRule rule;
    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new FeePerByteValidationRule();
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void fee_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid fee.");

        Transaction tx = Builder.newTransaction(timeProvider).forFee(0L).build(networkID, sender);
        tx.setLength(1);
        validate(tx);
    }

    @Test
    public void too_small_fee() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid fee.");

        Transaction tx = Builder.newTransaction(timeProvider).forFee(10L).build(networkID, sender);
        tx.setLength(1024 + 1);
        validate(tx);
    }
}
