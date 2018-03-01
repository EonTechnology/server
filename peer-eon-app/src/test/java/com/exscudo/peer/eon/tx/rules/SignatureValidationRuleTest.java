package com.exscudo.peer.eon.tx.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISignatureVerifier;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SignatureValidationRuleTest extends AbstractValidationRuleTest {
    private SignatureValidationRule rule = new SignatureValidationRule();
    private Transaction transaction;

    @Override
    protected IValidationRule getValidationRule() {
        return rule;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Account account = Mockito.spy(new DefaultAccount(new AccountID(new byte[32])));
        AccountProperties.setProperty(account, new RegistrationDataProperty(new byte[32]));
        AccountProperties.setProperty(account, new BalanceProperty(5L));
        when(ledger.getAccount(any())).thenReturn(account);

        transaction = new Transaction();
        transaction.setFee(1L);
        transaction.setVersion(1);
        transaction.setSenderID(new AccountID(1L));
        transaction.setDeadline((short) 1);
        transaction.setTimestamp(1000);
    }

    @Test
    public void validate_sender_unknown() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Unknown sender.");

        when(ledger.getAccount(any())).thenReturn(null);
        validate(transaction);
    }

    @Test
    public void validate_sender_illegal_signature() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Illegal signature.");

        CryptoProvider.getInstance().addProvider(new DummyVerifier(false));
        CryptoProvider.getInstance().setDefaultProvider("test");

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
