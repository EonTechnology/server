package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.store.sqlite.utils.SettingHelper;

public class SettingHelperTest {
	private ConnectionProxy connection = null;

	@Before
	public void setUp() throws Exception {
		connection = new ConnectionProxy(
				ConnectionUtils.create("/com/exscudo/peer/store/sqlite/transactions_test.sql"));
	}

	@After
	public void after() throws Exception {
		if (connection != null) {
			connection.getConnection().close();
		}
	}

	@Test
	public void getValue() throws Exception {
		assertEquals(SettingHelper.getValue(connection, "Setting_1"), "Value_1");
	}

	@Test
	public void getNonExistentValue() throws Exception {
		assertNull(SettingHelper.getValue(connection, "Setting_NonExistent"));
	}

	@Test
	public void setValue() throws Exception {
		assertNull(SettingHelper.getValue(connection, "Setting_2"));
		SettingHelper.setValue(connection, "Setting_2", "Value_2");
		assertEquals(SettingHelper.getValue(connection, "Setting_2"), "Value_2");
	}
}
