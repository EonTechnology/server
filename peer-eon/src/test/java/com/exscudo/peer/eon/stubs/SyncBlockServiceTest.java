package com.exscudo.peer.eon.stubs;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Instance;

public class SyncBlockServiceTest {
	private SyncBlockService service;

	private IBlockchainService blockchain;

	@Before
	public void setup() throws Exception {

		blockchain = mock(IBlockchainService.class);
		Instance mockInstance = mock(Instance.class);
		when(mockInstance.getBlockchainService()).thenReturn(blockchain);
		ExecutionContext context = mock(ExecutionContext.class);
		when(context.getInstance()).thenReturn(mockInstance);

		service = new SyncBlockService(context);

	}

	@Test
	public void getDifficulty_should_return_last_block_info() throws Exception {

		Block mockBlock = mock(Block.class);
		when(mockBlock.getCumulativeDifficulty()).thenReturn(BigInteger.valueOf(100500L));
		when(mockBlock.getID()).thenReturn(1L);
		when(blockchain.getLastBlock()).thenReturn(mockBlock);

		Difficulty diff = service.getDifficulty();
		assertTrue(diff.compareTo(new Difficulty(1L, BigInteger.valueOf(100500L))) == 0);

	}

}