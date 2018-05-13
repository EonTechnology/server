package com.exscudo.eon.app.IT;

import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
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

    private IFork fork1;
    private IFork fork2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter ps1 = PeerStarterFactory.create(GENERATOR, mockTimeProvider);
        fork1 = Mockito.spy(ps1.getFork());
        ps1.setFork(fork1);

        PeerStarter ps2 = PeerStarterFactory.create(GENERATOR2, mockTimeProvider);
        fork2 = Mockito.spy(ps2.getFork());
        ps2.setFork(fork2);

        ctx1 = new PeerContext(ps1);
        ctx2 = new PeerContext(ps2);
    }

    @Test
    public void step_1_input() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                       .validity(lastBlock.getTimestamp(), 3600)
                                       .build(new BlockID(1L), ctx1.getSigner());

        try {
            ctx1.transactionBotService.putTransaction(tx);
            throw new Error("Exception must throw in put");
        } catch (Exception ignored) {
        }

        Assert.assertNull("Transaction not accepted", ctx1.backlogExplorerService.get(tx.getID()));
    }

    @Test
    public void step_2_syncTran() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        BlockID genesisBlockID = fork1.getGenesisBlockID();
        Mockito.when(fork1.getGenesisBlockID()).thenReturn(new BlockID(1L));
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                       .validity(lastBlock.getTimestamp(), 3600)
                                       .build(new BlockID(1L), ctx1.getSigner());
        ctx1.transactionBotService.putTransaction(tx);

        Mockito.when(fork1.getGenesisBlockID()).thenReturn(genesisBlockID);

        ctx2.setPeerToConnect(ctx1);
        ctx2.syncTransactionListTask.run();

        Assert.assertNotNull("Transaction accepted in (1)", ctx1.backlogExplorerService.get(tx.getID()));
        Assert.assertNull("Transaction not accepted in (2)", ctx2.backlogExplorerService.get(tx.getID()));
    }

    @Test
    public void step_3_syncBlock() throws Exception {

        ctx2.setPeerToConnect(ctx1);

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx1.generateBlockForNow();

        ctx2.fullBlockSync();
        Assert.assertNotEquals("Normal block accepted in (2)",
                               lastBlock.getID(),
                               ctx2.blockExplorerService.getLastBlock().getID());

        Mockito.when(fork1.getGenesisBlockID()).thenReturn(new BlockID(1L));
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        Transaction tx = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                       .validity(lastBlock.getTimestamp(), 3600)
                                       .build(ctx1.getNetworkID(), ctx1.getSigner());

        ctx1.transactionBotService.putTransaction(tx);
        ctx1.generateBlockForNow();

        ctx2.fullBlockSync();

        Block lastBlock1 = ctx1.blockExplorerService.getLastBlock();
        Block lastBlock2 = ctx2.blockExplorerService.getLastBlock();
        Assert.assertNotEquals("New block generated in (1)", lastBlock.getID(), lastBlock1.getID());
        Assert.assertNotNull("Transaction accepted in (1)", ctx1.transactionExplorerService.getById(tx.getID()));
        Assert.assertNotEquals("Blockchain not synchronized (2)", lastBlock1.getID(), lastBlock2.getID());
    }
}
