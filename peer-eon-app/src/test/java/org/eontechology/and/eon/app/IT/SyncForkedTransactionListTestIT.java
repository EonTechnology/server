package org.eontechology.and.eon.app.IT;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import org.eontechology.and.peer.core.common.TimeProvider;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.env.Peer;
import org.eontechology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechology.and.peer.tx.TransactionType;
import org.eontechology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@SuppressWarnings("WeakerAccess")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SyncForkedTransactionListTestIT {

    private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx1;
    private PeerContext ctx2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        ctx1 = new PeerContext(PeerStarterFactory.create()
                                                 .route(TransactionType.Payment, new PaymentParser())
                                                 .seed(GENERATOR)
                                                 .build(mockTimeProvider));
        ctx2 = new PeerContext(PeerStarterFactory.create()
                                                 .route(TransactionType.Payment, new PaymentParser())
                                                 .seed(GENERATOR2)
                                                 .build(mockTimeProvider));

        ctx1.syncBlockPeerService = Mockito.spy(ctx1.syncBlockPeerService);
        ctx2.syncBlockPeerService = Mockito.spy(ctx2.syncBlockPeerService);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);
    }

    @Test
    public void step_1_too_many_blocks() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * (Constant.BLOCK_IN_DAY + 2) + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        Thread thread1 = new Thread(() -> ctx1.generateBlockForNow());
        Thread thread2 = new Thread(() -> ctx2.generateBlockForNow());

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        ctx1.syncBlockListTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertNotEquals("Blockchain not synchronized",
                               ctx1.blockExplorerService.getLastBlock().getID(),
                               ctx2.blockExplorerService.getLastBlock().getID());

        lastBlock = ctx1.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);

        Transaction tx1 = PaymentBuilder.createNew(10000L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 150, 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());
        Transaction tx2 = PaymentBuilder.createNew(10000L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 100, 3600)
                                        .build(ctx2.getNetworkID(), ctx1.getSigner());

        ctx1.transactionBotService.putTransaction(tx1);
        ctx2.transactionBotService.putTransaction(tx2);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * 2 + 1);

        SyncForkedTransactionListTask task1 = ctx1.syncForkedTransactionListTask;
        SyncForkedTransactionListTask task2 = ctx2.syncForkedTransactionListTask;

        Peer peer1 = ctx1.context.getAnyConnectedPeer();
        if (peer1 != null) {
            ctx1.context.disablePeer(peer1);
        }

        Peer peer2 = ctx2.context.getAnyConnectedPeer();
        if (peer2 != null) {
            ctx2.context.disablePeer(peer2);
        }

        task1.run();
        task2.run();

        Assert.assertNotNull(ctx1.transactionExplorerService.getById(tx1.getID()));
        Assert.assertNotNull(ctx2.transactionExplorerService.getById(tx2.getID()));

        Assert.assertNotNull(ctx1.backlogExplorerService.get(tx2.getID()));
        Assert.assertNotNull(ctx2.backlogExplorerService.get(tx1.getID()));
    }
}
