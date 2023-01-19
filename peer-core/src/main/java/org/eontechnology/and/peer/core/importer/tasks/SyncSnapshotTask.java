package org.eontechnology.and.peer.core.importer.tasks;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.api.ISnapshotSynchronizationService;
import org.eontechnology.and.peer.core.blockchain.Blockchain;
import org.eontechnology.and.peer.core.blockchain.BlockchainProvider;
import org.eontechnology.and.peer.core.common.IAccountHelper;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.env.Peer;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.eontechnology.and.peer.core.storage.Storage;

/**
 * Performs the task of initial synchronization from a snapshot.
 *
 * <p>In the first step, the states of the accounts is loaded. The states correspond to the block of
 * day-old. Next, the subsequent blocks are loaded
 */
public class SyncSnapshotTask implements Runnable {

  private final Storage storage;
  private final ExecutionContext context;
  private final IFork fork;
  private final BlockchainProvider blockchainProvider;
  private final LedgerProvider ledgerProvider;
  private final IAccountHelper accountHelper;
  private boolean disabled = false;

  public SyncSnapshotTask(
      Storage storage,
      ExecutionContext context,
      IFork fork,
      BlockchainProvider blockchainProvider,
      LedgerProvider ledgerProvider,
      IAccountHelper accountHelper) {

    this.storage = storage;
    this.context = context;
    this.fork = fork;
    this.blockchainProvider = blockchainProvider;
    this.ledgerProvider = ledgerProvider;
    this.accountHelper = accountHelper;
  }

  @Override
  public void run() {

    if (disabled) {
      Loggers.info(SyncSnapshotTask.class, "Snapshot exist, disabling task...");
      throw new RuntimeException("Disabling SyncSnapshotTask");
    }

    Peer peer = null;
    try {

      if (storage.metadata().getHistoryFromHeight() >= 0) {
        disabled = true;
        return;
      }

      peer = context.getAnyConnectedPeer();
      if (peer == null) {
        return;
      }

      ISnapshotSynchronizationService service = peer.getSnapshotSynchronizationService();

      Block lastBlock = service.getLastBlock();

      int targetHeight =
          fork.getTargetBlockHeight(lastBlock.getTimestamp() - Constant.SECONDS_IN_DAY);
      if (targetHeight <= 0) {
        Loggers.info(SyncSnapshotTask.class, "Short history, load full blockchain");
        storage.metadata().setHistoryFromHeight(0);
        disabled = true;
        return;
      }

      final Block block = service.getBlockByHeight(targetHeight);
      if (block == null) {
        throw new RemotePeerException("Empty history on remote peer");
      }
      if (targetHeight != fork.getTargetBlockHeight(block.getTimestamp())) {
        throw new RemotePeerException("Unexpected block");
      }
      block.setHeight(targetHeight);

      Loggers.info(
          SyncSnapshotTask.class, "Loaded start block: [{}]{}", block.getHeight(), block.getID());

      ledgerProvider.truncate(block.getTimestamp());
      ILedger ledger =
          ledgerProvider.getLedger(null, fork.getNextBlockTimestamp(block.getTimestamp()));

      Account[] accounts = service.getAccounts(block.getID().toString());

      Set<String> totalAccSet = new HashSet<>();

      Loggers.info(SyncSnapshotTask.class, "Loading accounts...");

      while (accounts != null) {
        boolean imported = false;
        BigInteger maxAcc = BigInteger.ZERO;

        for (Account account : accounts) {

          if (totalAccSet.contains(account.getID().toString())) {
            continue;
          }
          ledger = ledger.putAccount(account);

          BigInteger reversed =
              new BigInteger(Long.toHexString(Long.reverse(account.getID().getValue())), 16);
          maxAcc = maxAcc.max(reversed);

          totalAccSet.add(account.getID().toString());
          imported = true;
        }

        if (imported) {

          try {
            ledgerProvider.addLedger(ledger);
          } catch (Throwable tx) {
            Loggers.error(SyncSnapshotTask.class, tx);
          }

          ledger =
              ledgerProvider.getLedger(
                  ledger.getHash(), fork.getNextBlockTimestamp(block.getTimestamp()));

          accounts =
              service.getNextAccounts(
                  block.getID().toString(),
                  new AccountID(Long.reverse(maxAcc.longValue())).toString());

          Loggers.info(SyncSnapshotTask.class, "Loaded accounts: {}", totalAccSet.size());
        } else {
          accounts = null;
        }
      }

      Loggers.info(SyncSnapshotTask.class, "All accounts loaded: {}", totalAccSet.size());

      if (!block.getSnapshot().equals(ledger.getHash())) {
        throw new RemotePeerException("Incorrect snapshot");
      }

      Loggers.info(SyncSnapshotTask.class, "Loading block heads for day...");
      // Load history for day before
      Map<BlockID, Block> blockSet = new LinkedHashMap<>();

      int h = fork.getTargetBlockHeight(lastBlock.getTimestamp() - 2 * Constant.SECONDS_IN_DAY);
      h = Math.max(h, 0);

      while (h < targetHeight - 1) {

        Block[] head = service.getBlocksHeadFrom(h + 1);

        for (Block b : head) {

          int height = fork.getTargetBlockHeight(b.getTimestamp());
          if (height < targetHeight) {
            b.setHeight(height);
            blockSet.put(b.getID(), b);
          }

          h = Math.max(h, height);
        }
      }

      // Check EDS
      blockSet.put(block.getID(), block);

      Loggers.info(SyncSnapshotTask.class, "Validating signatures...");

      Account generator = ledger.getAccount(block.getSenderID());

      if (!accountHelper.verifySignature(
          block, block.getSignature(), generator, block.getTimestamp())) {
        throw new RemotePeerException("Bad block signature");
      }

      if (block.getTransactions() != null) {
        for (Transaction tx : block.getTransactions()) {
          Account account = ledger.getAccount(tx.getSenderID());

          if (!accountHelper.verifySignature(
              tx, tx.getSignature(), account, block.getTimestamp())) {
            throw new RemotePeerException("Bad transaction signature");
          }
        }
      }

      Loggers.info(SyncSnapshotTask.class, "Saving...");

      Blockchain blockchain = blockchainProvider.createBlockchain();
      Set<Block> set =
          new TreeSet<>(
              new Comparator<Block>() {
                @Override
                public int compare(Block o1, Block o2) {
                  return Integer.compare(o1.getHeight(), o2.getHeight());
                }
              });
      set.addAll(blockSet.values());
      for (Block b : set) {
        blockchain.addBlock(b);
      }
      blockchainProvider.initialize(
          blockchain, storage.metadata().getGenesisBlockID(), block.getHeight());

      Loggers.info(SyncSnapshotTask.class, "Snapshot downloaded!");
      disabled = true;
    } catch (RemotePeerException th) {
      Loggers.error(SyncSnapshotTask.class, "Failed to sync with '" + peer + "'", th);
      Loggers.debug(SyncSnapshotTask.class, "The node is disconnected. \"{}\".", peer);

      context.blacklistPeer(peer);
      Loggers.error(SyncSnapshotTask.class, th);
    } catch (Throwable th) {
      Loggers.error(SyncSnapshotTask.class, "Failed to sync with '" + peer + "'", th);
      Loggers.error(SyncSnapshotTask.class, th);
    }
  }
}
