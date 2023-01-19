package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Signer;
import org.eontechnology.and.peer.core.common.exceptions.LifecycleException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.middleware.AbstractValidationRuleTest;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FutureTimestampValidationRuleTest extends AbstractValidationRuleTest {
  private FutureTimestampValidationRule rule;
  private ISigner sender =
      new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");

  @Override
  protected IValidationRule getValidationRule() {
    return rule;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    rule = new FutureTimestampValidationRule(timeProvider);
  }

  @Test
  public void success() throws Exception {
    Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
    validate(tx);
  }

  @Test
  public void future_timestamp() throws Exception {
    expectedException.expect(LifecycleException.class);

    Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);

    int timestamp = tx.getTimestamp() - 1;
    Mockito.when(timeProvider.get()).thenReturn(timestamp);
    validate(tx);
  }
}
