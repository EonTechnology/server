package com.exscudo.peer.eon.stubs;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.LinkedBlock;
import com.exscudo.peer.eon.EngineConfigurator;
import com.exscudo.peer.eon.ExecutionContext;

public class SyncBlockServiceTest {
	private SyncBlockService service;

	private IBacklogService backlog;
	private IBlockchainService blockchain;

	@Before
	public void setup() throws Exception {

		backlog = mock(IBacklogService.class);
		blockchain = mock(IBlockchainService.class);

		EngineConfigurator cfg = new EngineConfigurator();
		ExecutionContext context = spy(cfg.setBacklog(backlog).setBlockchain(blockchain).setInnerPeers(new String[0])
				.setPublicPeers(new String[0]).build());
		service = new SyncBlockService(context);

	}

	@Test
	public void getDifficulty_should_return_last_block_info() throws Exception {

		LinkedBlock mockBlock = mock(LinkedBlock.class);
		when(mockBlock.getCumulativeDifficulty()).thenReturn(BigInteger.valueOf(100500L));
		when(mockBlock.getID()).thenReturn(1L);
		when(blockchain.getLastBlock()).thenReturn(mockBlock);

		Difficulty diff = service.getDifficulty();
		assertTrue(diff.compareTo(new Difficulty(1L, BigInteger.valueOf(100500L))) == 0);

	}

}