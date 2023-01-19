package org.eontechnology.and.eon.app.cfg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import org.eontechnology.and.eon.app.cfg.forks.ForkItem;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.junit.Before;
import org.junit.Test;

public class ForkTest {

  private Fork forkState;

  @Before
  public void setUp() throws Exception {
    forkState =
        new Fork(
            new BlockID(0L),
            new ForkItem[] {
              new ForkItem(1, Instant.ofEpochMilli(100 * 1000L).toString()),
              new ForkItem(2, Instant.ofEpochMilli(200 * 1000L).toString())
            },
            Instant.ofEpochMilli(1 * 1000L).toString());
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
