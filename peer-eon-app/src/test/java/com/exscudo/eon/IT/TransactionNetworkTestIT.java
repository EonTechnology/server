package com.exscudo.eon.IT;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.transactions.Payment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionNetworkTestIT {

	private static String GENERATOR = "55373380ff77987646b816450824310fb377c1a14b6f725b94382af3cf7b788a";
	private static String GENERATOR2 = "dd6403d520afbfadeeff0b1bb49952440b767663454ab1e5f1a358e018cf9c73";
	private TimeProvider mockTimeProvider;

	private PeerContext ctx1;
	private PeerContext ctx2;

	private long genesisBlockID;
	private Fork fork;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);
		ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		genesisBlockID = ForkProvider.getInstance().getGenesisBlockID();
		fork = Mockito.spy(ForkProvider.getInstance());
		Mockito.when(ctx1.context.getCurrentFork()).thenReturn(fork);
		Mockito.when(ctx2.context.getCurrentFork()).thenReturn(fork);

		ForkProvider.init(fork);
	}

	@Test
	public void step_1_input() throws Exception {

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		Mockito.when(fork.getGenesisBlockID()).thenReturn(1L);

		Transaction tx = Payment.newPayment(100L, Format.MathID.pick(ctx2.getSigner().getPublicKey())).forFee(1L)
				.validity(lastBlock.getTimestamp(), 3600).build(ctx1.getSigner());

		Mockito.when(fork.getGenesisBlockID()).thenReturn(genesisBlockID);

		try {
			ctx1.transactionBotService.putTransaction(tx);
			throw new Error("Exception must throw in put");
		} catch (Exception ignored) {
		}

		Assert.assertFalse("Transaction not accepted",
				ctx1.context.getInstance().getBacklogService().contains(tx.getID()));
	}

	@Test
	public void step_2_syncTran() throws Exception {

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		Mockito.when(fork.getGenesisBlockID()).thenReturn(1L);

		Transaction tx = Payment.newPayment(100L, Format.MathID.pick(ctx2.getSigner().getPublicKey())).forFee(1L)
				.validity(lastBlock.getTimestamp(), 3600).build(ctx1.getSigner());

		ctx1.transactionBotService.putTransaction(tx);

		Mockito.when(fork.getGenesisBlockID()).thenReturn(genesisBlockID);

		ctx2.setPeerToConnect(ctx1);
		ctx2.syncTransactionListTask.run();

		Assert.assertTrue("Transaction accepted in (1)",
				ctx1.context.getInstance().getBacklogService().contains(tx.getID()));
		Assert.assertFalse("Transaction not accepted in (2)",
				ctx2.context.getInstance().getBacklogService().contains(tx.getID()));
	}

	@Test
	public void step_3_syncBlock() throws Exception {

		ctx2.setPeerToConnect(ctx1);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);
		Mockito.when(fork.getGenesisBlockID()).thenReturn(1L);

		ctx1.generateBlockForNow();

		Mockito.when(fork.getGenesisBlockID()).thenReturn(genesisBlockID);

		ctx2.fullBlockSync();
		Assert.assertNotEquals("Normal block accepted in (2)", lastBlock.getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());

		Mockito.when(fork.getGenesisBlockID()).thenReturn(1L);

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		Transaction tx = Payment.newPayment(100L, Format.MathID.pick(ctx2.getSigner().getPublicKey())).forFee(1L)
				.validity(lastBlock.getTimestamp(), 3600).build(ctx1.getSigner());

		ctx1.transactionBotService.putTransaction(tx);
		ctx1.generateBlockForNow();

		Mockito.when(fork.getGenesisBlockID()).thenReturn(genesisBlockID);

		ctx2.fullBlockSync();

		Assert.assertNotEquals("New block generated in (1)", lastBlock.getID(),
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID());
		Assert.assertTrue("Transaction accepted in (1)",
				ctx1.context.getInstance().getBlockchainService().transactionMapper().containsTransaction(tx.getID()));
		Assert.assertNotEquals("Blockchain not synchronized (2)",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

}
