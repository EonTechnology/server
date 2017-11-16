package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;

public class BlockHelperTest {
	private ConnectionProxy connection = null;

	@Before
	public void setUp() throws Exception {
		connection = new ConnectionProxy(ConnectionUtils.create("/com/exscudo/peer/store/sqlite/blocks_test.sql"));
	}

	@After
	public void after() throws Exception {
		if (connection != null) {
			connection.getConnection().close();
		}
	}

	@Test()
	public void get_with_loadTrsAndProps_should_return_trs_amd_props() throws Exception {
		long blockId = -4478580686957051904L;

		Block block = BlockHelper.get(connection, blockId);
		assertNotNull(block);
		assertEquals(block.getID(), blockId);
		assertEquals(block.getCumulativeDifficulty(), new BigInteger("1"));

		Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
		assertEquals(transactions.length, 3);
		Arrays.sort(transactions, new Comparator<Transaction>() {
			@Override
			public int compare(Transaction o1, Transaction o2) {
				return Long.compare(o1.getID(), o2.getID());
			}
		});
		assertEquals(transactions[0].getID(), -5907171703930224640L);
		assertEquals(transactions[1].getID(), -5790597521193566208L);
		assertEquals(transactions[2].getID(), 8715428717435813888L);

	}

	@Test
	public void save() throws Exception {

		Block block = new Block();
		block.setVersion(1);
		block.setTimestamp(0);
		block.setPreviousBlock(0);
		block.setGenerationSignature(new byte[64]);
		block.setSenderID(12345L);
		block.setSignature(new byte[64]);
		block.setCumulativeDifficulty(new BigInteger("0"));
		block.setSnapshot(new byte[64]);

		long id = block.getID();
		assertNull(BlockHelper.get(connection, id));
		BlockHelper.save(connection, block);
		Block b = BlockHelper.get(connection, id);
		assertNotNull(b);
	}

	@Test
	public void remove() throws Exception {
		assertNotNull(BlockHelper.get(connection, 7816843914693836980L));
		BlockHelper.remove(connection, 7816843914693836980L);
		assertNull(BlockHelper.get(connection, 7816843914693836980L));
	}

	@Test
	public void blockLinkedList() throws Exception {
		long[] list = BlockHelper.getBlockLinkedList(connection, Integer.MIN_VALUE, Integer.MAX_VALUE);
		assertEquals(list.length, 4);
		assertEquals(list[0], 0L);
		assertEquals(list[1], -4478580686957051904L);
		assertEquals(list[2], 7816843914693836980L);
		assertEquals(list[3], -2972036271259516568L);
	}
}
