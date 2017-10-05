package com.exscudo.eon.IT;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.transactions.Payment;

@SuppressWarnings("WeakerAccess")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ForkTest1IT {

	protected static String GENERATOR = "55373380ff77987646b816450824310fb377c1a14b6f725b94382af3cf7b788a";
	protected static String GENERATOR2 = "dd6403d520afbfadeeff0b1bb49952440b767663454ab1e5f1a358e018cf9c73";
	protected TimeProvider mockTimeProvider;
	protected Fork forkState1;
	protected Fork forkState2;

	protected static int BEGIN_H = 3;
	protected static int END_H = 6;
	protected static int END_H2 = 9;

	protected static int BEGIN = Constant.BLOCK_PERIOD * BEGIN_H;
	protected static int END = Constant.BLOCK_PERIOD * END_H;
	protected static int END2 = Constant.BLOCK_PERIOD * END_H2;
	protected static int FORK = 2;
	protected PeerContext ctx1;
	protected PeerContext ctx2;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);
		ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		long genesisBlockID = ForkProvider.getInstance().getGenesisBlockID();

		forkState1 = new Fork(genesisBlockID, "2017-10-01T00:00:00.00Z", "2017-10-03T12:00:00.00Z") {
			{
				BEGIN = (lastBlock.getTimestamp() + ForkTest1IT.BEGIN) * 1000L; // in ms
				END = (lastBlock.getTimestamp() + ForkTest1IT.END) * 1000L; // in ms
				FORK = ForkTest1IT.FORK;
			}
		};
		forkState2 = new Fork(genesisBlockID, "2017-10-01T00:00:00.00Z", "2017-10-03T12:00:00.00Z") {
			{
				BEGIN = (lastBlock.getTimestamp() + ForkTest1IT.END) * 1000L; // in ms
				END = (lastBlock.getTimestamp() + ForkTest1IT.END2) * 1000L; // in ms
				FORK = ForkTest1IT.FORK + 1;
			}
		};

		Mockito.when(ctx1.context.getCurrentFork()).thenReturn(forkState1);
		Mockito.when(ctx2.context.getCurrentFork()).thenReturn(forkState2);
	}

	@Test
	public void step_1_1_isCome() {
		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Assert.assertFalse("Before fork", forkState1.isCome(lastBlock.getTimestamp() + BEGIN - 50));
		Assert.assertTrue("On fork", forkState1.isCome(lastBlock.getTimestamp() + BEGIN + 50));
		Assert.assertTrue("On fork", forkState1.isCome(lastBlock.getTimestamp() + END - 50));
		Assert.assertTrue("After fork", forkState1.isCome(lastBlock.getTimestamp() + END + 50));
	}

	@Test
	public void step_1_2_isPassed() {
		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Assert.assertFalse("Before fork", forkState1.isPassed(lastBlock.getTimestamp() + BEGIN - 50));
		Assert.assertFalse("On fork", forkState1.isPassed(lastBlock.getTimestamp() + BEGIN + 50));
		Assert.assertFalse("On fork", forkState1.isPassed(lastBlock.getTimestamp() + END - 50));
		Assert.assertTrue("After fork", forkState1.isPassed(lastBlock.getTimestamp() + END + 50));
	}

	@Test
	public void step_1_3_getNumber() {
		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Assert.assertEquals("Before fork", 1, forkState1.getNumber(lastBlock.getTimestamp() + BEGIN - 50));
		Assert.assertEquals("On fork", 2, forkState1.getNumber(lastBlock.getTimestamp() + BEGIN + 50));
		Assert.assertEquals("On fork", 2, forkState1.getNumber(lastBlock.getTimestamp() + END - 50));
		Assert.assertEquals("After fork", 2, forkState1.getNumber(lastBlock.getTimestamp() + END + 50));
	}

	@Test
	public void step_2_1_CheckGeneratorIsDisabled() {

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();
		long lastBlockID = 0;

		do {
			lastBlockID = lastBlock.getID();

			Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
			ctx1.generateBlockForNow();

			lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

			if (lastBlock.getID() != lastBlockID) {
				Assert.assertFalse(forkState1.isPassed(lastBlock.getTimestamp()));
			} else {
				Assert.assertTrue(forkState1.isPassed(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD));
			}
		} while (lastBlockID != lastBlock.getID());

		Assert.assertEquals("Blockchain max possible height", END / Constant.BLOCK_PERIOD,
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getHeight());
	}

	@Test
	public void step_2_2_CheckGeneratorIsDisabled() {

		Block lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();
		long lastBlockID = 0;

		do {
			lastBlockID = lastBlock.getID();

			Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
			ctx2.generateBlockForNow();

			lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();

			if (lastBlock.getID() != lastBlockID) {
				Assert.assertFalse(forkState2.isPassed(lastBlock.getTimestamp()));
			} else {
				Assert.assertTrue(forkState2.isPassed(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD));
			}
		} while (lastBlockID != lastBlock.getID());

		Assert.assertEquals("Blockchain max possible height", END2 / Constant.BLOCK_PERIOD,
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getHeight());

	}

	@Test
	public void step_3_1_CheckBlockSyncIsDisabled() {

		ctx1.setPeerToConnect(ctx2);

		Block lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();
		long lastBlockID = 0;

		do {
			lastBlockID = lastBlock.getID();

			Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);
			ctx2.generateBlockForNow();
			ctx1.fullBlockSync();

			lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();

			if (ctx1.context.getInstance().getBlockchainService().getLastBlock().getID() == ctx2.context.getInstance()
					.getBlockchainService().getLastBlock().getID()) {
				Assert.assertFalse(forkState1.isPassed(lastBlock.getTimestamp()));
				Assert.assertFalse(forkState2.isCome(lastBlock.getTimestamp()));
			} else {
				Assert.assertTrue(forkState1.isPassed(lastBlock.getTimestamp()));
				Assert.assertTrue(forkState2.isCome(lastBlock.getTimestamp()));
			}
		} while (lastBlockID != lastBlock.getID());

		Assert.assertEquals("Blockchain max possible height", END / Constant.BLOCK_PERIOD,
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getHeight());
		Assert.assertEquals("Blockchain max possible height", END2 / Constant.BLOCK_PERIOD,
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getHeight());

	}

	@Test
	public void step_3_2_CheckBlockSyncIsDisabled() {

		ctx1.setPeerToConnect(ctx2);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();
		int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * END_H2 * 2 + 1;
		Mockito.when(mockTimeProvider.get()).thenReturn(time);

		ctx2.generateBlockForNow();
		ctx1.fullBlockSync();

		Assert.assertNotEquals("Blockchain not synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertEquals("Blockchain synchronized to possible height", END / Constant.BLOCK_PERIOD,
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getHeight());
		Assert.assertEquals("Blockchain max possible height", END2 / Constant.BLOCK_PERIOD,
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getHeight());
	}

	@Test
	public void step_4_CheckTranSyncIsDisabled() throws Exception {

		ctx1.setPeerToConnect(ctx2);
		ctx2.setPeerToConnect(ctx1);

		Block lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();
		long lastBlockID = 0;

		do {
			lastBlockID = lastBlock.getID();

			Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 1);

			Transaction tx1 = Payment.newPayment(100L).to(Format.MathID.pick(ctx2.getSigner().getPublicKey()))
					.forFee(1L).validity(lastBlock.getTimestamp(), (short) 60).build(ctx1.getSigner());
			Transaction tx2 = Payment.newPayment(100L).to(Format.MathID.pick(ctx2.getSigner().getPublicKey()))
					.forFee(1L).validity(lastBlock.getTimestamp() + 1, (short) 60).build(ctx1.getSigner());

			ctx1.transactionBotService.putTransaction(tx1);
			ctx2.transactionBotService.putTransaction(tx2);

			ctx1.syncTransactionListTask.run();
			ctx2.syncTransactionListTask.run();

			if (forkState1.isPassed(mockTimeProvider.get())) {
				Assert.assertTrue(ctx1.context.getInstance().getBacklogService().contains(tx1.getID()));
				Assert.assertFalse(ctx1.context.getInstance().getBacklogService().contains(tx2.getID()));
			} else {
				Assert.assertTrue(ctx1.context.getInstance().getBacklogService().contains(tx1.getID()));
				Assert.assertTrue(ctx1.context.getInstance().getBacklogService().contains(tx2.getID()));
			}

			if (forkState2.isPassed(mockTimeProvider.get())) {
				Assert.assertFalse(ctx2.context.getInstance().getBacklogService().contains(tx1.getID()));
				Assert.assertTrue(ctx2.context.getInstance().getBacklogService().contains(tx2.getID()));
			} else {
				Assert.assertTrue(ctx2.context.getInstance().getBacklogService().contains(tx1.getID()));
				Assert.assertTrue(ctx2.context.getInstance().getBacklogService().contains(tx2.getID()));
			}

			Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + Constant.BLOCK_PERIOD + 1);

			ctx2.generateBlockForNow();
			ctx1.fullBlockSync();

			lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();

		} while (lastBlockID != lastBlock.getID());

		Assert.assertEquals("Blockchain max possible height", END / Constant.BLOCK_PERIOD,
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getHeight());
		Assert.assertEquals("Blockchain max possible height", END2 / Constant.BLOCK_PERIOD,
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getHeight());

	}

	@Test
	public void step_5_CheckPeerConnected() throws Exception {

		Block lastBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();

		ctx1.setPeerToConnect(ctx2);
		ctx2.setPeerToConnect(ctx1);

		for (int i = 0; i <= END2 + Constant.BLOCK_PERIOD; i++) {

			Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + i);

			ctx2.generateBlockForNow();
			ctx1.syncBlockListTask.run();
			Block newBlock = ctx2.context.getInstance().getBlockchainService().getLastBlock();

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

		Assert.assertEquals("Blockchain max possible height", END / Constant.BLOCK_PERIOD,
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getHeight());
		Assert.assertEquals("Blockchain max possible height", END2 / Constant.BLOCK_PERIOD,
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getHeight());

	}

}
