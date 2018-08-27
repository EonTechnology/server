package com.exscudo.peer.core.middleware.rules;

import java.util.HashSet;

import com.exscudo.peer.core.Builder;
import com.exscudo.peer.core.Signer;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.middleware.AbstractValidationRuleTest;
import com.exscudo.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;

public class TypeValidationRuleTest extends AbstractValidationRuleTest {
    private TypeValidationRule rule;
    private ISigner sender = new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        rule = new TypeValidationRule(new HashSet<Integer>() {{
            add(10);
        }});
    }

    @Test
    public void success() throws Exception {
        Transaction tx = Builder.newTransaction(timeProvider).type(10).build(networkID, sender);
        validate(tx);
    }

    @Test
    public void unknown_type() throws Exception {
        int type = 12345;

        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid transaction type. Type :" + type);

        Transaction tx = Builder.newTransaction(timeProvider).type(type).build(networkID, sender);
        validate(tx);
    }
}
