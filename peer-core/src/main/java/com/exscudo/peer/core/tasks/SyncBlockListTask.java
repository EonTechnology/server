package com.exscudo.peer.core.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exscudo.peer.core.AbstractContext;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IInstance;
import com.exscudo.peer.core.IPeer;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.ProtocolException;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IBlockSynchronizationService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;

/**
 * Performs the task of synchronizing the chain of the current node and a random
 * node.
 * <p>
 * In the first step, a random node for interaction is selected. The random node
 * selection algorithm must be described in a certain {@code AbstractContext}
 * implementation. The second step, it compares difficulty of the chain at the
 * services and the current nodes. The synchronization process starts if the
 * "difficulty" of the last block of the services node is more than the
 * "difficulty" of the last block.
 */
public final class SyncBlockListTask implements Runnable {

	private final AbstractContext<?, ?> context;

	public SyncBlockListTask(AbstractContext<?, ?> context) {
		this.context = context;
	}

	@Override
	public void run() {

		try {

			IInstance instance = context.getInstance();
			IPeer peer = context.getAnyConnectedPeer();
			if (peer == null) {
				return;
			}

			try {

				IBlockSynchronizationService service = peer.getBlockSynchronizationService();
				Difficulty remoteState = service.getDifficulty();

				IBlockchainService blockchain = instance.getBlockchainService();
				Difficulty currentState = new Difficulty(blockchain.getLastBlock());

				if (remoteState.compareTo(currentState) == 0) {

					// The chain of the blocks is synchronized with at
					// least one node. Initiate an event and return.
					Loggers.trace(SyncBlockListTask.class, "The chain of the blocks is synchronized with \"{}\".",
							peer);
					context.raiseSynchronizedEvent(this, peer);
					return;

				}

				if (remoteState.compareTo(currentState) < 0) {

					// The "difficulty" of the chain at the services node is
					// lower than the current one. No need for synchronization.
					return;

				}

				// Starts the synchronization process, if the "difficulty" of
				// the last block of the randomly node more than the
				// "difficulty" of our last block.

				Loggers.info(SyncBlockListTask.class, "Begining synchronization. Difficulty: [this] {}, [{}] {}. ",
						currentState.getDifficulty(), peer, remoteState.getDifficulty());

				if (shortSyncScheme(service) != null) {
					return;
				}

				while (remoteState.compareTo(currentState) > 0) {

					Block headBlock = longSyncScheme(service);

					Difficulty currentStateNew = new Difficulty(headBlock);
					if (currentStateNew.compareTo(currentState) == 0) {
						return;
					}
					currentState = currentStateNew;
					remoteState = service.getDifficulty();
				}

			} catch (RemotePeerException | IOException e) {

				context.disablePeer(peer);

				Loggers.trace(SyncBlockListTask.class, "Failed to execute a request. Target: " + peer, e);
				Loggers.debug(SyncBlockListTask.class, "The node is disconnected. \"{}\".", peer);

			} catch (ProtocolException e) {

				context.blacklistPeer(peer);

				Loggers.error(SyncBlockListTask.class, "Failed to sync with '" + peer + "'", e);
				Loggers.debug(SyncBlockListTask.class, "The node is disconnected. \"{}\".", peer);
				throw e;

			}

		} catch (Exception e) {
			Loggers.error(SyncBlockListTask.class, e);
		}

	}

	/**
	 * Short synchronization scheme (synchronization of the last block only). In
	 * most cases, if the network is in a "stable" state, the synchronization will
	 * be performed exactly by the short scheme.
	 * <p>
	 * At first, the last block is requested on the services peer. In the second
	 * step, searches for a block preceding the received from services node. If it
	 * is contained in the chain of blocks on the current node, then its added to
	 * the end of chain (if necessary, roll back the local chain of blocks to common
	 * block). The criterion for adding the block is difficulty.
	 *
	 * @param service
	 *            access point on a remote node that implements the block
	 *            synchronization protocol.
	 * @return true if the chain of blocks was synchronized, if a short
	 *         synchronization algorithm is not applicable - false
	 * @throws IOException
	 *             Error during access to the services node.
	 * @throws RemotePeerException
	 *             Error during request processing on the services node. (e.g.
	 *             illegal arguments, invalid format, etc.)
	 * @throws ProtocolException
	 *             The behavior of the services node does not match expectations.
	 */
	private Block shortSyncScheme(IBlockSynchronizationService service)
			throws ProtocolException, IOException, RemotePeerException {

		Loggers.info(SyncBlockListTask.class, "ShortSyncScheme");

		Block newBlock = service.getLastBlock();
		Difficulty remoteState = new Difficulty(newBlock.getID(), newBlock.getCumulativeDifficulty());

		IBlockchainService blockchain = context.getInstance().getBlockchainService();
		Difficulty currentState = new Difficulty(blockchain.getLastBlock());

		if (remoteState.compareTo(currentState) <= 0) {
			throw new IllegalStateException("The state was changed.");
		}

		Block prevBlock = blockchain.getBlock(newBlock.getPreviousBlock());
		if (prevBlock == null) {
			return null;
		}

		if (context.isCurrentForkPassed(newBlock.getTimestamp())) {
			throw new ProtocolException("Incorrect FORK");
		}

		HashMap<Long, Block> futuresBlock = new HashMap<>();
		futuresBlock.put(newBlock.getPreviousBlock(), newBlock);
		return pushBlocks(blockchain, prevBlock, futuresBlock);

	}

	/**
	 * Long synchronization scheme (synchronization from the common block). This
	 * synchronization mode is used usually when a node in unstable state (for
	 * example, it is used during connected to a network).
	 * <p>
	 * In the process of synchronization, the chain is rolled back to the point of
	 * division of the chain. Then the new ending imported. The maximum depth of
	 * division is defined in {@link Constant#SYNC_MILESTONE_DEPTH}
	 *
	 * @param service
	 *            access point on a remote node that implements the block
	 *            synchronization protocol.
	 * @throws IOException
	 *             Error during access to the services node.
	 * @throws RemotePeerException
	 *             Error during request processing on the services node. (e.g.
	 *             illegal arguments, invalid format, etc.)
	 * @throws ProtocolException
	 *             The behavior of the services node does not match expectations.
	 */
	private Block longSyncScheme(IBlockSynchronizationService service)
			throws ProtocolException, IOException, RemotePeerException {

		Loggers.info(SyncBlockListTask.class, "LongSyncScheme");

		IBlockchainService blockchain = context.getInstance().getBlockchainService();
		Block lastBlock = blockchain.getLastBlock();
		Difficulty beginState = new Difficulty(lastBlock);

		long[] lastBlockIDs = blockchain.getLatestBlocks(Constant.SYNC_SHORT_FRAME);
		Block[] items = service.getBlockHistory(blockIdEncode(lastBlockIDs));
		if (items.length == 0) {
			// Common blocks was not found... Requests blocks that have been
			// added since the last milestone.
			Loggers.warning(SyncBlockListTask.class, "Sync over a latest milestone.");
			lastBlockIDs = blockchain.getLatestBlocks(Constant.SYNC_LONG_FRAME);
			items = service.getBlockHistory(blockIdEncode(lastBlockIDs));
		}

		Block commonBlock = getCommonBlockID(items);
		if (commonBlock == null) {
			throw new ProtocolException("Unable to get common block.");
		}

		List<Block> linked = getNextBlockchain(commonBlock, items);

		if (linked.size() == 0) {
			return lastBlock;
		}

		Map<Long, Block> futureBlocks = new HashMap<>();
		for (Block block : linked) {
			futureBlocks.put(block.getPreviousBlock(), block);
		}

		Block newBlock = linked.get(linked.size() - 1);
		Difficulty newState = new Difficulty(newBlock);

		int newSize = futureBlocks.size();
		while (newState.compareTo(beginState) < 0 && newSize == futureBlocks.size()) {

			lastBlockIDs = new long[] { newBlock.getID() };
			items = service.getBlockHistory(blockIdEncode(lastBlockIDs));

			newSize = futureBlocks.size() + items.length;

			linked = getNextBlockchain(newBlock, items);

			if (linked.size() > 0) {

				for (Block block : linked) {
					futureBlocks.put(block.getPreviousBlock(), block);
				}

				newBlock = linked.get(linked.size() - 1);
				newState = new Difficulty(newBlock);
			}

		}

		Loggers.warning(SyncBlockListTask.class, "Target difficulty {} in {}", newState.getDifficulty(),
				Format.ID.blockId(newState.getLastBlockID()));

		if (newState.compareTo(beginState) <= 0) {
			throw new ProtocolException(" Invalid difficulty. Before: " + beginState.getDifficulty() + ", after: "
					+ newState.getDifficulty());
		}

		// There are blocks after the general block
		if (futureBlocks.isEmpty()) {
			return lastBlock;
		}

		return pushBlocks(blockchain, commonBlock, futureBlocks);

	}

	private List<Block> getNextBlockchain(Block commonBlock, Block[] items) {

		Map<Long, Block> map = new HashMap<>();
		for (Block block : items) {
			map.put(block.getPreviousBlock(), block);
		}

		ArrayList<Block> next = new ArrayList<>(items.length);

		while (true) {
			Block newBlock = map.get(commonBlock.getID());

			if (newBlock == null) {
				return next;
			}

			if (context.isCurrentForkPassed(newBlock.getTimestamp())) {
				return next;
			}

			if (newBlock.isFuture(context.getCurrentTime() + Constant.MAX_LATENCY)) {
				return next;
			}

			next.add(newBlock);
			newBlock.setHeight(commonBlock.getHeight() + 1);
			commonBlock = newBlock;
		}
	}

	private Block getCommonBlockID(Block[] items) {
		long commonBlockID = 0L;
		int commonHeight = -1;

		IBlockchainService blockchain = context.getInstance().getBlockchainService();

		for (Block block : items) {
			int height = blockchain.getBlockHeight(block.getPreviousBlock());
			if (height > commonHeight) {
				commonHeight = height;
				commonBlockID = block.getPreviousBlock();
			}
		}

		return blockchain.getBlock(commonBlockID);
	}

	private Block pushBlocks(IBlockchainService blockchain, Block commonBlock, Map<Long, Block> futureBlocks)
			throws ProtocolException {

		if (blockchain.getBlock(commonBlock.getID()) == null)
			throw new IllegalStateException();

		Block lastBlock = blockchain.getLastBlock();
		if ((lastBlock.getHeight() - commonBlock.getHeight()) > Constant.SYNC_MILESTONE_DEPTH) {
			throw new IllegalStateException("Failed to remove blocks. Illegal depth.");
		}

		Block maxBlock = commonBlock;
		while (futureBlocks.containsKey(maxBlock.getID())) {
			maxBlock = futureBlocks.get(maxBlock.getID());
		}

		Difficulty currentState = new Difficulty(blockchain.getLastBlock());
		Difficulty targetState = new Difficulty(maxBlock);

		if (targetState.compareTo(currentState) <= 0) {
			return lastBlock;
		}

		Loggers.trace(SyncBlockListTask.class, "Last block: [{}]{}. Common block: [{}]{}.", lastBlock.getHeight(),
				Format.ID.blockId(currentState.getLastBlockID()), commonBlock.getHeight(),
				Format.ID.blockId(commonBlock.getID()));

		Block currBlock = commonBlock;
		IUnitOfWork uow = blockchain.beginPush(this, currBlock);
		try {

			try {

				long newBlockID = currBlock.getID();
				while (futureBlocks.containsKey(newBlockID)) {

					Block newBlock = futureBlocks.get(newBlockID);
					if (newBlock.isFuture(context.getCurrentTime() + Constant.MAX_LATENCY)) {
						throw new LifecycleException(Format.ID.blockId(newBlock.getID()));
					}
					if (context.isCurrentForkPassed(newBlock.getTimestamp())) {
						throw new LifecycleException("Incorrect FORK");
					}

					Loggers.info(SyncBlockListTask.class, "Block pushing... [{}] {} -> {}", newBlock.getHeight(),
							Format.ID.blockId(newBlock.getPreviousBlock()), Format.ID.blockId(newBlock.getID()));

					currBlock = uow.pushBlock(newBlock);
					newBlockID = currBlock.getID();

					Loggers.info(SyncBlockListTask.class, "Block pushed: [{}] {} CD: {}", currBlock.getHeight(),
							Format.ID.blockId(newBlock.getID()), currBlock.getCumulativeDifficulty());

				}

			} catch (ValidateException e) {
				throw new ProtocolException(e);
			} catch (Exception ignore) {
				Loggers.error(SyncBlockListTask.class, ignore);
			}

			Difficulty diff = new Difficulty(currBlock);

			// If node have imported all the blocks, the difficulty should match
			if (diff.getLastBlockID() == targetState.getLastBlockID()
					&& !diff.getDifficulty().equals(targetState.getDifficulty())) {
				throw new ProtocolException("The Difficulty of the latest block is not valid.");
			}

			// If node have imported a part of the sequence
			if (diff.compareTo(currentState) > 0) {
				uow.commit();
				Loggers.info(SyncBlockListTask.class, "Sync complete. [{}]{} -> [{}]{}", commonBlock.getHeight(),
						Format.ID.blockId(commonBlock.getID()), currBlock.getHeight(),
						Format.ID.blockId(diff.getLastBlockID()));
			} else {

				// There were problems with the addition of the block. The node
				// is placed in the black list.
				throw new ProtocolException(
						"Failed to sync. Before: " + currentState.getDifficulty() + ", after: " + diff.getDifficulty());
			}

			return currBlock;

		} catch (Throwable e) {
			uow.rollback();
			throw e;
		}
	}

	/**
	 * Encode id set to user-friendly strings
	 * 
	 * @param ids
	 * @return
	 */
	private String[] blockIdEncode(long[] ids) {

		String[] encoded = new String[ids.length];
		for (int i = 0; i < ids.length; i++) {
			encoded[i] = Format.ID.blockId(ids[i]);
		}
		return encoded;

	}

}