package org.eontechology.and.eon.app.IT;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.api.Difficulty;
import org.eontechology.and.peer.core.common.TimeProvider;
import org.eontechology.and.peer.core.crypto.ISigner;
import org.eontechology.and.peer.core.crypto.Signer;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.eon.midleware.parsers.DepositParser;
import org.eontechology.and.peer.eon.midleware.parsers.PaymentParser;
import org.eontechology.and.peer.eon.midleware.parsers.RegistrationParser;
import org.eontechology.and.peer.tx.TransactionType;
import org.eontechology.and.peer.tx.midleware.builders.DepositBuilder;
import org.eontechology.and.peer.tx.midleware.builders.PaymentBuilder;
import org.eontechology.and.peer.tx.midleware.builders.RegistrationBuilder;
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

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 60);

        ctx.generateBlockForNow();

        Assert.assertEquals("Is new block not generated",
                            lastBlock.getID(),
                            ctx.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_2_generate_block_not_started() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

        Assert.assertFalse("Generation should be not allowed till blockchain sync",
                           ctx.generator.isGenerationAllowed());
        ctx.generateBlockTask.run();

        Assert.assertEquals("Is new block not generated",
                            lastBlock.getID(),
                            ctx.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_3_generate_block() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

        ctx.generateBlockForNow();

        Block newLastBlock = ctx.blockExplorerService.getLastBlock();

        Assert.assertNotEquals("Is new block generated", lastBlock.getID(), newLastBlock.getID());
        Assert.assertFalse("Generator stopped", ctx.generator.isGenerationAllowed());

        Block generatedBlock = ctx.blockExplorerService.getById(newLastBlock.getID());

        Assert.assertEquals("DB store new block ID", generatedBlock.getID(), newLastBlock.getID());
        Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180, generatedBlock.getTimestamp());
    }

    @Test
    public void step_4_generate_2_block() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);

        ctx.generateBlockForNow();

        Block newLastBlock = ctx.blockExplorerService.getLastBlock();
        Assert.assertNotEquals("Is new block generated", lastBlock.getID(), newLastBlock.getID());
        Assert.assertFalse("Generator stopped", ctx.generator.isGenerationAllowed());

        Block generatedBlock = ctx.blockExplorerService.getById(newLastBlock.getID());

        Assert.assertEquals("DB store new block ID", generatedBlock.getID(), newLastBlock.getID());
        Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180 * 2, generatedBlock.getTimestamp());
    }

    @Test
    public void step_5_SyncBlockService() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);
        ctx.generateBlockForNow();

        Difficulty difficulty = ctx.syncBlockPeerService.getDifficulty();
        Block[] lastBlocks = ctx.syncBlockPeerService.getBlockHistory(new String[] {lastBlock.getID().toString()});

        lastBlock = ctx.blockExplorerService.getLastBlock();
        Assert.assertEquals(lastBlock.getCumulativeDifficulty(), difficulty.getDifficulty());
        Assert.assertEquals(lastBlock.getID(), difficulty.getLastBlockID());
        Assert.assertEquals(lastBlock.getID(), lastBlocks[1].getID());
    }

    @Test
    public void step_6_CallSyncBlock() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);
        ctx.generateBlockForNow();

        // Reset DB

        PeerContext ctx2 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);

        ctx2.setPeerToConnect(ctx);

        ctx2.syncBlockListTask.run();

        Block lastBlock2 = ctx2.blockExplorerService.getLastBlock();
        Assert.assertNotEquals("Is new block generated", lastBlock.getID(), lastBlock2.getID());
        Assert.assertFalse("Generator stopped", ctx2.generator.isGenerationAllowed());

        Block generatedBlock = ctx2.blockExplorerService.getById(lastBlock2.getID());

        Assert.assertEquals("DB store new block ID", generatedBlock.getID(), lastBlock2.getID());
        Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180 * 2, generatedBlock.getTimestamp());
    }

    @Test
    public void step_7_generate_by_new_acc() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_NEW).build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

        ctx.generateBlockForNow();

        Assert.assertEquals("Is new block not generated",
                            lastBlock.getID(),
                            ctx.blockExplorerService.getLastBlock().getID());
    }

    @Test
    public void step_8_generate_new_acc() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create()
                                                            .route(TransactionType.Payment, new PaymentParser())
                                                            .route(TransactionType.Registration,
                                                                   new RegistrationParser())
                                                            .route(TransactionType.Deposit, new DepositParser())
                                                            .seed(GENERATOR)
                                                            .build(mockTimeProvider));

        PeerContext ctxNew = new PeerContext(PeerStarterFactory.create()
                                                               .route(TransactionType.Payment, new PaymentParser())
                                                               .route(TransactionType.Registration,
                                                                      new RegistrationParser())
                                                               .route(TransactionType.Deposit, new DepositParser())
                                                               .seed(GENERATOR_NEW)
                                                               .build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        Transaction tx = RegistrationBuilder.createNew(ctxNew.getSigner().getPublicKey())
                                            .validity(lastBlock.getTimestamp() + 100, 3600)
                                            .build(ctx.getNetworkID(), ctx.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx.transactionBotService.putTransaction(tx);
        ctx.generateBlockForNow();

        Assert.assertEquals("Registration in block",
                            1,
                            ctx.blockExplorerService.getLastBlock().getTransactions().size());

        Transaction tx2 = PaymentBuilder.createNew(Utils.MIN_DEPOSIT_SIZE + 1000L,
                                                   new AccountID(ctxNew.getSigner().getPublicKey()))
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);
        ctx.transactionBotService.putTransaction(tx2);
        ctx.generateBlockForNow();

        Assert.assertEquals("Payment in block", 1, ctx.blockExplorerService.getLastBlock().getTransactions().size());

        Transaction tx3 = DepositBuilder.createNew(Utils.MIN_DEPOSIT_SIZE)
                                        .validity(lastBlock.getTimestamp() + 200, 3600)
                                        .build(ctxNew.getNetworkID(), ctxNew.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);
        ctx.transactionBotService.putTransaction(tx3);
        ctx.generateBlockForNow();

        Assert.assertEquals("Deposit.refill in block",
                            1,
                            ctx.blockExplorerService.getLastBlock().getTransactions().size());

        int newTime = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * (Constant.BLOCK_IN_DAY + 1) + 1;
        Mockito.when(mockTimeProvider.get()).thenReturn(newTime);
        ctx.generateBlockForNow();

        ctxNew.setPeerToConnect(ctx);

        ctxNew.fullBlockSync();
        Assert.assertEquals("Block synchronized",
                            ctx.blockExplorerService.getLastBlock().getID(),
                            ctxNew.blockExplorerService.getLastBlock().getID());

        Block lastBlock2 = ctx.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 200);

        ctxNew.generateBlockForNow();

        Assert.assertEquals("Is new block not generated",
                            ctx.blockExplorerService.getLastBlock().getID(),
                            ctxNew.blockExplorerService.getLastBlock().getID());

        ctx.generateBlockForNow();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 400);

        ctxNew.syncBlockListTask.run();
        ctxNew.generateBlockForNow();

        Assert.assertNotEquals("Is new block generated",
                               ctx.blockExplorerService.getLastBlock().getID(),
                               ctxNew.blockExplorerService.getLastBlock().getID());

        ctx.setPeerToConnect(ctxNew);
        ctx.syncBlockListTask.run();

        Assert.assertEquals("Is new block synchronized",
                            ctx.blockExplorerService.getLastBlock().getID(),
                            ctxNew.blockExplorerService.getLastBlock().getID());

        Transaction tx4 = DepositBuilder.createNew(Utils.MIN_DEPOSIT_SIZE + 100L)
                                        .validity(lastBlock2.getTimestamp() + 200, 3600)
                                        .build(ctxNew.getNetworkID(), ctxNew.getSigner());
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 180 * 3 + 1);
        ctxNew.transactionBotService.putTransaction(tx4);

        ctxNew.generateBlockForNow();
        ctx.syncBlockListTask.run();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 180 * 4 + 1);
        ctxNew.generateBlockForNow();

        Assert.assertEquals("Is generated generation stopped",
                            ctx.blockExplorerService.getLastBlock().getID(),
                            ctxNew.blockExplorerService.getLastBlock().getID());

        Utils.comparePeer(ctx, ctxNew);
    }

    @Test
    public void step_9_GeneratorReplaceBlock() throws Exception {

        PeerContext ctx1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        PeerContext ctx2 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR2).build(mockTimeProvider));

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.generateBlockForNow();

        Assert.assertNotEquals("Blockchain different",
                               ctx1.blockExplorerService.getLastBlock().getID(),
                               ctx2.blockExplorerService.getLastBlock().getID());

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        ctx1.syncBlockListTask.run();
        ctx2.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());

        // ctx3 - double of ctx1 state
        // ctx4 - double of ctx2 state
        PeerContext ctx3 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider));

        PeerContext ctx4 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR2).build(mockTimeProvider));

        ctx3.generateBlockForNow();
        ctx4.generateBlockForNow();

        ctx3.setPeerToConnect(ctx4);
        ctx4.setPeerToConnect(ctx3);

        ctx3.syncBlockListTask.run();
        ctx4.syncBlockListTask.run();

        Assert.assertEquals("Blockchain synchronized",
                            ctx3.blockExplorerService.getLastBlock().getID(),
                            ctx4.blockExplorerService.getLastBlock().getID());
        Assert.assertEquals("Blockchain equals",
                            ctx2.blockExplorerService.getLastBlock().getID(),
                            ctx3.blockExplorerService.getLastBlock().getID());

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
                            ctx2.blockExplorerService.getLastBlock().getTimestamp());
        Assert.assertEquals("Blockchain different",
                            lastBlock.getTimestamp() + 180 * 3,
                            ctx3.blockExplorerService.getLastBlock().getTimestamp());
        Assert.assertEquals("Blockchain different",
                            ctx2.blockExplorerService.getLastBlock().getCumulativeDifficulty(),
                            ctx3.blockExplorerService.getLastBlock().getCumulativeDifficulty());
        Assert.assertEquals("Blockchain different",
                            ctx2.blockExplorerService.getLastBlock().getID(),
                            ctx3.blockExplorerService.getLastBlock().getID());

        Utils.comparePeer(ctx2, ctx3);
    }

    @Test
    public void step10_generator_should_use_trs_of_parallel_block() throws Exception {

        PeerContext ctx1 = new PeerContext(PeerStarterFactory.create()
                                                             .route(TransactionType.Payment, new PaymentParser())
                                                             .seed(GENERATOR2)
                                                             .build(mockTimeProvider));

        PeerContext ctx2 = new PeerContext(PeerStarterFactory.create()
                                                             .route(TransactionType.Payment, new PaymentParser())
                                                             .seed(GENERATOR)
                                                             .build(mockTimeProvider));

        // ctx1 generates block 1
        int netStart = ctx1.blockExplorerService.getLastBlock().getTimestamp();
        Mockito.when(mockTimeProvider.get()).thenReturn(netStart + Constant.BLOCK_PERIOD + 1);
        ctx1.generateBlockForNow();

        ctx2.setPeerToConnect(ctx1);
        ctx2.syncBlockListTask.run();
        Block lastGenBlock = ctx1.blockExplorerService.getLastBlock();
        Assert.assertEquals("Blockchains should be equal",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());

        // put tx1 to ctx2
        AccountID ctx_signer_id = new AccountID(ctx2.getSigner().getPublicKey());
        Transaction tx1 = PaymentBuilder.createNew(10000L, ctx_signer_id)
                                        .validity(netStart + Constant.BLOCK_PERIOD + 2, 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());
        ctx2.transactionBotService.putTransaction(tx1);

        // ctx2 generates block 2 with tx1
        Mockito.when(mockTimeProvider.get()).thenReturn(netStart + 180 * 2 + 1);
        ctx2.generateBlockForNow();

        // ctx1 syncs with ctx2
        ctx1.setPeerToConnect(ctx2);
        ctx1.syncBlockListTask.run();
        BlockID lastBlockID = ctx1.blockExplorerService.getLastBlock().getID();
        Assert.assertEquals("Blockchains should be equal",
                            lastBlockID,
                            ctx2.blockExplorerService.getLastBlock().getID());

        // put tx2 to ctx1
        Transaction tx2 = PaymentBuilder.createNew(10000L, ctx_signer_id)
                                        .validity(netStart + Constant.BLOCK_PERIOD + 3, 3600)
                                        .build(ctx1.getNetworkID(), ctx1.getSigner());
        ctx1.transactionBotService.putTransaction(tx2);

        // ctx1 generates 'parallel' block 2 with tx1 and tx2
        ctx1.generator.allowGenerate();
        Block lastGenBlock2 = ctx1.generator.createNextBlock(lastGenBlock);

        Transaction[] txSet = lastGenBlock2.getTransactions().toArray(new Transaction[0]);
        Assert.assertEquals(2, txSet.length);
        Assert.assertTrue("Last block should contain tx1",
                          tx1.getID().equals(txSet[0].getID()) || tx1.getID().equals(txSet[1].getID()));
        Assert.assertTrue("Last block should contain tx2",
                          tx2.getID().equals(txSet[0].getID()) || tx2.getID().equals(txSet[1].getID()));
    }

    @Test
    public void step_11_generate_with_conflicting_transaction_order() throws Exception {

        PeerContext ctx = new PeerContext(PeerStarterFactory.create()
                                                            .route(TransactionType.Payment, new PaymentParser())
                                                            .route(TransactionType.Registration,
                                                                   new RegistrationParser())
                                                            .seed(GENERATOR)
                                                            .build(mockTimeProvider));

        Block lastBlock = ctx.blockExplorerService.getLastBlock();

        ISigner newSigner = Signer.createNew(GENERATOR_NEW);

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp());

        Transaction tx = RegistrationBuilder.createNew(newSigner.getPublicKey())
                                            .validity(mockTimeProvider.get(), 3600)
                                            .build(ctx.getNetworkID(), ctx.getSigner());
        Transaction tx1 = PaymentBuilder.createNew(100L, new AccountID(newSigner.getPublicKey()))
                                        .validity(mockTimeProvider.get() + 1, 3600)
                                        .build(ctx.getNetworkID(), ctx.getSigner());

        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
        ctx.transactionBotService.putTransaction(tx);
        ctx.transactionBotService.putTransaction(tx1);

        ctx.generateBlockForNow();

        Block newLastBlock = ctx.blockExplorerService.getLastBlock();

        Assert.assertNotEquals("New block not created", lastBlock.getID(), newLastBlock.getID());
        Assert.assertEquals("Some transactions are not added to the block", newLastBlock.getTransactions().size(), 1);
    }
}
