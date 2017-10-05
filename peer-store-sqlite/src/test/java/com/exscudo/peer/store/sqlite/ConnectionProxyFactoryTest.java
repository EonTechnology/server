package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;

public class ConnectionProxyFactoryTest {

	@Test
	public void createConnectionProxy() throws Exception {

		Connection mockConnection = mock(Connection.class);
		PreparedStatement mockStatement = mock(PreparedStatement.class);
		String sql = "Some sql";
		when(mockConnection.prepareStatement(sql)).thenReturn(mockStatement);
		ConnectionProxy proxy = new ConnectionProxy(mockConnection);
		PreparedStatement statement1 = proxy.prepareStatement(sql);
		PreparedStatement statement2 = proxy.prepareStatement(sql);
		verify(mockConnection, VerificationModeFactory.only()).prepareStatement(any(String.class));
		assertTrue(statement1 == statement2);

	}

}
