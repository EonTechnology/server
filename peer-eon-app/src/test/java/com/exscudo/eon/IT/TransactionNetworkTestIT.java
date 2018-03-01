package com.exscudo.eon.IT;

import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ed25519.Ed25519SignatureVerifier;
import com.exscudo.peer.core.crypto.mapper.SignedObjectMapper;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionNetworkTestIT {

    private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx1;
    private PeerContext ctx2;

    private CryptoProvider cryptoProvider1;
    private CryptoProvider cryptoProvider2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
        ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

        Ed25519SignatureVerifier signatureVerifier = new Ed25519SignatureVerifier();
        BlockID genesisBlockID = ctx1.fork.getGenesisBlockID();

        cryptoProvider1 = new CryptoProvider(new SignedObjectMapper(genesisBlockID));
        cryptoProvider1.addProvider(signatureVerifier);
        cryptoProvider1.setDefaultProvider(signatureVerifier.getName());

        cryptoProvider2 = new CryptoProvider(new SignedObjectMapper(new BlockID(1L)));
        cryptoProvider2.addProvider(signatureVerifier);
        cryptoProvider2.setDefaultProvider(signatureVerifier.getName());
        CryptoProvider.init(cryptoProvider1);
    }

    @Test
    public void step_1_input() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        CryptoProvider.init(cryptoProvider2);

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                       .validity(lastBlock.getTimestamp(), 3600)
                                       .build(ctx1.getSigner());

        CryptoProvider.init(cryptoProvider1);

        try {
            ctx1.transactionBotService.putTransaction(tx);
            throw new Error("Exception must throw in put");
        } catch (Exception ignored) {
        }

        Assert.assertFalse("Transaction not accepted", ctx1.backlog.contains(tx.getID()));
    }

    @Test
    public void step_2_syncTran() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        CryptoProvider.init(cryptoProvider2);

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                       .validity(lastBlock.getTimestamp(), 3600)
                                       .build(ctx1.getSigner());

        ctx1.transactionBotService.putTransaction(tx);

        CryptoProvider.init(cryptoProvider1);

        ctx2.setPeerToConnect(ctx1);
        ctx2.syncTransactionListTask.run();

        Assert.assertTrue("Transaction accepted in (1)", ctx1.backlog.contains(tx.getID()));
        Assert.assertFalse("Transaction not accepted in (2)", ctx2.backlog.contains(tx.getID()));
    }

    @Test
    public void step_3_syncBlock() throws Exception {

        ctx2.setPeerToConnect(ctx1);

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        CryptoProvider.init(cryptoProvider2);

        ctx1.generateBlockForNow();

        ctx2.fullBlockSync();
        Assert.assertNotEquals("Normal block accepted in (2)",
                               lastBlock.getID(),
                               ctx2.blockchain.getLastBlock().getID());

        CryptoProvider.init(cryptoProvider2);

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                       .validity(lastBlock.getTimestamp(), 3600)
                                       .build(ctx1.getSigner());

        ctx1.transactionBotService.putTransaction(tx);
        ctx1.generateBlockForNow();

        CryptoProvider.init(cryptoProvider1);

        ctx2.fullBlockSync();

        Assert.assertNotEquals("New block generated in (1)", lastBlock.getID(), ctx1.blockchain.getLastBlock().getID());
        Assert.assertTrue("Transaction accepted in (1)", ctx1.transactionProvider.containsTransaction(tx.getID()));
        Assert.assertNotEquals("Blockchain not synchronized (2)",
                               ctx1.blockchain.getLastBlock().getID(),
                               ctx2.blockchain.getLastBlock().getID());
    }
}
