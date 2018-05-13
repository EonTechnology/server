package com.exscudo.peer.core;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;

/**
 * Hard fork is a pre-planned network update point. At that point in time, new
 * functionality is introduced into the network.
 * <p>
 * If the fork is expired, the state of the env is considered as irrelevant. In
 * this case, the tasks of synchronization and the generation of new blocks must
 * be stopped.
 */
public interface IFork {

    /**
     * Returns genesis-block ID.
     * <p>
     * Genesis block can be considered as a network identifier.
     */
    BlockID getGenesisBlockID();

    /**
     * Checks whether the hard-fork has been expired for the specified
     * {@code timestamp}.
     *
     * @param timestamp for which a check is made (unix timestamp)
     * @return true if time not in fork, otherwise false
     */
    boolean isPassed(int timestamp);

    /**
     * Checks whether the hard-fork started at the specified {@code timestamp}.
     * <p>
     * The moment that the fork was started at the specified time, does not
     * guarantee that it was not already completed
     *
     * @param timestamp for which a check is made (unix timestamp)
     * @return true if the hard-fork was started, otherwise - false.
     */
    boolean isCome(int timestamp);

    /**
     * Returns hard-fork number.
     * <p>
     * Hard-forks are numbered sequentially in ascending order.
     *
     * @param timestamp on which it is necessary to calculate the number of the hard-fork
     *                  (unix timestamp)
     * @return hard-fork number
     */
    int getNumber(int timestamp);

    /**
     * Returns the known transaction types.
     *
     * @param timestamp for which a types will be returned (unix timestamp)
     * @return set of types
     */
    Set<Integer> getTransactionTypes(int timestamp);

    /**
     * Returns the block version used for the specified time.
     *
     * @param timestamp for which a block version will be returned (unix timestamp)
     * @return block version or -1
     */
    int getBlockVersion(int timestamp);

    /**
     * Converts tree state on new fork.
     *
     * @param ledger    old tree state.
     * @param timestamp current time (unix timestamp)
     * @return new tree state.
     */
    ILedger covert(ILedger ledger, int timestamp);

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
    boolean validConfirmation(Transaction transaction, Map<AccountID, Account> set, int timestamp);

    /**
     * Returns the public key obtained from the specified SEED.
     *
     * @param seed
     * @param timestamp
     * @return
     * @throws IllegalArgumentException
     */
    byte[] getPublicKeyBySeed(String seed, int timestamp) throws IllegalArgumentException;

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

    /**
     * Calculates and returns the difficulty of the transaction.
     *
     * @param transaction
     * @param timestamp
     * @return
     */
    int getDifficulty(Transaction transaction, int timestamp);

    /**
     * Returns the maximum comment size allowed.
     *
     * @param timestamp
     * @return
     */
    int getMaxNoteLength(int timestamp);
}
