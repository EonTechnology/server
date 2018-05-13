package com.exscudo.eon.app.IT;

import com.exscudo.TestSigner;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.tx.midleware.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
        ctx1 = new PeerContext(PeerStarterFactory.create(GENERATOR, mockTimeProvider));
        ctx2 = new PeerContext(PeerStarterFactory.create(GENERATOR2, mockTimeProvider));

        ctx1.syncBlockPeerService = Mockito.spy(ctx1.syncBlockPeerService);
        ctx2.syncBlockPeerService = Mockito.spy(ctx2.syncBlockPeerService);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);
    }

    @Test
    public void step_1_one_block() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(0)).getBlockHistory(ArgumentMatchers.any());

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_2_two_block() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();

        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getBlockHistory(ArgumentMatchers.any());

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_3_replace_generated() throws Exception {

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Difficulty difficulty1 = ctx1.syncBlockPeerService.getDifficulty();
        Difficulty difficulty2 = ctx2.syncBlockPeerService.getDifficulty();

        ctx1.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
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

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Difficulty difficulty1 = ctx1.syncBlockPeerService.getDifficulty();
        Difficulty difficulty2 = ctx2.syncBlockPeerService.getDifficulty();

        ctx1.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
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

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * Constant.BLOCK_IN_DAY * 2 + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(time);

        ctx1.generateBlockForNow();

        ctx2.syncBlockListTask.run();

        Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(2)).getBlockHistory(ArgumentMatchers.any());
        Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(2)).getDifficulty();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_6_sync_with_secondary_blockchain() throws Exception {
        // Step 1 - generate new block
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        ctx1.fullBlockSync();
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());

        // Step 2 - create chain branching
        lastBlock = ctx1.blockExplorerService.getLastBlock();

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

        Block lastBlock1 = ctx1Chain.blockExplorerService.getLastBlock();
        Block lastBlock2 = ctx2Chain.blockExplorerService.getLastBlock();

        Assert.assertTrue("ctx2Chain generated better block",
                          lastBlock1.getCumulativeDifficulty().compareTo(lastBlock2.getCumulativeDifficulty()) < 0);
        Assert.assertNotEquals("Blockchain different", lastBlock1.getID(), lastBlock2.getID());

        // Step 3 - secondary branch is better
        while (lastBlock1.getCumulativeDifficulty().compareTo(lastBlock2.getCumulativeDifficulty()) < 0) {
            Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock1.getTimestamp() + 180 + 1);
            ctx1Chain.generateBlockForNow();
            lastBlock1 = ctx1Chain.blockExplorerService.getLastBlock();
        }

        ctx2Chain.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1Chain.blockExplorerService.getLastBlock().getID(),
                            ctx2Chain.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_7_set_pointer_to_primary_blockchain() throws Exception {

        byte[] publicKey = new TestSigner(GENERATOR2).getPublicKey();
        Block targetBlock = null;

        // generate chain
        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);

        ctx1.generateBlockForNow();

        Transaction tx1 = PaymentBuilder.createNew(100L, new AccountID(publicKey))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());
        ctx1.transactionBotService.putTransaction(tx1);

        lastBlock = ctx1.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        targetBlock = lastBlock;
        ctx1.generateBlockForNow();

        Transaction tx2 = PaymentBuilder.createNew(100L, new AccountID(publicKey))
                                        .validity(mockTimeProvider.get(), 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());
        ctx1.transactionBotService.putTransaction(tx2);

        lastBlock = ctx1.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
        final Block invalidBlock = lastBlock;
        ctx1.generateBlockForNow();

        PeerStarter ps = PeerStarterFactory.create(GENERATOR2, mockTimeProvider);
        LedgerProvider ledgerProvider = ps.getLedgerProvider();
        LedgerProvider mockLedgerProvider = Mockito.mock(LedgerProvider.class);
        Mockito.when(mockLedgerProvider.getLedger(ArgumentMatchers.any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return ledgerProvider.getLedger(invocationOnMock.getArgument(0));
            }
        });
        Mockito.when(mockLedgerProvider.addLedger(ArgumentMatchers.any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                ILedger arg0 = invocationOnMock.getArgument(0);

                if (arg0.getHash().equals(invalidBlock.getSnapshot())) {
                    throw new DataAccessException();
                }
                return ledgerProvider.addLedger(arg0);
            }
        });
        ps.setLedgerProvider(mockLedgerProvider);
        PeerContext ctx2 = new PeerContext(ps);

        ctx2.setPeerToConnect(ctx1);
        ctx2.fullBlockSync();

        Assert.assertEquals("Blockchain synchronized",
                            targetBlock.getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());
    }
}
