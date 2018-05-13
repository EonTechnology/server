package com.exscudo.eon.app.IT;

import java.time.Instant;

import com.exscudo.eon.app.cfg.Fork;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.eon.app.cfg.forks.Item;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@SuppressWarnings("WeakerAccess")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Fork1TestIT {

    protected static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    protected static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    protected static int BEGIN_H = 3;
    protected static int END_H = 6;
    protected static int END_H2 = 9;
    protected static int BEGIN = Constant.BLOCK_PERIOD * BEGIN_H;
    protected static int END = Constant.BLOCK_PERIOD * END_H;
    protected static int END2 = Constant.BLOCK_PERIOD * END_H2;
    protected static int FORK = 2;
    protected TimeProvider mockTimeProvider;
    protected Fork forkState1;
    protected Fork forkState2;
    protected PeerContext ctx1;
    protected PeerContext ctx2;

    @Before
    public void setUp() throws Exception {

        mockTimeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter peerStarter1 = PeerStarterFactory.create(GENERATOR, mockTimeProvider);
        long timestamp = Utils.getLastBlock(peerStarter1.getStorage()).getTimestamp();

        String i1_begin = getTimeString(timestamp - 1);
        String i2_begin = getTimeString(timestamp + Fork1TestIT.BEGIN);
        String i3_begin = getTimeString(timestamp + Fork1TestIT.END);
        String i4_begin = getTimeString(timestamp + Fork1TestIT.END2);

        forkState1 = new Fork(Utils.getGenesisBlockID(peerStarter1.getStorage()), new Item[] {
                new Utils.TestItem(Fork1TestIT.FORK - 1, i1_begin), new Utils.TestItem(Fork1TestIT.FORK, i2_begin)
        }, i3_begin);
        forkState1.setMinDepositSize(Utils.MIN_DEPOSIT_SIZE);
        peerStarter1.setFork(forkState1);

        ctx1 = new PeerContext(peerStarter1);

        PeerStarter peerStarter2 = PeerStarterFactory.create(GENERATOR2, mockTimeProvider);

        BlockID networkID2 = Utils.getGenesisBlockID(peerStarter2.getStorage());
        forkState2 = new Fork(Utils.getGenesisBlockID(peerStarter2.getStorage()), new Item[] {
                new Utils.TestItem(Fork1TestIT.FORK - 1, i1_begin),
                new Utils.TestItem(Fork1TestIT.FORK, i2_begin),
                new Utils.TestItem(Fork1TestIT.FORK + 1, i3_begin)
        }, i4_begin);
        forkState2.setMinDepositSize(Utils.MIN_DEPOSIT_SIZE);
        peerStarter2.setFork(forkState2);
        ctx2 = new PeerContext(peerStarter2);
    }

    private String getTimeString(long timestamp) {
        return Instant.ofEpochMilli((timestamp) * 1000L).toString();
    }

    @Test
    public void step_1_1_isCome() throws Exception {
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Assert.assertFalse("Before fork", forkState1.isCome(lastBlock.getTimestamp() + BEGIN - 50));
        Assert.assertTrue("On fork", forkState1.isCome(lastBlock.getTimestamp() + BEGIN + 50));
        Assert.assertTrue("On fork", forkState1.isCome(lastBlock.getTimestamp() + END - 50));
        Assert.assertTrue("After fork", forkState1.isCome(lastBlock.getTimestamp() + END + 50));
    }

    @Test
    public void step_1_2_isPassed() throws Exception {
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Assert.assertFalse("Before fork", forkState1.isPassed(lastBlock.getTimestamp() + BEGIN - 50));
        Assert.assertFalse("On fork", forkState1.isPassed(lastBlock.getTimestamp() + BEGIN + 50));
        Assert.assertFalse("On fork", forkState1.isPassed(lastBlock.getTimestamp() + END - 50));
        Assert.assertTrue("After fork", forkState1.isPassed(lastBlock.getTimestamp() + END + 50));
    }

    @Test
    public void step_1_3_getNumber() throws Exception {
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Assert.assertEquals("Before fork", 1, forkState1.getNumber(lastBlock.getTimestamp() + BEGIN - 50));
        Assert.assertEquals("On fork", 2, forkState1.getNumber(lastBlock.getTimestamp() + BEGIN + 50));
        Assert.assertEquals("On fork", 2, forkState1.getNumber(lastBlock.getTimestamp() + END - 50));
        Assert.assertEquals("After fork", 2, forkState1.getNumber(lastBlock.getTimestamp() + END + 50));
    }

    @Test
    public void step_2_1_CheckGeneratorIsDisabled() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        BlockID lastBlockID;

        do {
            lastBlockID = lastBlock.getID();

            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
            ctx1.generateBlockForNow();

            lastBlock = ctx1.blockExplorerService.getLastBlock();

            if (!lastBlock.getID().equals(lastBlockID)) {
                Assert.assertFalse(forkState1.isPassed(lastBlock.getTimestamp()));
            } else {
                Assert.assertTrue(forkState1.isPassed(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD));
            }
        } while (!lastBlockID.equals(lastBlock.getID()));

        Assert.assertEquals("Blockchain max possible height",
                            END / Constant.BLOCK_PERIOD,
                            ctx1.blockExplorerService.getLastBlock().getHeight());
    }

    @Test
    public void step_2_2_CheckGeneratorIsDisabled() throws Exception {

        Block lastBlock = ctx2.blockExplorerService.getLastBlock();
        BlockID lastBlockID;

        do {
            lastBlockID = lastBlock.getID();

            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
            ctx2.generateBlockForNow();

            lastBlock = ctx2.blockExplorerService.getLastBlock();

            if (!lastBlock.getID().equals(lastBlockID)) {
                Assert.assertFalse(forkState2.isPassed(lastBlock.getTimestamp()));
            } else {
                Assert.assertTrue(forkState2.isPassed(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD));
            }
        } while (!lastBlockID.equals(lastBlock.getID()));

        Assert.assertEquals("Blockchain max possible height",
                            END2 / Constant.BLOCK_PERIOD,
                            ctx2.blockExplorerService.getLastBlock().getHeight());
    }

    @Test
    public void step_3_1_CheckBlockSyncIsDisabled() throws Exception {

        ctx1.setPeerToConnect(ctx2);

        Block lastBlock = ctx2.blockExplorerService.getLastBlock();
        BlockID lastBlockID;

        do {
            lastBlockID = lastBlock.getID();

            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
            ctx2.generateBlockForNow();
            ctx1.fullBlockSync();

            lastBlock = ctx2.blockExplorerService.getLastBlock();

            if (ctx1.blockExplorerService.getLastBlock()
                                         .getID()
                                         .equals(ctx2.blockExplorerService.getLastBlock().getID())) {
                Assert.assertFalse(forkState1.isPassed(lastBlock.getTimestamp()));
                Assert.assertFalse(forkState2.isCome(lastBlock.getTimestamp()));
            } else {
                Assert.assertTrue(forkState1.isPassed(lastBlock.getTimestamp()));
                Assert.assertTrue(forkState2.isCome(lastBlock.getTimestamp()));
            }
        } while (!lastBlockID.equals(lastBlock.getID()));

        Assert.assertEquals("Blockchain max possible height",
                            END / Constant.BLOCK_PERIOD,
                            ctx1.blockExplorerService.getLastBlock().getHeight());
        Assert.assertEquals("Blockchain max possible height",
                            END2 / Constant.BLOCK_PERIOD,
                            ctx2.blockExplorerService.getLastBlock().getHeight());
    }

    @Test
    public void step_3_2_CheckBlockSyncIsDisabled() throws Exception {

        ctx1.setPeerToConnect(ctx2);

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        int time = lastBlock.getTimestamp() + END2 * 2 + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx2.generateBlockForNow();
        ctx1.fullBlockSync();

        Block lastBlock1 = ctx1.blockExplorerService.getLastBlock();
        Block lastBlock2 = ctx2.blockExplorerService.getLastBlock();
        Assert.assertNotEquals("Blockchain not synchronized", lastBlock1.getID(), lastBlock2.getID());
        Assert.assertEquals("Blockchain synchronized to possible height",
                            END / Constant.BLOCK_PERIOD,
                            lastBlock1.getHeight());
        Assert.assertEquals("Blockchain max possible height", END2 / Constant.BLOCK_PERIOD, lastBlock2.getHeight());
    }

    @Test
    public void step_4_CheckTranSyncIsDisabled() throws Exception {

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        Block lastBlock = ctx2.blockExplorerService.getLastBlock();
        BlockID lastBlockID;

        do {
            lastBlockID = lastBlock.getID();

            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

            Transaction tx1 = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                            .validity(lastBlock.getTimestamp(), 3600)
                                            .build(ctx1.getNetworkID(), ctx1.getSigner());
            Transaction tx2 = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                            .validity(lastBlock.getTimestamp() + 1, 3600)
                                            .build(ctx1.getNetworkID(), ctx1.getSigner());

            ctx1.transactionBotService.putTransaction(tx1);
            ctx2.transactionBotService.putTransaction(tx2);

            ctx1.syncTransactionListTask.run();
            ctx2.syncTransactionListTask.run();

            if (forkState1.isPassed(mockTimeProvider.get())) {
                Assert.assertNotNull(ctx1.backlogExplorerService.get(tx1.getID()));
                Assert.assertNull(ctx1.backlogExplorerService.get(tx2.getID()));
            } else {
                Assert.assertNotNull(ctx1.backlogExplorerService.get(tx1.getID()));
                Assert.assertNotNull(ctx1.backlogExplorerService.get(tx2.getID()));
            }

            if (forkState2.isPassed(mockTimeProvider.get())) {
                Assert.assertNull(ctx2.backlogExplorerService.get(tx1.getID()));
                Assert.assertNotNull(ctx2.backlogExplorerService.get(tx2.getID()));
            } else {
                Assert.assertNotNull(ctx2.backlogExplorerService.get(tx1.getID()));
                Assert.assertNotNull(ctx2.backlogExplorerService.get(tx2.getID()));
            }

            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);

            ctx2.generateBlockForNow();
            ctx1.fullBlockSync();

            lastBlock = ctx2.blockExplorerService.getLastBlock();
        } while (!lastBlockID.equals(lastBlock.getID()));

        Assert.assertEquals("Blockchain max possible height",
                            END / Constant.BLOCK_PERIOD,
                            ctx1.blockExplorerService.getLastBlock().getHeight());
        Assert.assertEquals("Blockchain max possible height",
                            END2 / Constant.BLOCK_PERIOD,
                            ctx2.blockExplorerService.getLastBlock().getHeight());
    }

    @Ignore
    @Test
    public void step_5_CheckPeerConnected() throws Exception {

        Block lastBlock = ctx2.blockExplorerService.getLastBlock();

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        for (int i = 0; i <= END2 + Constant.BLOCK_PERIOD; i++) {

            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + i);

            ctx2.generateBlockForNow();
            ctx1.syncBlockListTask.run();
            Block newBlock = ctx2.blockExplorerService.getLastBlock();

            Peer peer1 = ctx1.context.getAnyConnectedPeer();
            Peer peer2 = ctx2.context.getAnyConnectedPeer();

            if (peer1 != null) {
                ctx1.context.disablePeer(peer1);
            }
            if (peer2 != null) {
                ctx2.context.disablePeer(peer2);
            }
            Assert.assertNull(ctx1.context.getAnyConnectedPeer());
            Assert.assertNull(ctx2.context.getAnyConnectedPeer());
            ctx1.peerConnectTask.run();
            ctx2.peerConnectTask.run();

            int fork1 = forkState1.getNumber(newBlock.getTimestamp());
            int fork2 = forkState2.getNumber(newBlock.getTimestamp());
            int fork1n = forkState1.getNumber(mockTimeProvider.get());

            if (fork1n == 1) {
                Assert.assertNull(ctx1.context.getAnyConnectedPeer());
                Assert.assertNull(ctx2.context.getAnyConnectedPeer());
            } else if (fork1 == 1) {
                Assert.assertNotNull(ctx1.context.getAnyConnectedPeer());
                Assert.assertNull(ctx2.context.getAnyConnectedPeer());
            } else if (fork2 == 2) {
                Assert.assertNotNull(ctx1.context.getAnyConnectedPeer());
                Assert.assertNotNull(ctx2.context.getAnyConnectedPeer());
            } else if (fork2 == 3) {
                Assert.assertNull(ctx1.context.getAnyConnectedPeer());
                Assert.assertNull(ctx2.context.getAnyConnectedPeer());
            } else {
                throw new Exception("Impossible");
            }
        }

        Assert.assertEquals("Blockchain max possible height",
                            END / Constant.BLOCK_PERIOD,
                            ctx1.blockExplorerService.getLastBlock().getHeight());
        Assert.assertEquals("Blockchain max possible height",
                            END2 / Constant.BLOCK_PERIOD,
                            ctx2.blockExplorerService.getLastBlock().getHeight());
    }

    @Ignore
    @Test
    public void step_6_tran_version_checked() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        int time = lastBlock.getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        Transaction tx1 = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());
        Transaction tx2 = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());

        try {
            ctx1.transactionBotService.putTransaction(tx1);
        } catch (Exception ignored) {
        }
        try {
            ctx1.transactionBotService.putTransaction(tx2);
        } catch (Exception ignored) {
        }

        Assert.assertNotNull(ctx1.backlogExplorerService.get(tx1.getID()));
        Assert.assertNull(ctx1.backlogExplorerService.get(tx2.getID()));

        time = lastBlock.getTimestamp() + BEGIN + Constant.BLOCK_PERIOD + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx1.generateBlockForNow();

        Transaction tx1p = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                         .validity(mockTimeProvider.get(), 3600)
                                         .build(ctx1.getNetworkID(), ctx1.getSigner());
        Transaction tx2p = PaymentBuilder.createNew(100L, new AccountID(ctx2.getSigner().getPublicKey()))
                                         .validity(mockTimeProvider.get(), 3600)
                                         .build(ctx1.getNetworkID(), ctx1.getSigner());

        try {
            ctx1.transactionBotService.putTransaction(tx1p);
        } catch (Exception ignored) {
        }
        try {
            ctx1.transactionBotService.putTransaction(tx2p);
        } catch (Exception ignored) {
        }

        Assert.assertNotNull(ctx1.backlogExplorerService.get(tx1p.getID()));
        Assert.assertNotNull(ctx1.backlogExplorerService.get(tx2p.getID()));
    }
}
