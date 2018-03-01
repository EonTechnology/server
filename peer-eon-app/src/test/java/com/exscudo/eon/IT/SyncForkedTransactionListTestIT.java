package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
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
        ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
        ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

        ctx1.syncBlockPeerService = Mockito.spy(ctx1.syncBlockPeerService);
        ctx2.syncBlockPeerService = Mockito.spy(ctx2.syncBlockPeerService);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);
    }

    @Test
    public void step_1_too_many_blocks() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

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
                               ctx1.blockchain.getLastBlock().getID(),
                               ctx2.blockchain.getLastBlock().getID());

        lastBlock = ctx1.blockchain.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);

        Transaction tx1 = PaymentBuilder.createNew(10000L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 150, 3600)
                                        .build(ctx1.getSigner());
        Transaction tx2 = PaymentBuilder.createNew(10000L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 100, 3600)
                                        .build(ctx1.getSigner());

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

        Assert.assertTrue(ctx1.transactionProvider.containsTransaction(tx1.getID()));
        Assert.assertTrue(ctx2.transactionProvider.containsTransaction(tx2.getID()));

        Assert.assertTrue(ctx1.backlog.contains(tx2.getID()));
        Assert.assertTrue(ctx2.backlog.contains(tx1.getID()));
    }
}
