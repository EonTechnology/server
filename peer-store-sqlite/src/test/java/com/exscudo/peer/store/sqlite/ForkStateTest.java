package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.exscudo.peer.core.Fork;

public class ForkStateTest {

	private Fork forkState;

	@Before
	public void setUp() throws Exception {
		forkState = new Fork(0, "2017-10-01T00:00:00.00Z", "2017-10-03T12:00:00.00Z") {
			{
				BEGIN = 100 * 1000L; // in ms
				END = 200 * 1000L; // in ms
				FORK = 2;
			}
		};
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
}
