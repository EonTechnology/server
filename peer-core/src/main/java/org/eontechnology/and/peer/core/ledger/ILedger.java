package org.eontechnology.and.peer.core.ledger;

import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;

/**
 * Provides an access to accounts and their properties. Defines the state at a certain height of the
 * chain of the blocks.
 */
public interface ILedger extends Iterable<Account> {

  Account getAccount(AccountID accountID);

  ILedger putAccount(Account account);

  String getHash();

  void save();
}
