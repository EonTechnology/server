package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

public class TransactionHelperTest {

	private ConnectionProxy connection = null;

	@Before
	public void setUp() throws Exception {
		connection = ConnectionUtils.create("/com/exscudo/eon/sqlite/transactions_test.sql");
	}

	@After
	public void after() throws Exception {
		if (connection != null) {
			connection.getConnection().close();
		}
	}

	@Test
	public void get() throws Exception {

		Transaction tx = TransactionHelper.get(connection, 4381492504715849993L);
		assertEquals(Format.ID.accountId(tx.getSenderID()), "EON-GKQXZ-7DMS8-QL65R");
		assertEquals(tx.getBlock(), 0);
		assertEquals(tx.getDeadline(), 60);
		assertEquals(tx.getFee(), 5);
		assertEquals(tx.getHeight(), Integer.MAX_VALUE);
		assertEquals(tx.getData().get("recipient"), "EON-WEUCY-TPM29-EK53X");
		assertEquals(tx.getData().get("amount"), 50L);
		assertEquals(tx.getReference(), 0);
		assertEquals(tx.getType(), 2);
		assertEquals(tx.getTimestamp(), 1503654153);
	}

	@Test
	public void getNonExistent() throws Exception {
		assertNull(TransactionHelper.get(connection, -1));
	}

	@Test
	public void contains() throws Exception {
		assertTrue(TransactionHelper.contains(connection, 2641518845407277113L));
		assertFalse(TransactionHelper.contains(connection, -1));
	}

	@Test
	public void save() throws Exception {

		Transaction tx = new Transaction();
		tx.setType(0);
		tx.setTimestamp(0);
		tx.setDeadline((short) 60);
		tx.setReference(0);
		tx.setSenderID(12345L);
		tx.setFee(0);
		tx.setData(new HashMap<>());
		tx.setSignature(new byte[64]);

		long id = tx.getID();
		assertFalse(TransactionHelper.contains(connection, id));
		TransactionHelper.save(connection, tx);
		tx = TransactionHelper.get(connection, id);
		assertEquals(tx.getID(), id);
		assertEquals(tx.getBlock(), 0);
		assertEquals(tx.getHeight(), Integer.MAX_VALUE);
	}

	@Test
	public void remove() throws Exception {
		long id = 2641518845407277113L;
		assertTrue(TransactionHelper.contains(connection, id));
		TransactionHelper.remove(connection, id);
		assertFalse(TransactionHelper.contains(connection, id));
	}

	@Test
	public void findByRecipient() throws Exception {
		long accountID = 4085011828883941788L;
		Map<Long, Transaction> map = TransactionHelper.findByRecipient(connection, accountID);
		assertTrue(map.size() == 2);
		assertTrue(map.containsKey(4381492504715849993L));
		assertTrue(map.containsKey(6265336001932030217L));
	}

	@Test
	public void findBySender() throws Exception {
		long accountID = 4085011828883941788L;
		Map<Long, Transaction> map = TransactionHelper.findBySender(connection, accountID);
		assertTrue(map.size() == 1);
		assertTrue(map.containsKey(2641518845407277113L));
	}
}
