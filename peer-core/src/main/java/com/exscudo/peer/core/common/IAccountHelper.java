package com.exscudo.peer.core.common;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;

public interface IAccountHelper {

    /**
     * Checks if the account could sign the block at the specified time.
     *
     * @param generator
     * @param timestamp
     * @return true if the account could create a block, otherwise - false.
     */
    boolean validateGenerator(Account generator, int timestamp);

    /**
     * Calculates and returns the cumulative difficulty of the block.
     *
     * @param block
     * @param generator
     * @param timestamp
     * @return
     */
    BigInteger getDifficultyAddition(Block block, Account generator, int timestamp);

    /**
     * Transfer fee for creating a block.
     *
     * @param account
     * @param fee
     * @param timestamp
     * @return
     */
    Account reward(Account account, long fee, int timestamp);

    /**
     * Returns a list of accounts that can perform transaction confirmation for the specified sender.
     *
     * @param sender
     * @param timestamp
     * @return
     */
    Set<AccountID> getConfirmingAccounts(Account sender, int timestamp);

    /**
     * Checks the sufficiency of the signature for the specified transaction in
     * accordance with the settings of the sender.
     *
     * @param transaction
     * @param set
     * @param timestamp
     * @return
     */
    boolean validConfirmation(Transaction transaction,
                              Map<AccountID, Account> set,
                              int timestamp) throws ValidateException;

    /**
     * Performs signature validation for the object.
     *
     * @param obj
     * @param signature
     * @param account
     * @param timestamp
     * @param <T>
     * @return
     */
    <T> boolean verifySignature(T obj, byte[] signature, Account account, int timestamp);
}
