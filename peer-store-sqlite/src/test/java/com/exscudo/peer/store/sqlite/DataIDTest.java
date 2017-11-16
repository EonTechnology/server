package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

public class DataIDTest {

	@Test
	public void blocks_test_sql() throws Exception {
		ConnectionProxy connection = new ConnectionProxy(
				ConnectionUtils.create("/com/exscudo/peer/store/sqlite/blocks_test.sql"));
		long tranIDs[] = new long[] { -5790597521193566208L, -5907171703930224640L, 8715428717435813888L,
				-8452106892286627576L, 3653135999151632648L, -9055899704065265399L };

		for (long id : tranIDs) {
			Transaction tx = TransactionHelper.get(connection, id);
			assertEquals(tx.getID(), id);
		}

		long blockIDs[] = new long[] { -4478580686957051904L, -2972036271259516568L, 7816843914693836980L };

		for (long id : blockIDs) {
			Block bl = BlockHelper.get(connection, id);
			assertEquals(bl.getID(), id);
		}
	}

	@Test
	public void transactions_test_sql() throws Exception {
		ConnectionProxy connection = new ConnectionProxy(
				ConnectionUtils.create("/com/exscudo/peer/store/sqlite/transactions_test.sql"));
		long ids[] = new long[]{2641518845407277113L, -1036124464445656823L, -1240188797449146102L,
				6265336001932030219L, 4381492504715849996L};

		for (long id : ids) {
			Transaction tx = TransactionHelper.get(connection, id);
			assertEquals(tx.getID(), id);
		}
	}
}
