package com.exscudo.peer.eon.tx.rules;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import com.exscudo.peer.eon.tx.parsers.RegistrationParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RegistrationValidationRuleTest extends AbstractParserTest {
    private RegistrationParser parser = new RegistrationParser();

    private ISigner senderSigner =
            new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
    private Account sender;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        sender = Mockito.spy(new DefaultAccount(new AccountID(senderSigner.getPublicKey())));
        AccountProperties.setProperty(sender, new RegistrationDataProperty(senderSigner.getPublicKey()));

        ledger = spy(new DefaultLedger());
        ledger.putAccount(sender);
    }

    @Test
    public void account_re_registration() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Account already exists.");

        AccountProperties.setProperty(sender, new BalanceProperty(1000L));
        Transaction tx = RegistrationBuilder.createNew(senderSigner.getPublicKey()).build(senderSigner);
        validate(tx);
    }

    @Test
    public void account_invalid_attachment() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Attachment of unknown type.");

        Transaction tx = spy(RegistrationBuilder.createNew(new byte[0]).build(senderSigner));
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("data", "test");
        when(tx.getData()).thenReturn(map);
        resolveSignature(tx);

        validate(tx);
    }

    private void resolveSignature(Transaction tx) {
        byte[] bytes = tx.getBytes();
        byte[] signature = senderSigner.sign(bytes);
        tx.setSignature(signature);
    }
}
