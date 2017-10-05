package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.store.sqlite.utils.AccountHelper;

public class AccountHelperTest {
	private ConnectionProxy connection = null;

	@Before
	public void setUp() throws Exception {
		connection = ConnectionUtils.create("/com/exscudo/eon/sqlite/accounts_test.sql");
	}

	@After
	public void after() throws Exception {
		if (connection != null) {
			connection.getConnection().close();
		}
	}

	@Test
	public void getAccount() throws Exception {
		long accountID = 4085011828883941788L;
		Account account = AccountHelper.getAccount(connection, accountID, Integer.MAX_VALUE);
		assertNotNull(account);
		assertEquals(account.getProperties().size(), 4);
		Map<String, Object> value = account.getProperty(UUID.fromString("00000000-0000-0000-0000-000000000001"))
				.getData();
		assertEquals(value.get("param"), "value");
	}

	@Test
	public void getNonExistentAccount() throws Exception {
		long accountID = 315037177L;
		IAccount account = AccountHelper.getAccount(connection, accountID, Integer.MAX_VALUE);
		assertNull(account);
	}

}
