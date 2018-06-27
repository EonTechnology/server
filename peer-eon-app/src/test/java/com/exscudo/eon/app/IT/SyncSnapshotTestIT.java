package com.exscudo.eon.app.IT;

import java.util.Random;

import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.tx.midleware.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SyncSnapshotTestIT {

    private static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private TimeProvider mockTimeProvider;

    private PeerContext ctx1;
    private PeerContext ctx2;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter peerStarter1 = PeerStarterFactory.create(GENERATOR, mockTimeProvider, true);
        ctx1 = new PeerContext(peerStarter1);

        PeerStarter peerStarter2 = PeerStarterFactory.create(GENERATOR2, mockTimeProvider, false);
        ctx2 = new PeerContext(peerStarter2);

        ctx1.syncBlockPeerService = Mockito.spy(ctx1.syncBlockPeerService);
        ctx2.syncBlockPeerService = Mockito.spy(ctx2.syncBlockPeerService);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);
    }

    @Test
    public void step_1_sync_snapshot() {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * Constant.BLOCK_IN_DAY * 2 + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx1.generateBlockForNow();

        ctx2.syncSnapshotTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_2_sync_short() {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * Constant.BLOCK_IN_DAY * 3 / 2 + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx1.generateBlockForNow();

        ctx2.syncSnapshotTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_3_sync_very_short() {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * Constant.BLOCK_IN_DAY / 2 + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx1.generateBlockForNow();

        ctx2.syncSnapshotTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_4_sync_not_empty_block() throws Exception {

        Random random = new Random();

        for (int i = 0; i < Constant.BLOCK_IN_DAY * 2; i++) {
            Block lastBlock = ctx1.blockExplorerService.getLastBlock();
            int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1;
            Mockito.when(mockTimeProvider.get()).thenReturn(time);

            byte[] pk = new byte[32];
            random.nextBytes(pk);
            Transaction tx = RegistrationBuilder.createNew(pk)
                                                .validity(lastBlock.getTimestamp() + 100, 3600)
                                                .build(ctx1.getNetworkID(), ctx1.getSigner());

            ctx1.transactionBotService.putTransaction(tx);

            ctx1.generateBlockForNow();

            lastBlock = ctx1.blockExplorerService.getLastBlock();
            Assert.assertEquals(1, lastBlock.getTransactions().size());
        }

        ctx2.syncSnapshotTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }
}
