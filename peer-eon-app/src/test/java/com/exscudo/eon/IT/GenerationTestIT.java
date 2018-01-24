package com.exscudo.eon.IT;

import java.util.Arrays;

import com.exscudo.peer.eon.transactions.builders.AccountRegistrationBuilder;
import com.exscudo.peer.eon.transactions.builders.DepositRefillBuilder;
import com.exscudo.peer.eon.transactions.builders.PaymentBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.dampcake.bencode.Bencode;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TimeProvider;

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
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 60);

		ctx.generateBlockForNow();

		Assert.assertEquals("Is new block not generated", lastBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

	@Test
	public void step_2_generate_block_not_started() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

		Assert.assertFalse("Generation should be not allowed till blockchain sync",
				ctx.context.getInstance().getGenerator().isGenerationAllowed());
		ctx.generateBlockTask.run();

		Assert.assertEquals("Is new block not generated", lastBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

	@Test
	public void step_3_generate_block() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

		ctx.generateBlockForNow();

		Assert.assertNotEquals("Is new block generated", lastBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertFalse("Generator stopped", ctx.context.getInstance().getGenerator().isGenerationAllowed());

		Block generatedBlock = ctx.context.getInstance().getBlockchainService()
				.getBlock(ctx.context.getInstance().getBlockchainService().getLastBlock().getID());

		Assert.assertEquals("DB store new block ID", generatedBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180, generatedBlock.getTimestamp());
	}

	@Test
	public void step_4_generate_2_block() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);

		ctx.generateBlockForNow();

		Assert.assertNotEquals("Is new block generated", lastBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertFalse("Generator stopped", ctx.context.getInstance().getGenerator().isGenerationAllowed());

		Block generatedBlock = ctx.context.getInstance().getBlockchainService()
				.getBlock(ctx.context.getInstance().getBlockchainService().getLastBlock().getID());

		Assert.assertEquals("DB store new block ID", generatedBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180 * 2, generatedBlock.getTimestamp());
	}

	@Test
	public void step_5_SyncBlockService() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);
		ctx.generateBlockForNow();

		Difficulty difficulty = ctx.syncBlockPeerService.getDifficulty();
		Block[] lastBlocks = ctx.syncBlockPeerService
				.getBlockHistory(new String[] { Format.ID.blockId(lastBlock.getID()) });

		Assert.assertEquals(ctx.context.getInstance().getBlockchainService().getLastBlock().getCumulativeDifficulty(),
				difficulty.getDifficulty());
		Assert.assertEquals(ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				difficulty.getLastBlockID());
		Assert.assertEquals(ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				lastBlocks[1].getID());
	}

	@Test
	public void step_6_CallSyncBlock() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);
		ctx.generateBlockForNow();

		// Reset DB
		PeerContext ctx2 = new PeerContext(GENERATOR, mockTimeProvider);

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 400);

		ctx2.setPeerToConnect(ctx);

		ctx2.syncBlockListTask.run();

		Assert.assertNotEquals("Is new block generated", lastBlock.getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertFalse("Generator stopped", ctx2.context.getInstance().getGenerator().isGenerationAllowed());

		Block generatedBlock = ctx2.context.getInstance().getBlockchainService()
				.getBlock(ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		Assert.assertEquals("DB store new block ID", generatedBlock.getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertEquals("Period is 180 sec", lastBlock.getTimestamp() + 180 * 2, generatedBlock.getTimestamp());
	}

	@Test
	public void step_7_generate_by_new_acc() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR_NEW, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 200);

		ctx.generateBlockForNow();

		Assert.assertEquals("Is new block not generated", lastBlock.getID(),
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

	@Test
	public void step_8_generate_new_acc() throws Exception {

		PeerContext ctx = new PeerContext(GENERATOR, mockTimeProvider);
		PeerContext ctxNew = new PeerContext(GENERATOR_NEW, mockTimeProvider);
		Block lastBlock = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Transaction tx = AccountRegistrationBuilder.createNew(ctxNew.getSigner().getPublicKey())
				.validity(lastBlock.getTimestamp() + 100, 3600).forFee(1L).build(ctx.getSigner());
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		ctx.transactionBotService.putTransaction(tx);
		ctx.generateBlockForNow();

		Assert.assertEquals("Registration in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		Transaction tx2 = PaymentBuilder
				.createNew(EonConstant.MIN_DEPOSIT_SIZE + 1000L, Format.MathID.pick(ctxNew.getSigner().getPublicKey()))
				.forFee(1L).validity(lastBlock.getTimestamp() + 200, 3600).build(ctx.getSigner());
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);
		ctx.transactionBotService.putTransaction(tx2);
		ctx.generateBlockForNow();

		Assert.assertEquals("Payment in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		Transaction tx3 = DepositRefillBuilder.createNew(EonConstant.MIN_DEPOSIT_SIZE)
				.validity(lastBlock.getTimestamp() + 200, 3600).build(ctxNew.getSigner());
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);
		ctx.transactionBotService.putTransaction(tx3);
		ctx.generateBlockForNow();

		Assert.assertEquals("Deposit.refill in block", 1,
				ctx.context.getInstance().getBlockchainService().getLastBlock().getTransactions().size());

		int newTime = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * (Constant.BLOCK_IN_DAY + 1) + 1;
		Mockito.when(mockTimeProvider.get()).thenReturn(newTime);
		ctx.generateBlockForNow();

		ctxNew.setPeerToConnect(ctx);

		ctxNew.fullBlockSync();
		Assert.assertEquals("Block synchronized",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctxNew.context.getInstance().getBlockchainService().getLastBlock().getID());

		Block lastBlock2 = ctx.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 200);

		ctxNew.generateBlockForNow();

		Assert.assertEquals("Is new block not generated",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctxNew.context.getInstance().getBlockchainService().getLastBlock().getID());

		ctx.generateBlockForNow();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 400);

		ctxNew.syncBlockListTask.run();
		ctxNew.generateBlockForNow();

		Assert.assertNotEquals("Is new block generated",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctxNew.context.getInstance().getBlockchainService().getLastBlock().getID());

		ctx.setPeerToConnect(ctxNew);
		ctx.syncBlockListTask.run();

		Assert.assertEquals("Is new block synchronized",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctxNew.context.getInstance().getBlockchainService().getLastBlock().getID());

		Transaction tx4 = DepositRefillBuilder.createNew(100L).validity(lastBlock2.getTimestamp() + 200, 3600)
				.build(ctxNew.getSigner());
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 180 * 3 + 1);
		ctxNew.transactionBotService.putTransaction(tx4);

		ctxNew.generateBlockForNow();
		ctx.syncBlockListTask.run();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock2.getTimestamp() + 180 * 4 + 1);
		ctxNew.generateBlockForNow();

		Assert.assertEquals("Is generated generation stopped",
				ctx.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctxNew.context.getInstance().getBlockchainService().getLastBlock().getID());

		comparePeer(ctx, ctxNew);

	}

	@Test
	public void step_9_GeneratorReplaceBlock() throws Exception {
		PeerContext ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		PeerContext ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		ctx1.generateBlockForNow();
		ctx2.generateBlockForNow();

		Assert.assertNotEquals("Blockchain different",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		ctx1.setPeerToConnect(ctx2);
		ctx2.setPeerToConnect(ctx1);

		ctx1.syncBlockListTask.run();
		ctx2.syncBlockListTask.run();

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

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
				ctx3.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx4.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertEquals("Blockchain equals",
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx3.context.getInstance().getBlockchainService().getLastBlock().getID());

		// Generate new block with rollback previous
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 3 + 1);

		ctx1.generateBlockForNow();
		ctx2.syncBlockListTask.run();
		ctx2.generateBlockForNow();

		ctx4.generateBlockForNow();
		ctx3.syncBlockListTask.run();
		ctx3.generateBlockForNow();

		Assert.assertEquals("Blockchain different", lastBlock.getTimestamp() + 180 * 3,
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getTimestamp());
		Assert.assertEquals("Blockchain different", lastBlock.getTimestamp() + 180 * 3,
				ctx3.context.getInstance().getBlockchainService().getLastBlock().getTimestamp());
		Assert.assertEquals("Blockchain different",
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getCumulativeDifficulty(),
				ctx3.context.getInstance().getBlockchainService().getLastBlock().getCumulativeDifficulty());
		Assert.assertEquals("Blockchain different",
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx3.context.getInstance().getBlockchainService().getLastBlock().getID());

		comparePeer(ctx2, ctx3);
	}

	@Test
	public void step10_generator_should_use_trs_of_parallel_block() throws Exception {
		PeerContext ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		PeerContext ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		// ctx1 generates block 1
		int netStart = ctx1.context.getInstance().getBlockchainService().getLastBlock().getTimestamp();
		Mockito.when(mockTimeProvider.get()).thenReturn(netStart + 180 * 1 + 1);
		ctx1.generateBlockForNow();

		ctx2.setPeerToConnect(ctx1);
		ctx2.syncBlockListTask.run();
		Assert.assertEquals("Blockchains should be equal",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		// put tx1 to ctx2
		Transaction tx1 = PaymentBuilder.createNew(10000L, Format.MathID.pick(ctx2.getSigner().getPublicKey()))
				.forFee(1L).validity(netStart + 180 * 1 + 2, 3600).build(ctx1.getSigner());
		ctx2.transactionBotService.putTransaction(tx1);

		// ctx2 generates block 2 with tx1
		Mockito.when(mockTimeProvider.get()).thenReturn(netStart + 180 * 2 + 1);
		ctx2.generateBlockForNow();

		// ctx1 syncs with ctx2
		ctx1.setPeerToConnect(ctx2);
		ctx1.syncBlockListTask.run();
		Assert.assertEquals("Blockchains should be equal",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		// put tx2 to ctx1
		Transaction tx2 = PaymentBuilder.createNew(10000L, Format.MathID.pick(ctx2.getSigner().getPublicKey()))
				.forFee(1L).validity(netStart + 180 * 1 + 3, 3600).build(ctx1.getSigner());
		ctx1.transactionBotService.putTransaction(tx2);

		// ctx1 generates 'parallel' block 2 with tx1 and tx2
		ctx1.generateBlockForNow();
		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();
		Transaction[] txSet = lastBlock.getTransactions().toArray(new Transaction[0]);
		Assert.assertEquals(2, txSet.length);
		Assert.assertTrue("Last block should contain tx1",
				tx1.getID() == txSet[0].getID() || tx1.getID() == txSet[1].getID());
		Assert.assertTrue("Last block should contain tx2",
				tx2.getID() == txSet[0].getID() || tx2.getID() == txSet[1].getID());

		// check block reference and height correctly set in db
		Transaction dbTx1 = ctx1.context.getInstance().getBlockchainService().transactionMapper()
				.getTransaction(tx1.getID());
		Transaction dbTx2 = ctx1.context.getInstance().getBlockchainService().transactionMapper()
				.getTransaction(tx2.getID());
		Assert.assertNotNull(dbTx1);
		Assert.assertNotNull(dbTx2);
		Assert.assertEquals(lastBlock.getID(), dbTx1.getBlock());
		Assert.assertEquals(lastBlock.getID(), dbTx2.getBlock());
		Assert.assertEquals(2, dbTx1.getHeight());
		Assert.assertEquals(2, dbTx2.getHeight());
	}

	@Test
	public void step11_UpdateBlockIdInTransaction() throws Exception {

		PeerContext ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		PeerContext ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		ctx1.setPeerToConnect(ctx2);
		ctx2.setPeerToConnect(ctx1);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

		Transaction tx = PaymentBuilder
				.createNew(1000L,
						Format.MathID.pick(ctx1.context.getInstance().getGenerator().getSigner().getPublicKey()))
				.forFee(1L).validity(mockTimeProvider.get() - 1, 3600)
				.build(ctx2.context.getInstance().getGenerator().getSigner());

		ctx1.transactionBotService.putTransaction(tx);
		ctx2.transactionBotService.putTransaction(tx);

		ctx1.generateBlockForNow();
		ctx2.generateBlockForNow();

		ctx1.fullBlockSync();
		ctx2.fullBlockSync();

		Block block1 = ctx1.context.getInstance().getBlockchainService().getLastBlock();
		Block block2 = ctx2.context.getInstance().getBlockchainService().getLastBlock();

		Assert.assertEquals(block1.getID(), block2.getID());
		Assert.assertEquals(1, block1.getTransactions().size());
		Assert.assertEquals(1, block2.getTransactions().size());

		Transaction txDB1 = ctx1.context.getInstance().getBlockchainService().transactionMapper()
				.getTransaction(tx.getID());
		Transaction txDB2 = ctx2.context.getInstance().getBlockchainService().transactionMapper()
				.getTransaction(tx.getID());

		Assert.assertEquals(txDB1.getBlock(), block1.getID());
		Assert.assertEquals(txDB2.getBlock(), block2.getID());
	}

	private void comparePeer(PeerContext ctx1, PeerContext ctx2) {

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		long lastBlockID = ctx1.context.getInstance().getBlockchainService().getLastBlock().getID();
		Bencode bencode = new Bencode();

		while (lastBlockID != 0) {
			Block block = ctx1.context.getInstance().getBlockchainService().getBlock(lastBlockID);
			Block blockNew = ctx2.context.getInstance().getBlockchainService().getBlock(lastBlockID);

			Assert.assertEquals(block.getVersion(), blockNew.getVersion());
			Assert.assertEquals(block.getTimestamp(), blockNew.getTimestamp());
			Assert.assertEquals(block.getPreviousBlock(), blockNew.getPreviousBlock());
			Assert.assertEquals(Format.convert(block.getGenerationSignature()),
					Format.convert(blockNew.getGenerationSignature()));
			Assert.assertEquals(block.getSenderID(), blockNew.getSenderID());
			Assert.assertEquals(Format.convert(block.getSignature()), Format.convert(blockNew.getSignature()));
			Assert.assertEquals(block.getID(), blockNew.getID());
			Assert.assertEquals(block.getHeight(), blockNew.getHeight());
			Assert.assertEquals(block.getCumulativeDifficulty().toString(),
					blockNew.getCumulativeDifficulty().toString());
			Assert.assertEquals(Format.convert(block.getSnapshot()), Format.convert(blockNew.getSnapshot()));

			Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
			Transaction[] transactionsNew = blockNew.getTransactions().toArray(new Transaction[0]);

			Arrays.sort(transactions, new TransactionComparator());
			Arrays.sort(transactionsNew, new TransactionComparator());

			Assert.assertEquals(transactions.length, transactionsNew.length);

			for (int i = 0; i < transactions.length; i++) {
				Transaction transaction = transactions[i];
				Transaction transactionNew = transactionsNew[i];

				Assert.assertEquals(transaction.getType(), transactionNew.getType());
				Assert.assertEquals(transaction.getTimestamp(), transactionNew.getTimestamp());
				Assert.assertEquals(transaction.getDeadline(), transactionNew.getDeadline());
				Assert.assertEquals(transaction.getReference(), transactionNew.getReference());
				Assert.assertEquals(transaction.getSenderID(), transactionNew.getSenderID());
				Assert.assertEquals(transaction.getFee(), transactionNew.getFee());
				Assert.assertEquals(Format.convert(bencode.encode(transaction.getData())),
						Format.convert(bencode.encode(transactionNew.getData())));
				Assert.assertEquals(Format.convert(transaction.getSignature()),
						Format.convert(transactionNew.getSignature()));
				Assert.assertEquals(transaction.getBlock(), transactionNew.getBlock());
				Assert.assertEquals(transaction.getHeight(), transactionNew.getHeight());
				Assert.assertEquals(transaction.getID(), transactionNew.getID());
				Assert.assertEquals(transaction.getLength(), transactionNew.getLength());
			}

			lastBlockID = block.getPreviousBlock();
		}
	}

}
