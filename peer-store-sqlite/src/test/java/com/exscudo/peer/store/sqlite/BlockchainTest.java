package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.store.sqlite.core.Blockchain;

public class BlockchainTest {

	private Blockchain blockchain;

	@Before
	public void setUp() throws Exception {
		Storage storage = new Storage(ConnectionUtils.create("/com/exscudo/peer/store/sqlite/blocks_test.sql"));
		blockchain = new Blockchain(storage);
	}

	@Test
	public void getBlockHeightTest() {
		assertEquals(-1, blockchain.getBlockHeight(0L));
		assertEquals(0, blockchain.getBlockHeight(-4478580686957051904L));
		assertEquals(1, blockchain.getBlockHeight(7816843914693836980L));
		assertEquals(2, blockchain.getBlockHeight(-2972036271259516568L));
	}

	@Test
	public void getLatestBlocksTest() {

		long[] list = blockchain.getLatestBlocks(3);

		assertEquals(list.length, 3);
		assertEquals(list[0], -4478580686957051904L);
		assertEquals(list[1], 7816843914693836980L);
		assertEquals(list[2], -2972036271259516568L);

		list = blockchain.getLatestBlocks(2);

		assertEquals(list.length, 2);
		assertEquals(list[0], 7816843914693836980L);
		assertEquals(list[1], -2972036271259516568L);

		list = blockchain.getLatestBlocks(1);

		assertEquals(list.length, 1);
		assertEquals(list[0], -2972036271259516568L);
	}

}
