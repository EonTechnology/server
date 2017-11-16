package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.Test;

public class ConnectionProxyFactoryTest {

	@Test
	public void createConnectionProxy() throws Exception {

		Connection mockConnection = mock(Connection.class);
		when(mockConnection.isClosed()).thenReturn(false);
		PreparedStatement mockStatement = mock(PreparedStatement.class);
		String sql = "Some sql";
		when(mockConnection.prepareStatement(sql)).thenReturn(mockStatement);
		ConnectionProxy proxy = new ConnectionProxy(mockConnection);

		PreparedStatement statement1 = proxy.prepareStatement(sql);
		PreparedStatement statement2 = proxy.prepareStatement(sql);

		verify(mockConnection, times(1)).prepareStatement(any(String.class));
		assertTrue(statement1 == statement2);

	}

}
