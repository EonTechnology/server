package com.exscudo.eon.IT;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.eon.TimeProvider;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BlockSyncTestIT {

	private static String GENERATOR = "55373380ff77987646b816450824310fb377c1a14b6f725b94382af3cf7b788a";
	private static String GENERATOR2 = "dd6403d520afbfadeeff0b1bb49952440b767663454ab1e5f1a358e018cf9c73";
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

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

		ctx1.generateBlockForNow();
		ctx2.fullBlockSync();

		Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
		Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(0)).getBlockHistory(ArgumentMatchers.any());

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

	@Test
	public void step_2_two_block() throws Exception {

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		ctx1.generateBlockForNow();
		ctx2.fullBlockSync();

		Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
		Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getBlockHistory(ArgumentMatchers.any());

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

	@Test
	public void step_3_replace_generated() throws Exception {

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 + 1);

		ctx1.generateBlockForNow();
		ctx2.generateBlockForNow();

		Difficulty difficulty1 = ctx1.syncBlockPeerService.getDifficulty();
		Difficulty difficulty2 = ctx2.syncBlockPeerService.getDifficulty();

		ctx1.fullBlockSync();
		ctx2.fullBlockSync();

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
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

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		ctx1.generateBlockForNow();
		ctx2.generateBlockForNow();

		Difficulty difficulty1 = ctx1.syncBlockPeerService.getDifficulty();
		Difficulty difficulty2 = ctx2.syncBlockPeerService.getDifficulty();

		ctx1.fullBlockSync();
		ctx2.fullBlockSync();

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
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

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();

		int time = lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * Constant.BLOCK_IN_DAY * 2 + 1;
		Mockito.when(mockTimeProvider.get()).thenReturn(time);

		ctx1.generateBlockForNow();

		ctx2.syncBlockListTask.run();

		Mockito.verify(ctx1.syncBlockPeerService, Mockito.times(1)).getLastBlock();
		Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(2)).getBlockHistory(ArgumentMatchers.any());
		Mockito.verify(ctx1.syncBlockPeerService, Mockito.atLeast(2)).getDifficulty();

		Assert.assertEquals("Blockchain synchronized",
				ctx1.context.getInstance().getBlockchainService().getLastBlock().getID(),
				ctx2.context.getInstance().getBlockchainService().getLastBlock().getID());
	}

}
