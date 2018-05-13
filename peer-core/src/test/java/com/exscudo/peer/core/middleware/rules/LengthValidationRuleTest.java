package com.exscudo.peer.core.middleware.rules;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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

public class LengthValidationRuleTest extends AbstractValidationRuleTest {
    private LengthValidationRule rule;
    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new LengthValidationRule();
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void length_exceeds_limit() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid transaction length.");

        Transaction tx = spy(Builder.newTransaction(timeProvider).build(networkID, sender));
        when(tx.getLength()).thenReturn(Constant.TRANSACTION_MAX_PAYLOAD_LENGTH + 1);
        validate(tx);
    }
}
