package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
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
