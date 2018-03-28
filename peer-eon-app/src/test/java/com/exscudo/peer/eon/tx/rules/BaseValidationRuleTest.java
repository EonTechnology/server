package com.exscudo.peer.eon.tx.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISignatureVerifier;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BaseValidationRuleTest extends AbstractValidationRuleTest {
    private BaseValidationRule rule = new BaseValidationRule();
    private Transaction transaction;
    private int timestamp;

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        CryptoProvider.getInstance().addProvider(new DummyVerifier(false));
        CryptoProvider.getInstance().setDefaultProvider("test");

        Account account = spy(new DefaultAccount(new AccountID(new byte[32])));
        AccountProperties.setProperty(account, new RegistrationDataProperty(new byte[32]));
        AccountProperties.setProperty(account, new BalanceProperty(5L));
        when(ledger.getAccount(any())).thenReturn(account);

        timestamp = timeProvider.get();
        when(timeProvider.get()).thenReturn(timestamp);

        transaction = spy(new Transaction());
        transaction.setType(10);
        transaction.setVersion(1);
        transaction.setTimestamp(timestamp);
        transaction.setDeadline((short) 1);
        transaction.setReference(null);
        transaction.setSenderID(new AccountID(1L));
        transaction.setFee(10L);
        transaction.setData(new HashMap<>());
        transaction.setSignature(new byte[0]);
    }

    @Test
    public void validate_OK() throws Exception {
        validate(transaction);
    }

    @Ignore
    @Test
    public void validate_experied() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid timestamp or other params for set the time.");

        transaction.setTimestamp(timestamp - transaction.getDeadline() * 60 - 1 - Constant.BLOCK_PERIOD);
        validate(transaction);
    }

    @Test
    public void validate_deadline_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid timestamp or other params for set the time.");

        transaction.setDeadline((short) 0);
        validate(transaction);
    }

    @Test
    public void validate_deadline_max() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid timestamp or other params for set the time.");
        transaction.setDeadline((short) (EonConstant.TRANSACTION_MAX_LIFETIME + 1));
        validate(transaction);
    }

    @Test
    public void validate_fee_zero() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid fee.");
        transaction.setFee(0L);
        validate(transaction);
    }

    @Test
    public void validate_fee_small_1() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid fee.");
        transaction.setFee(1L);
        validate(transaction);
    }

    @Test
    public void validate_max_length() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid transaction length.");

        when(transaction.getLength()).thenReturn(EonConstant.TRANSACTION_MAX_PAYLOAD_LENGTH + 1);
        validate(transaction);
    }

    private static class DummyVerifier implements ISignatureVerifier {
        private final boolean value;

        public DummyVerifier(boolean value) {
            this.value = value;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
            return value;
        }
    }
}
