package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.tx.builders.DepositBuilder;
import com.exscudo.peer.eon.tx.builders.PaymentBuilder;
import com.exscudo.peer.eon.tx.builders.RegistrationBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GenerationTestIT {

    private static final String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private static final String GENERATOR_NEW = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private TimeProvider mockTimeProvider;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);
    }

    @Test
    public void step_1_generate_block_early() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 60);

        ctx.generateBlockForNow();

        Assert.assertEquals("Is new block not generated", lastBlock.getID(), ctx.blockchain.getLastBlock().getID());
    }

    @Test
    public void step_2_generate_block_not_started() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

        Assert.assertFalse("Generation should be not allowed till blockchain sync",
                           ctx.generator.isGenerationAllowed());
        ctx.generateBlockTask.run();

        Assert.assertEquals("Is new block not generated", lastBlock.getID(), ctx.blockchain.getLastBlock().getID());
    }

    @Test
    public void step_3_generate_block() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

        ctx.generateBlockForNow();

        Assert.assertNotEquals("Is new block generated", lastBlock.getID(), ctx.blockchain.getLastBlock().getID());
        Assert.assertFalse("Generator stopped", ctx.generator.isGenerationAllowed());

        Block generatedBlock = ctx.blockchain.getBlock(ctx.blockchain.getLastBlock().getID());

        Assert.assertEquals("DB store new block ID", generatedBlock.getID(), ctx.blockchain.getLastBlock().getID());
        Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180, generatedBlock.getTimestamp());
    }

    @Test
    public void step_4_generate_2_block() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);

        ctx.generateBlockForNow();

        Assert.assertNotEquals("Is new block generated", lastBlock.getID(), ctx.blockchain.getLastBlock().getID());
        Assert.assertFalse("Generator stopped", ctx.generator.isGenerationAllowed());

        Block generatedBlock = ctx.blockchain.getBlock(ctx.blockchain.getLastBlock().getID());

        Assert.assertEquals("DB store new block ID", generatedBlock.getID(), ctx.blockchain.getLastBlock().getID());
        Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180 * 2, generatedBlock.getTimestamp());
    }

    @Test
    public void step_5_SyncBlockService() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);
        ctx.generateBlockForNow();

        Difficulty difficulty = ctx.syncBlockPeerService.getDifficulty();
        Block[] lastBlocks = ctx.syncBlockPeerService.getBlockHistory(new String[] {lastBlock.getID().toString()});

        Assert.assertEquals(ctx.blockchain.getLastBlock().getCumulativeDifficulty(), difficulty.getDifficulty());
        Assert.assertEquals(ctx.blockchain.getLastBlock().getID(), difficulty.getLastBlockID());
        Assert.assertEquals(ctx.blockchain.getLastBlock().getID(), lastBlocks[1].getID());
    }

    @Test
    public void step_6_CallSyncBlock() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);
        ctx.generateBlockForNow();

        // Reset DB
        PeerContext ctx2 = new PeerContext(GENERATOR, mockTimeProvider);

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);

        ctx2.setPeerToConnect(ctx);

        ctx2.syncBlockListTask.run();

        Assert.assertNotEquals("Is new block generated", lastBlock.getID(), ctx2.blockchain.getLastBlock().getID());
        Assert.assertFalse("Generator stopped", ctx2.generator.isGenerationAllowed());

        Block generatedBlock = ctx2.blockchain.getBlock(ctx2.blockchain.getLastBlock().getID());

        Assert.assertEquals("DB store new block ID", generatedBlock.getID(), ctx2.blockchain.getLastBlock().getID());
        Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180 * 2, generatedBlock.getTimestamp());
    }

    @Test
    public void step_7_generate_by_new_acc() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR_NEW, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

        ctx.generateBlockForNow();

        Assert.assertEquals("Is new block not generated", lastBlock.getID(), ctx.blockchain.getLastBlock().getID());
    }

    @Test
    public void step_8_generate_new_acc() throws Exception {

        PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
        PeerContext ctxNew = new PeerContext(GENERATOR_NEW, mockTimeProvider);
        Block lastBlock = ctx.blockchain.getLastBlock();

        Transaction tx = RegistrationBuilder.createNew(ctxNew.getSigner().getPublicKey())
                                            .validity(lastBlock.getTimestamp() + 100, 3600)
                                            .build(ctx.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx.transactionBotService.putTransaction(tx);
        ctx.generateBlockForNow();

        Assert.assertEquals("Registration in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        Transaction tx2 = PaymentBuilder.createNew(EonConstant.MIN_DEPOSIT_SIZE + 1000L,
                                                   new AccountID(ctxNew.getSigner().getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(ctx.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);
        ctx.transactionBotService.putTransaction(tx2);
        ctx.generateBlockForNow();

        Assert.assertEquals("Payment in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        Transaction tx3 = DepositBuilder.createNew(EonConstant.MIN_DEPOSIT_SIZE)
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(ctxNew.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);
        ctx.transactionBotService.putTransaction(tx3);
        ctx.generateBlockForNow();

        Assert.assertEquals("Deposit.refill in block", 1, ctx.blockchain.getLastBlock().getTransactions().size());

        int newTime = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * (Constant.BLOCK_IN_DAY + 1) + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(newTime);
        ctx.generateBlockForNow();

        ctxNew.setPeerToConnect(ctx);

        ctxNew.fullBlockSync();
        Assert.assertEquals("Block synchronized",
                            ctx.blockchain.getLastBlock().getID(),
                            ctxNew.blockchain.getLastBlock().getID());

        Block lastBlock2 = ctx.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 200);

        ctxNew.generateBlockForNow();

        Assert.assertEquals("Is new block not generated",
                            ctx.blockchain.getLastBlock().getID(),
                            ctxNew.blockchain.getLastBlock().getID());

        ctx.generateBlockForNow();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 400);

        ctxNew.syncBlockListTask.run();
        ctxNew.generateBlockForNow();

        Assert.assertNotEquals("Is new block generated",
                               ctx.blockchain.getLastBlock().getID(),
                               ctxNew.blockchain.getLastBlock().getID());

        ctx.setPeerToConnect(ctxNew);
        ctx.syncBlockListTask.run();

        Assert.assertEquals("Is new block synchronized",
                            ctx.blockchain.getLastBlock().getID(),
                            ctxNew.blockchain.getLastBlock().getID());

        Transaction tx4 = DepositBuilder.createNew(EonConstant.MIN_DEPOSIT_SIZE + 100L)
                                        .validity(lastBlock2.getTimestamp() + 200, 3600)
                                        .build(ctxNew.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 180 * 3 + 1);
        ctxNew.transactionBotService.putTransaction(tx4);

        ctxNew.generateBlockForNow();
        ctx.syncBlockListTask.run();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 180 * 4 + 1);
        ctxNew.generateBlockForNow();

        Assert.assertEquals("Is generated generation stopped",
                            ctx.blockchain.getLastBlock().getID(),
                            ctxNew.blockchain.getLastBlock().getID());

        Utils.comparePeer(ctx, ctxNew);
    }

    @Test
    public void step_9_GeneratorReplaceBlock() throws Exception {
        PeerContext ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
        PeerContext ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

        Block lastBlock = ctx1.blockchain.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Assert.assertNotEquals("Blockchain different",
                               ctx1.blockchain.getLastBlock().getID(),
                               ctx2.blockchain.getLastBlock().getID());

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        ctx1.syncBlockListTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        // ctx3 - double of ctx1 state
        // ctx4 - double of ctx2 state
        PeerContext ctx3 = new PeerContext(GENERATOR, mockTimeProvider);
        PeerContext ctx4 = new PeerContext(GENERATOR2, mockTimeProvider);
        ctx3.generateBlockForNow();
        ctx4.generateBlockForNow();

        ctx3.setPeerToConnect(ctx4);
        ctx4.setPeerToConnect(ctx3);

        ctx3.syncBlockListTask.run();
        ctx4.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx3.blockchain.getLastBlock().getID(),
                            ctx4.blockchain.getLastBlock().getID());
        Assert.assertEquals("Blockchain equals",
                            ctx2.blockchain.getLastBlock().getID(),
                            ctx3.blockchain.getLastBlock().getID());

        // Generate new block with rollback previous
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);

        ctx1.generateBlockForNow();
        ctx2.syncBlockListTask.run();
        ctx2.generateBlockForNow();

        ctx4.generateBlockForNow();
        ctx3.syncBlockListTask.run();
        ctx3.generateBlockForNow();

        Assert.assertEquals("Blockchain different",
                            lastBlock.getTimestamp() + 180 * 3,
                            ctx2.blockchain.getLastBlock().getTimestamp());
        Assert.assertEquals("Blockchain different",
                            lastBlock.getTimestamp() + 180 * 3,
                            ctx3.blockchain.getLastBlock().getTimestamp());
        Assert.assertEquals("Blockchain different",
                            ctx2.blockchain.getLastBlock().getCumulativeDifficulty(),
                            ctx3.blockchain.getLastBlock().getCumulativeDifficulty());
        Assert.assertEquals("Blockchain different",
                            ctx2.blockchain.getLastBlock().getID(),
                            ctx3.blockchain.getLastBlock().getID());

        Utils.comparePeer(ctx2, ctx3);
    }

    @Test
    public void step10_generator_should_use_trs_of_parallel_block() throws Exception {
        PeerContext ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
        PeerContext ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

        // ctx1 generates block 1
        int netStart = ctx1.blockchain.getLastBlock().getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(netStart + 180 * 1 + 1);
        ctx1.generateBlockForNow();

        ctx2.setPeerToConnect(ctx1);
        ctx2.syncBlockListTask.run();
        Assert.assertEquals("Blockchains should be equal",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        // put tx1 to ctx2
        AccountID ctx_signer_id = new AccountID(ctx2.getSigner().getPublicKey());
        Transaction tx1 = PaymentBuilder.createNew(10000L, ctx_signer_id)
                                        .validity(netStart + 180 * 1 + 2, 3600)
                                        .build(ctx1.getSigner());
        ctx2.transactionBotService.putTransaction(tx1);

        // ctx2 generates block 2 with tx1
        Mockito.when(mockTimeProvider.get()).thenReturn(netStart + 180 * 2 + 1);
        ctx2.generateBlockForNow();

        // ctx1 syncs with ctx2
        ctx1.setPeerToConnect(ctx2);
        ctx1.syncBlockListTask.run();
        BlockID lastBlockID = ctx1.blockchain.getLastBlock().getID();
        Assert.assertEquals("Blockchains should be equal",
                            ctx1.blockchain.getLastBlock().getID(),
                            ctx2.blockchain.getLastBlock().getID());

        // put tx2 to ctx1
        Transaction tx2 = PaymentBuilder.createNew(10000L, ctx_signer_id)
                                        .validity(netStart + 180 * 1 + 3, 3600)
                                        .build(ctx1.getSigner());
        ctx1.transactionBotService.putTransaction(tx2);

        // ctx1 generates 'parallel' block 2 with tx1 and tx2
        ctx1.generateBlockForNow();
        Block lastBlock = ctx1.blockchain.getLastBlock();
        Transaction[] txSet = lastBlock.getTransactions().toArray(new Transaction[0]);
        Assert.assertEquals(2, txSet.length);
        Assert.assertTrue("Last block should contain tx1",
                          tx1.getID().equals(txSet[0].getID()) || tx1.getID().equals(txSet[1].getID()));
        Assert.assertTrue("Last block should contain tx2",
                          tx2.getID().equals(txSet[0].getID()) || tx2.getID().equals(txSet[1].getID()));

        Assert.assertNull(ctx1.blockchain.getBlock(lastBlockID));
        Assert.assertNotNull(ctx1.storage.getBlockHelper().get(lastBlockID));
    }
}
