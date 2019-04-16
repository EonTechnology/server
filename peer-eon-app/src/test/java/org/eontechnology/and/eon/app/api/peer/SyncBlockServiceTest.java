package org.eontechnology.and.eon.app.api.peer;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.eontechnology.and.peer.core.api.Difficulty;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

public class SyncBlockServiceTest {
    private SyncBlockService service;

    private IBlockchainProvider blockchain;

    @Before
    public void setup() {

        blockchain = mock(IBlockchainProvider.class);
        ExecutionContext context = mock(ExecutionContext.class);

        service = new SyncBlockService(blockchain);
    }

    @Test
    public void getDifficulty_should_return_last_block_info() throws Exception {

        Block mockBlock = mock(Block.class);
        when(mockBlock.getCumulativeDifficulty()).thenReturn(BigInteger.valueOf(100500L));
        when(mockBlock.getID()).thenReturn(new BlockID(1L));
        when(blockchain.getLastBlock()).thenReturn(mockBlock);

        Difficulty diff = service.getDifficulty();
        assertTrue(diff.compareTo(new Difficulty(new BlockID(1L), BigInteger.valueOf(100500L))) == 0);
    }
}