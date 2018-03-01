package com.exscudo.peer.eon.tx.rules;

import static org.mockito.Mockito.spy;

import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ed25519.Ed25519SignatureVerifier;
import com.exscudo.peer.core.crypto.mapper.SignedObjectMapper;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.tx.ITransactionParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class AbstractParserTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    protected TimeProvider timeProvider = spy(new TimeProvider());
    protected DefaultLedger ledger = spy(new DefaultLedger());

    protected abstract ITransactionParser getParser();

    @Before
    public void setUp() throws Exception {
        Ed25519SignatureVerifier signatureVerifier = new Ed25519SignatureVerifier();
        CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(new BlockID(0L)));
        cryptoProvider.addProvider(signatureVerifier);
        cryptoProvider.setDefaultProvider(signatureVerifier.getName());
        CryptoProvider.init(cryptoProvider);
    }

    protected void validate(Transaction tx) throws Exception {

        ILedgerAction[] actions = getParser().parse(tx);

        ILedger newLedger = ledger;
        for (ILedgerAction action : actions) {
            newLedger = action.run(newLedger, new TransactionContext(timeProvider.get()));
        }
    }
}
