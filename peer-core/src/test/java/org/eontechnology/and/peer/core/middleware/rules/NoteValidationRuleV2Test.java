package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.data.Transaction;
import org.junit.Before;
import org.junit.Test;

public class NoteValidationRuleV2Test extends NoteValidationRuleTest {

  protected static final String alphabet_v2 =
      "-_.~ abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789 !*'();:@&=+$,/?%#[]";

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    rule = new NoteValidationRuleV2();
  }

  @Test
  public void check_alphabet2() throws Exception {
    Transaction tx =
        Builder.newTransaction(timeProvider).note(alphabet_v2).build(networkID, sender);
    validate(tx);
  }
}
