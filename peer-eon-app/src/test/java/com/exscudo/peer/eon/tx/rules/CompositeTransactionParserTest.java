package com.exscudo.peer.eon.tx.rules;

import java.util.HashMap;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.tx.CompositeTransactionParser;
import com.exscudo.peer.eon.tx.ITransactionParser;
import org.junit.Before;
import org.junit.Test;

public class CompositeTransactionParserTest extends AbstractParserTest {

    private CompositeTransactionParser parser =
            CompositeTransactionParser.create().addParser(1, new ITransactionParser() {
                @Override
                public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
                    return new ILedgerAction[0];
                }
            }).build();

    private Transaction tx;

    @Override
    protected ITransactionParser getParser() {
        return parser;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void unknown_transaction_type() throws Exception {
        expectedException.expect(ValidateException.class);
        expectedException.expectMessage("Invalid transaction type. Type :10");

        ISigner signer = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

        tx = new Transaction();
        tx.setType(10);
        tx.setVersion(1);
        tx.setTimestamp(timeProvider.get());
        tx.setDeadline(3600);
        tx.setReference(null);
        tx.setSenderID(new AccountID(signer.getPublicKey()));
        tx.setFee(1L);
        tx.setData(new HashMap<>());
        tx.setSignature(signer.sign(tx.getBytes()));

        validate(tx);
    }

    @Test
    public void success() throws Exception {

        ISigner signer = new Ed25519Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

        tx = new Transaction();
        tx.setType(1);
        tx.setVersion(1);
        tx.setTimestamp(timeProvider.get());
        tx.setDeadline(3600);
        tx.setReference(null);
        tx.setSenderID(new AccountID(signer.getPublicKey()));
        tx.setFee(1L);
        tx.setData(new HashMap<>());
        tx.setSignature(signer.sign(tx.getBytes()));

        validate(tx);
    }
}
