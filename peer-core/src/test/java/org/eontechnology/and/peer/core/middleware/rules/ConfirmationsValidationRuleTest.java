package org.eontechnology.and.peer.core.middleware.rules;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import org.eontechnology.and.peer.core.Builder;
import org.eontechnology.and.peer.core.Constant;
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

public class ConfirmationsValidationRuleTest extends AbstractValidationRuleTest {
  private ConfirmationsValidationRule rule;

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

    Mockito.when(
            accountHelper.validConfirmation(
                ArgumentMatchers.any(), ArgumentMatchers.anyMap(), ArgumentMatchers.anyInt()))
        .then(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Transaction tx = invocation.getArgument(0);
                Map<AccountID, Account> map = invocation.getArgument(1);

                if (tx.getSenderID().equals(senderAccount.getID())) {
                  return map.containsKey(senderAccount.getID())
                      && map.containsKey(delegate1Account.getID());
                }

                throw new IllegalArgumentException();
              }
            });

    rule = new ConfirmationsValidationRule(timeProvider, accountHelper);
  }

  @Test
  public void unknown_sender() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Unknown sender.");

    Mockito.when(ledger.getAccount(senderAccount.getID())).thenReturn(null);

    Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender);
    validate(tx);
  }

  @Test
  public void unknown_delegate() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Unknown account " + delegate1Account.getID());

    Mockito.when(ledger.getAccount(delegate1Account.getID())).thenReturn(null);

    Transaction tx =
        Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {delegate1});
    validate(tx);
  }

  @Test
  public void invalid_quorum_without_confirmation() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("The quorum is not exist.");

    Transaction tx =
        Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {});
    validate(tx);
  }

  @Test
  public void invalid_quorum_with_confirmation() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("The quorum is not exist.");

    Transaction tx =
        Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {delegate2});
    validate(tx);
  }

  @Test
  public void quorum_with_confirmation() throws Exception {
    Transaction tx =
        Builder.newTransaction(timeProvider)
            .build(networkID, sender, new ISigner[] {delegate1, delegate2});
    validate(tx);
  }

  @Test
  public void quorum_with_partial_confirmation() throws Exception {
    Transaction tx =
        Builder.newTransaction(timeProvider).build(networkID, sender, new ISigner[] {delegate1});
    validate(tx);
  }

  @Test
  public void confirmation_limit() throws Exception {
    expectedException.expect(ValidateException.class);
    expectedException.expectMessage("Invalid use of the confirmation field.");

    Random random = new Random();
    String alphabet = "0123456789abcdef";
    char[] symbols = alphabet.toCharArray();

    ISigner[] signers = new ISigner[Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE + 1];
    for (int i = 0; i < Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE + 1; i++) {

      char[] buffer = new char[64];
      for (int j = 0; j < buffer.length; j++) {
        buffer[j] = symbols[random.nextInt(symbols.length)];
      }
      signers[i] = new Signer(new String(buffer));
    }

    Transaction tx = Builder.newTransaction(timeProvider).build(networkID, sender, signers);
    validate(tx);
  }

  @Test
  public void unspecified_delegate() throws Exception {
    ISigner unknown =
        new Signer("33445566778899aabbccddeeff00112233445566778899aabbccddeeff001122");
    Account unknownAccount = Mockito.spy(new TestAccount(new AccountID(unknown.getPublicKey())));
    ledger.putAccount(unknownAccount);

    Transaction tx =
        Builder.newTransaction(timeProvider)
            .build(networkID, sender, new ISigner[] {unknown, delegate1, delegate2});
    validate(tx);
  }
}
