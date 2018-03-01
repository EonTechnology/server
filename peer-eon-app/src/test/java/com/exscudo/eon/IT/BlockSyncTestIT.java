package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BlockSyncTestIT {

    private static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
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
    public void step_1_one_block() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(0)).getBlockHistory(ArgumentMatchers.any());

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());
    }

    @Test
    public void step_2_two_block() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getBlockHistory(ArgumentMatchers.any());

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());
    }

    @Test
    public void step_3_replace_generated() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Difficulty difficulty1 = ctx1.syncBlockPeerService.getDifficulty();
        Difficulty difficulty2 = ctx2.syncBlockPeerService.getDifficulty();

        ctx1.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(1)).getDifficulty();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(1)).getDifficulty();

        if (difficulty1.compareTo(difficulty2) > 0) {
            Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
            Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(0)).getBlockHistory(ArgumentMatchers.any());
        } else {
            Mockito.verify(ctx2.syncBlockPeerService, Mockito.times(1)).getLastBlock();
            Mockito.verify(ctx2.syncBlockPeerService, Mockito.times(0)).getBlockHistory(ArgumentMatchers.any());
        }
    }

    @Test
    public void step_4_replace_2_generated() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Difficulty difficulty1 = ctx1.syncBlockPeerService.getDifficulty();
        Difficulty difficulty2 = ctx2.syncBlockPeerService.getDifficulty();

        ctx1.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(1)).getDifficulty();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(1)).getDifficulty();

        if (difficulty1.compareTo(difficulty2) > 0) {
            Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
            Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getBlockHistory(ArgumentMatchers.any());
        } else {
            Mockito.verify(ctx2.syncBlockPeerService, Mockito.times(1)).getLastBlock();
            Mockito.verify(ctx2.syncBlockPeerService, Mockito.times(1)).getBlockHistory(ArgumentMatchers.any());
        }
    }

    @Test
    public void step_5_too_many_blocks() throws Exception {

        Block lastBlock = ctx1.blockchain.getLastBlock();

        int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * Constant.BLOCK_IN_DAY * 2 + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx1.generateBlockForNow();

        ctx2.syncBlockListTask.run();

        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(2)).getBlockHistory(ArgumentMatchers.any());
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(2)).getDifficulty();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());
    }

    @Test
    public void step_6_sync_with_secondary_blockchain() throws Exception {
        // Step 1 - generate new block
        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        ctx1.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        // Step 2 - create chain branching
        lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        PeerContext ctx1Chain = this.ctx1;
        PeerContext ctx2Chain = this.ctx2;

        ctx1Chain.generator.allowGenerate();
        ctx2Chain.generator.allowGenerate();
        Block nextBlock1 = ctx1Chain.generator.createNextBlock(lastBlock);
        Block nextBlock2 = ctx2Chain.generator.createNextBlock(lastBlock);

        // ctx2Chain must generate best block
        if (nextBlock1.getCumulativeDifficulty().compareTo(nextBlock2.getCumulativeDifficulty()) > 0) {
            PeerContext tmp = ctx1Chain;
            ctx1Chain = ctx2Chain;
            ctx2Chain = tmp;
        }

        ctx1Chain.generateBlockForNow();
        ctx2Chain.fullBlockSync();

        ctx2Chain.generateBlockForNow();

        Block lastBlock1 = ctx1Chain.blockchain.getLastBlock();
        Block lastBlock2 = ctx2Chain.blockchain.getLastBlock();

        Assert.assertTrue("ctx2Chain generated better block",
                          lastBlock1.getCumulativeDifficulty().compareTo(lastBlock2.getCumulativeDifficulty()) < 0);
        Assert.assertNotEquals("Blockchain different",
                               ctx1Chain.blockchain.getLastBlock().getID(),
                               ctx2Chain.blockchain.getLastBlock().getID());

        // Step 3 - secondary branch is better
        while (lastBlock1.getCumulativeDifficulty().compareTo(lastBlock2.getCumulativeDifficulty()) < 0) {
            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock1.getTimestamp() + 180 + 1);
            ctx1Chain.generateBlockForNow();
            lastBlock1 = ctx1Chain.blockchain.getLastBlock();
        }

        ctx2Chain.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1Chain.blockchain.getLastBlock().getID(),
                            ctx2Chain.blockchain.getLastBlock().getID());
    }
}
