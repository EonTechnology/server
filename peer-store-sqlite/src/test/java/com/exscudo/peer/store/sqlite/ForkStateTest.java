package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.eon.TransactionHandlerDecorator;
import com.exscudo.peer.eon.TransactionType;

public class ForkStateTest {

	private Fork forkState;

	@Before
	public void setUp() throws Exception {
		ITransactionHandler handler = new TransactionHandlerDecorator(new HashMap<Integer, ITransactionHandler>() {
			private static final long serialVersionUID = 3518338953704623292L;

			{
				put(TransactionType.AccountRegistration,
						new com.exscudo.peer.eon.transactions.handlers.AccountRegistrationHandler());
				put(TransactionType.OrdinaryPayment,
						new com.exscudo.peer.eon.transactions.handlers.OrdinaryPaymentHandler());
				put(TransactionType.DepositRefill,
						new com.exscudo.peer.eon.transactions.handlers.DepositRefillHandler());
				put(TransactionType.DepositWithdraw,
						new com.exscudo.peer.eon.transactions.handlers.DepositWithdrawHandler());
			}
		});
		forkState = new Fork(0L, new ArrayList<Fork.Item>() {
			private static final long serialVersionUID = 1L;
			{
				add(new Fork.Item(1, 1 * 1000L, 100 * 1000L, new int[] { 1, 2 }, handler, 1));
				add(new Fork.Item(2, 100 * 1000L, 200 * 1000L, new int[] { 2, 3 }, handler, 1));
			}
		});

	}

	@Test
	public void isCome() {
		assertFalse("Before fork", forkState.isCome(50));
		assertFalse("Fork started", forkState.isCome(100));
		assertTrue("On fork", forkState.isCome(150));
		assertTrue("Fork ended", forkState.isCome(200));
		assertTrue("After fork", forkState.isCome(250));
	}

	@Test
	public void isPassed() {
		assertFalse("Before fork", forkState.isPassed(50));
		assertFalse("Fork started", forkState.isPassed(100));
		assertFalse("On fork", forkState.isPassed(150));
		assertFalse("Fork ended", forkState.isPassed(200));
		assertTrue("After fork", forkState.isPassed(250));
	}

	@Test
	public void getNumber() {
		assertEquals("Before fork", 1, forkState.getNumber(50));
		assertEquals("Fork started", 1, forkState.getNumber(100));
		assertEquals("On fork", 2, forkState.getNumber(150));
		assertEquals("Fork ended", 2, forkState.getNumber(200));
		assertEquals("After fork", 2, forkState.getNumber(250));
	}

	@Test
	public void isTranVersionSupported_v1() {
		Transaction tx = new Transaction();
		tx.setVersion(1);

		assertTrue("Before fork", forkState.isSupportedTran(tx, 50));
		assertTrue("Fork started", forkState.isSupportedTran(tx, 100));
		assertFalse("On fork", forkState.isSupportedTran(tx, 150));
		assertFalse("Fork ended", forkState.isSupportedTran(tx, 200));
		assertFalse("After fork", forkState.isSupportedTran(tx, 250));
	}

	@Test
	public void isTranVersionSupported_v2() {
		Transaction tx = new Transaction();
		tx.setVersion(2);

		assertTrue("Before fork", forkState.isSupportedTran(tx, 50));
		assertTrue("Fork started", forkState.isSupportedTran(tx, 100));
		assertTrue("On fork", forkState.isSupportedTran(tx, 150));
		assertTrue("Fork ended", forkState.isSupportedTran(tx, 200));
		assertTrue("After fork", forkState.isSupportedTran(tx, 250));
	}

	@Test
	public void isTranVersionSupported_v3() {
		Transaction tx = new Transaction();
		tx.setVersion(3);

		assertFalse("Before fork", forkState.isSupportedTran(tx, 50));
		assertFalse("Fork started", forkState.isSupportedTran(tx, 100));
		assertTrue("On fork", forkState.isSupportedTran(tx, 150));
		assertTrue("Fork ended", forkState.isSupportedTran(tx, 200));
		assertTrue("After fork", forkState.isSupportedTran(tx, 250));
	}
}
