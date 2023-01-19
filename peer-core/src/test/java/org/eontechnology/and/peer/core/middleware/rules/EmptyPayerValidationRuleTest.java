package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Signer;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.AbstractValidationRuleTest;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.junit.Before;
import org.junit.Test;

public class EmptyPayerValidationRuleTest extends AbstractValidationRuleTest {
  private EmptyPayerValidationRule rule;
  private ISigner sender =
      new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
  private ISigner payer =
      new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
  private AccountID payerAccount = new AccountID(payer.getPublicKey());

  @Override
  protected IValidationRule getValidationRule() {
    return rule;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    rule = new EmptyPayerValidationRule();
  }

  @Test
  public void payer_error() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Forbidden.");

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .payedBy(payerAccount)
            .build(networkID, sender, new ISigner[] {payer});
    validate(tx);
  }

  @Test
  public void payer_success() throws Exception {
    Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
    validate(tx);
  }

  @Test
  public void payer_nested_error() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Forbidden.");

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .payedBy(payerAccount)
            .build(networkID, sender, new ISigner[] {payer});
    validate(Builder.newTransaction(timeProvider).addNested(tx).build(networkID, sender));
  }

  @Test
  public void payer_nested_success() throws Exception {
    Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
    validate(Builder.newTransaction(timeProvider).addNested(tx).build(networkID, sender));
  }
}
