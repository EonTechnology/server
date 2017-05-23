package com.exscudo.eon.peer.data;

import java.util.Enumeration;
import java.util.Map;

import javax.naming.NamingException;

import com.exscudo.eon.peer.exceptions.ValidateException;

/**
 * This interface defines the extension to managing to the data.
 *
 */
public interface DatastoreConnector {

	public interface TransactionMapper extends Synchronizable {

		/**
		 * Returns a set of transactions that are not currently in the block.
		 * 
		 * @return
		 */
		Set unconfirmed();

		/**
		 * 
		 * @return
		 */
		Set doubleSpending();

		/**
		 * Returns a set of transactions that are in blocks.
		 * 
		 * @return
		 */
		Set confirmed();

		public interface Set {

			/**
			 * Returns the transaction with the specified <code>id</code> or
			 * null.
			 * 
			 * @param id
			 * @return
			 * @throws DataAccessException
			 */
			Transaction get(long id);

			/**
			 * Returns <code>true</code> if the set contains the specified
			 * transaction, otherwise - false.
			 * 
			 * @return
			 * @throws DataAccessException
			 */
			boolean contains(long id);

			/**
			 * Returns a list of transaction IDs.
			 * 
			 * @return
			 */
			default Enumeration<Long> indexes() {
				throw new UnsupportedOperationException("indexes");
			}

		}
	}

	/**
	 * This returns the Transaction Management API for the current connection.
	 * 
	 * @return
	 */
	TransactionMapper transactions();

	public interface BlockLinkedList extends Synchronizable {

		/**
		 * Finds and returns the block with the specified <code>id</code> or
		 * null.
		 * 
		 * @param id
		 * @return
		 * @throws DataAccessException
		 */
		Block getBlock(long id);

		/**
		 * Returns the last block in the linked list.
		 * 
		 * @return
		 * @throws DataAccessException
		 */
		Block getLastBlock();

		/**
		 * Returns the id of the last block.
		 * 
		 * @return
		 * @throws DataAccessException
		 */
		long getLastBlockID();

		/**
		 * Returns Genesis block id
		 * 
		 * @return
		 * @throws DataAccessException
		 */
		long getGenesisBlockID();

		/**
		 * Returns the block index in the chain.
		 * 
		 * @param id
		 *            block ID.
		 * @return
		 * @throws DataAccessException
		 */
		int getBlockHeight(long id);

		/**
		 * Finds the latest blocks in the chain.
		 * 
		 * @param frameSize
		 *            The number of returned units.
		 * @return
		 * @throws DataAccessException
		 */
		Long[] getLatestBlocks(int frameSize);

	}

	/**
	 * This returns the Block Management API for the current connection.
	 * 
	 * @return
	 */
	BlockLinkedList blocks();

	/**
	 * Creates a new UnitOfWork instance.
	 * 
	 * @param name
	 * @return a new instance, which has been started.
	 */
	UnitOfWork createUnitOfWork(String name);

	/**
	 * Puts passed <code>transaction</code> to the Backlog.
	 * 
	 * @param transaction
	 *            the transaction for processing.
	 * @throws ValidateException
	 *             If some property of the specified <code>transaction</code>
	 *             prevents it from being processed.
	 * @throws NamingException
	 *             If the <code>transaction</code> type is not supported.
	 * @throws DataAccessException
	 *             If data access error.
	 */
	void importTransaction(Transaction transaction) throws ValidateException, NamingException;

	/**
	 * Removes the transaction with the specified <code>id</code> from Backlog.
	 * 
	 * @param id
	 * @return Returns the removed transaction or null if the transaction is not
	 *         exist.
	 * @throws DataAccessException
	 *             If data access error.
	 */
	Transaction removeTransaction(long id);

	/**
	 * Adding a <code>block</code> to the end of the chain with checking all
	 * data.
	 * 
	 * @param nextBlock
	 * @param transactions
	 *            Transactions that are attached to the block
	 * @throws ValidateException
	 *             If some property of the specified block or attached
	 *             transactions prevents this block from being processed.
	 * @throws NamingException
	 *             If the transaction that enters the block has an unknown type
	 * @throws DataAccessException
	 *             If data access error
	 */
	void importBlock(Block nextBlock, Map<Long, Transaction> transactions) throws ValidateException, NamingException;

	/**
	 * Adding a <code>block</code> to the end of the chain without checking.
	 * Used when a block was created on the current node.
	 * 
	 * @param nextBlock
	 * @param transactions
	 *            Transactions that are attached to the block
	 * @throws ValidateException
	 *             If some property of the specified block or attached
	 *             transactions prevents this block from being processed.
	 * @throws NamingException
	 *             If the transaction that enters the block has an unknown type.
	 * @throws DataAccessException
	 *             If data access error.
	 */
	void pushBlock(Block nextBlock, Map<Long, Transaction> transactions) throws ValidateException, NamingException;

	/**
	 * Delete the last blocks from the chain to the specified by
	 * <code>blockID</code>.
	 * 
	 * @param blockID
	 * @throws DataAccessException
	 *             If data access error
	 */
	void popTo(long blockID);

	/**
	 * Creates a new block to add to the end of the chain.
	 * 
	 * @param previousBlock
	 * @param secretPhrase
	 *            The secret phrase of the account on behalf of which the block
	 *            is created
	 * @return created block.
	 * @throws ValidateException
	 *             if error in the block for which the subsequent block is
	 *             created.
	 * @throws DataAccessException
	 *             If data access error.
	 */
	Block createNextBlock(Block previousBlock, String secretPhrase) throws ValidateException;

}
