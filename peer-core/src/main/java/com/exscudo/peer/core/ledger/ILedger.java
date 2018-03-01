package com.exscudo.peer.core.ledger;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;

/**
 * Provides an access to accounts and their properties. Defines the state at a
 * certain height of the chain of the blocks.
 */
public interface ILedger extends Iterable<Account> {

    Account getAccount(AccountID accountID);

    ILedger putAccount(Account account);

    String getHash();
}
