package org.eontechnology.and.peer.core.middleware.rules;

import java.util.HashSet;
import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Signer;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.AbstractValidationRuleTest;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.TestAccount;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ConfirmationsSetValidationRuleTest extends AbstractValidationRuleTest {
  private ConfirmationsSetValidationRule rule;

  private ISigner sender =
      new Signer("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
  private Account senderAccount;

  private ISigner delegate1 =
      new Signer("112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00");
  private Account delegate1Account;

  private ISigner delegate2 =
      new Signer("2233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011");
  private Account delegate2Account;

  @Override
  protected IValidationRule getValidationRule() {
    return rule;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    senderAccount = Mockito.spy(new TestAccount(new AccountID(sender.getPublicKey())));
    delegate1Account = Mockito.spy(new TestAccount(new AccountID(delegate1.getPublicKey())));
    delegate2Account = Mockito.spy(new TestAccount(new AccountID(delegate2.getPublicKey())));

    ledger.putAccount(senderAccount);
    ledger.putAccount(delegate1Account);
    ledger.putAccount(delegate2Account);

    Mockito.when(fork.getTransactionTypes(ArgumentMatchers.anyInt()))
        .thenReturn(
            new HashSet<Integer>() {
              {
                add(1);
              }
            });

    // required confirmations: delegate1 or (delegate 1 and delegate2)
    Mockito.when(
            accountHelper.getConfirmingAccounts(ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
        .then(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Account account = invocation.getArgument(0);
                if (!account.getID().equals(senderAccount.getID())) {
                  return null;
                }
                HashSet<AccountID> set = new HashSet<>();
                set.add(delegate1Account.getID());
                set.add(delegate2Account.getID());
                return set;
              }
            });

    rule = new ConfirmationsSetValidationRule(timeProvider, accountHelper);
  }

  @Test
  public void unset_mfa() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("can not sign transaction.");

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .build(networkID, delegate1, new ISigner[] {sender, delegate2});
    validate(tx);
  }

  @Test
  public void unspecified_delegate() throws Exception {
    ISigner unknown =
        new Signer("33445566778899aabbccddeeff00112233445566778899aabbccddeeff001122");
    AccountID id = new AccountID(unknown.getPublicKey());

    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Account '" + id + "' can not sign transaction.");

    Transaction tx =
        Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {unknown});
    validate(tx);
  }

  @Test
  public void mfa_allow_payer() throws Exception {
    ISigner unknown =
        new Signer("33445566778899aabbccddeeff00112233445566778899aabbccddeeff001122");
    AccountID id = new AccountID(unknown.getPublicKey());

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .payedBy(id)
            .build(networkID, sender, new ISigner[] {unknown});
    validate(tx);
  }

  @Test
  public void mfa_not_allow_payer() throws Exception {
    rule.setAllowPayer(false);

    ISigner unknown =
        new Signer("33445566778899aabbccddeeff00112233445566778899aabbccddeeff001122");
    AccountID id = new AccountID(unknown.getPublicKey());

    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Account '" + id + "' can not sign transaction.");

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .payedBy(id)
            .build(networkID, sender, new ISigner[] {unknown});
    validate(tx);
  }

  @Test
  public void unset_mfa_allow_payer() throws Exception {
    AccountID id = new AccountID(sender.getPublicKey());

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .payedBy(id)
            .build(networkID, delegate1, new ISigner[] {sender});
    validate(tx);
  }

  @Test
  public void unset_mfa_not_allow_payer() throws Exception {
    rule.setAllowPayer(false);

    AccountID id = new AccountID(sender.getPublicKey());

    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Account '" + id + "' can not sign transaction.");

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .payedBy(id)
            .build(networkID, delegate1, new ISigner[] {sender});
    validate(tx);
  }
}
