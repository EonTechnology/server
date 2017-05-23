package com.exscudo.eon.peer.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.eon.peer.Constant;
import com.exscudo.eon.peer.ExecutionContext;
import com.exscudo.eon.peer.Peer;
import com.exscudo.eon.peer.Peer.State;
import com.exscudo.eon.peer.TimeProvider;
import com.exscudo.eon.peer.contract.DataSynchronizationService;
import com.exscudo.eon.peer.contract.Difficulty;
import com.exscudo.eon.peer.contract.TransportableBlock;
import com.exscudo.eon.peer.data.Block;
import com.exscudo.eon.peer.data.DatastoreConnector;
import com.exscudo.eon.peer.data.DatastoreConnector.BlockLinkedList;
import com.exscudo.eon.peer.data.UnitOfWork;
import com.exscudo.eon.peer.exceptions.LifecycleException;
import com.exscudo.eon.peer.exceptions.ProtocolException;
import com.exscudo.eon.peer.exceptions.RemotePeerException;
import com.exscudo.eon.peer.exceptions.UnknownObjectException;
import com.exscudo.eon.peer.exceptions.ValidateException;
import com.exscudo.eon.utils.Format;
import com.exscudo.eon.utils.Loggers;

/**
 * Performs the task of synchronizing the chain of the current node and random
 * node.
 *
 */
public final class SyncBlockListTask extends AbstractTask implements Runnable {
	private final DatastoreConnector target;

	public SyncBlockListTask(ExecutionContext context, DatastoreConnector target) {
		super(context);

		this.target = target;
	}

	@Override
	public void run() {

		try {

			final Peer peer = context.getAnyConnectedPeer();
			if (peer != null) {
				try {

					DataSynchronizationService stub = context.createProxy(peer, DataSynchronizationService.class);
					Difficulty remoteState = stub.getDifficulty();
					Difficulty currentState = new Difficulty(target.blocks().getLastBlock());

					if (remoteState.getDifficulty().compareTo(currentState.getDifficulty()) == 0) {

						// The chain of the blocks is synchronized with at
						// least one node.
						// Generating a new block...

						Loggers.VERBOSE.trace(SyncBlockListTask.class,
								"The chain of the blocks is synchronized with \"{}\".", peer.getAnnouncedAddress());
						((AsyncState) context.state()).creator.start();

						return;

					} else if (remoteState.getDifficulty().compareTo(currentState.getDifficulty()) > 0) {

						// Start the synchronization process, if the
						// "difficulty" of the last block of the random node
						// more than the "difficulty" of our last block.

						Loggers.VERBOSE.info(SyncBlockListTask.class,
								"Begining synchronization. Difficulty: [this] {}, [{}] {}. ",
								currentState.getDifficulty(), peer.getAnnouncedAddress(), remoteState.getDifficulty());

						final BlockLinkedList blockList = target.blocks();

						Long[] lastBlockIDs = blockList.getLatestBlocks(Constant.SYNC_SHORT_FRAME);
						TransportableBlock[] items = stub.getLastBlocks(encodeBlockIDs(lastBlockIDs));
						if (items.length == 0) {

							if (currentState.getLastBlockID() != blockList.getLastBlockID()) {
								Loggers.VERBOSE.info(SyncBlockListTask.class,
										"New last block in sync with " + peer.getAnnouncedAddress());
								return;
							}

							// Common blocks was not found... Requests
							// blocks that have been added since the last
							// milestone.

							Loggers.VERBOSE.warning(SyncBlockListTask.class, "Sync over a latest milestone.");
							lastBlockIDs = blockList.getLatestBlocks(Constant.SYNC_LONG_FRAME);
							items = stub.getLastBlocks(encodeBlockIDs(lastBlockIDs));

						}

						// ATTENTION. The node will be out of network, if the
						// division happened at the depth is greater than
						// the SYNC_SHORT_FRAME.
						if (items.length != 0 && currentState.getLastBlockID() == blockList.getLastBlockID()) {

							Map<Long, TransportableBlock> futureBlocks = new HashMap<Long, TransportableBlock>();
							long commonBlockID = 0;
							int commonBlockHeight = -1;
							for (TransportableBlock item : items) {

								Block block = item.block;
								if (blockList.getBlockHeight(block.getID()) == -1) {

									final long id = block.getPreviousBlock();
									final int prevBlockHeight = blockList.getBlockHeight(id);
									if (prevBlockHeight != -1 && prevBlockHeight >= commonBlockHeight) {

										commonBlockID = id;
										commonBlockHeight = prevBlockHeight;
									}
									futureBlocks.put(id, item);

								}
							}

							if (commonBlockID == 0) {

								Loggers.VERBOSE.warning(SyncBlockListTask.class,
										"Unable to get common block in sync with '{}'", peer.getAnnouncedAddress());
								return;

							}

							Difficulty remoteStateNew = stub.getDifficulty();

							synchronized (target.blocks().syncObject()) {
								Block lastBlock = blockList.getLastBlock();

								Difficulty currentStateNew = new Difficulty(lastBlock);
								if (!remoteState.equals(remoteStateNew) || !currentState.equals(currentStateNew)) {

									Loggers.VERBOSE.info(SyncBlockListTask.class, "New state in sync with {}.",
											peer.getAnnouncedAddress());
									return;

								}

								// There are blocks after the general block
								if (futureBlocks.isEmpty()) {
									return;
								}

								Loggers.VERBOSE.trace(SyncBlockListTask.class,
										"Synch with {}. Last block: [{}]{}. Common block: [{}]{}.",
										peer.getAnnouncedAddress(), lastBlock.getHeight(),
										Format.BlockIdEncode(currentState.getLastBlockID()), commonBlockHeight,
										Format.BlockIdEncode(commonBlockID));

								synchronized (target.transactions().syncObject()) {

									UnitOfWork uow = target.createUnitOfWork(SyncBlockListTask.class.getName());
									try {

										try {

											target.popTo(commonBlockID);
											long newBlockID = blockList.getLastBlockID();
											if (newBlockID != commonBlockID) {

												throw new IllegalStateException("Unexpected block. Expected - "
														+ Format.BlockIdEncode(commonBlockID) + ", current - "
														+ Format.BlockIdEncode(newBlockID));

											}

											// Loading
											// sequence of
											// blocks...
											while (futureBlocks.containsKey(newBlockID)) {

												TransportableBlock tb = futureBlocks.get(commonBlockID);

												Loggers.VERBOSE.info(SyncBlockListTask.class,
														"Block pushing... [{}] {} -> {}",
														blockList.getBlockHeight(tb.block.getPreviousBlock()),
														Format.BlockIdEncode(tb.block.getPreviousBlock()),
														Format.BlockIdEncode(tb.block.getID()));

												if (tb.block
														.isFuture(TimeProvider.getEpochTime() + Constant.MAX_LATENCY)) {

													// TODO: change to ProtocolException()
													throw new LifecycleException(
															Format.BlockIdEncode(tb.block.getID()));

												}

												target.importBlock(tb.block, tb.transactions);

												Loggers.VERBOSE.info(SyncBlockListTask.class, "Block pushed: [{}] {}",
														blockList.getBlockHeight(tb.block.getPreviousBlock()) + 1,
														Format.BlockIdEncode(tb.block.getID()));

												newBlockID = blockList.getLastBlockID();

											}

										} catch (UnknownObjectException | LifecycleException
												| IllegalStateException ignore) {

											Loggers.VERBOSE.error(SyncBlockListTask.class, ignore);

										} catch (ValidateException e) {

											throw new ProtocolException(e);
										}

										lastBlock = blockList.getLastBlock();
										Difficulty state = new Difficulty(lastBlock);

										// If node have imported all the blocks,
										// the difficulty should match
										if (state.getLastBlockID() == remoteStateNew.getLastBlockID()
												&& !state.getDifficulty().equals(remoteStateNew.getDifficulty())) {

											throw new ProtocolException(
													"Failed to sync with '" + peer.getAnnouncedAddress()
															+ "'. The Difficulty of the latest block is not valid.");

										}

										// If node have imported a part of the
										// sequence
										if (state.getDifficulty().compareTo(currentState.getDifficulty()) > 0) {

											Loggers.VERBOSE.info(SyncBlockListTask.class,
													"Sync with '{}' complete. [{}]{} -> [{}]{}",
													peer.getAnnouncedAddress(), commonBlockHeight, commonBlockID,
													lastBlock.getHeight(), state.getLastBlockID());

										} else {

											// There were problems with the
											// addition of the block. The node
											// is placed in the black list.

											throw new ProtocolException(
													"Failed to sync with '" + peer.getAnnouncedAddress() + "'. Before: "
															+ currentState.getDifficulty() + ", after: "
															+ state.getDifficulty());
										}

										uow.apply();
									} catch (ProtocolException e) {
										uow.restore();

										peer.setBlacklistingTime(System.currentTimeMillis());
										throw e;

									} catch (Exception e) {
										Loggers.VERBOSE.debug(SyncBlockListTask.class,
												"The node is disconnected. \"{}\".", peer.getAnnouncedAddress());

										uow.restore();
									}

								}

							}

						} else {
							Loggers.VERBOSE.info(SyncBlockListTask.class,
									"Synchronization with '{}' is not complete. Perhaps the state was changed.",
									peer.getAnnouncedAddress());
						}
					}

				} catch (RemotePeerException | IOException e) {

					synchronized (peer) {
						if (peer.getState() == State.STATE_AMBIGUOUS) {
							peer.setBlacklistingTime(System.currentTimeMillis());
						} else {
							peer.setState(State.STATE_DISCONNECTED);
						}
					}

					Loggers.STREAM.trace(SyncBlockListTask.class,
							">> [" + peer.getAnnouncedAddress() + "] Failed to execute a request.", e);
					Loggers.VERBOSE.debug(SyncBlockListTask.class, "The node is disconnected. \"{}\".",
							peer.getAnnouncedAddress());

					return;
				}

			}

		} catch (Exception e) {
			Loggers.NOTICE.error(SyncBlockListTask.class, e);
		}

	}

	private String[] encodeBlockIDs(Long[] ids) {

		String[] encoded = new String[ids.length];
		for (int i = 0; i < ids.length; i++) {
			encoded[i] = Format.BlockIdEncode(ids[i]);
		}
		return encoded;

	}

}