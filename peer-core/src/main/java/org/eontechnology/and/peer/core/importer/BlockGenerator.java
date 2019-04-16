package org.eontechnology.and.peer.core.importer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.backlog.IBacklog;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.blockchain.events.BlockchainEvent;
import org.eontechnology.and.peer.core.blockchain.events.IBlockchainEventListener;
import org.eontechnology.and.peer.core.blockchain.events.UpdatedBlockchainEvent;
import org.eontechnology.and.peer.core.common.IAccountHelper;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.ITransactionEstimator;
import org.eontechnology.and.peer.core.common.ImmutableTimeProvider;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.common.TransactionComparator;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Generation;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.env.events.IPeerEventListener;
import org.eontechnology.and.peer.core.env.events.PeerEvent;
import org.eontechnology.and.peer.core.importer.tasks.GenerateBlockTask;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.core.middleware.TransactionValidator;
import org.eontechnology.and.peer.core.middleware.TransactionValidatorFabric;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

/**
 * Creates the next block for the chain.
 */
public class BlockGenerator implements IPeerEventListener, IBlockchainEventListener {

    private final AtomicBoolean isGenAllowed = new AtomicBoolean();
    private final IBacklog backlog;
    private final IBlockchainProvider blockchain;
    private final IFork fork;
    private final LedgerProvider ledgerProvider;
    private final TransactionValidatorFabric transactionValidatorFabric;
    private final ITransactionEstimator estimator;
    private final IAccountHelper accountHelper;
    private boolean useFastGeneration = false;
    private ISigner signer;

    public BlockGenerator(IFork fork,
                          ISigner signer,
                          IBacklog backlog,
                          IBlockchainProvider blockchain,
                          LedgerProvider ledgerProvider,
                          TransactionValidatorFabric transactionValidatorFabric,
                          ITransactionEstimator estimator,
                          IAccountHelper accountHelper) {
        this.backlog = backlog;
        this.blockchain = blockchain;
        this.signer = signer;
        this.fork = fork;
        this.ledgerProvider = ledgerProvider;
        this.transactionValidatorFabric = transactionValidatorFabric;
        this.estimator = estimator;
        this.accountHelper = accountHelper;
    }

    public boolean isInitialized() {
        return signer != null;
    }

    public ISigner getSigner() {
        return signer;
    }

    public void setSigner(ISigner signer) {
        this.signer = signer;
    }

    public boolean isUseFastGeneration() {
        return useFastGeneration;
    }

    public void setUseFastGeneration(boolean useFastGeneration) {
        this.useFastGeneration = useFastGeneration;
    }

    public Block createNextBlock(Block previousBlock) {

        if (!isGenAllowed.get()) {
            return null;
        }

        isGenAllowed.set(false);

        int currentTimestamp = previousBlock.getTimestamp() + Constant.BLOCK_PERIOD;

        Map<TransactionID, Transaction> map = new HashMap<>();
        Iterator<TransactionID> indexes = backlog.iterator();
        while (indexes.hasNext() && map.size() < Constant.BLOCK_TRANSACTION_LIMIT) {
            TransactionID id = indexes.next();
            Transaction tx = backlog.get(id);
            if (tx != null && !tx.isFuture(currentTimestamp) && !tx.isExpired(currentTimestamp)) {
                map.put(id, tx);
            }
        }

        Block parallelBlock = blockchain.getBlockByHeight(previousBlock.getHeight() + 1);
        while (parallelBlock != null) {
            for (Transaction tx : parallelBlock.getTransactions()) {

                if (!tx.isFuture(currentTimestamp) && !tx.isExpired(currentTimestamp)) {
                    int difficulty = estimator.estimate(tx);
                    tx.setLength(difficulty);
                    map.put(tx.getID(), tx);
                }
            }
            parallelBlock = blockchain.getBlockByHeight(parallelBlock.getHeight() + 1);
        }

        return createBlock(previousBlock, map.values().toArray(new Transaction[0]));
    }

    private Block createBlock(Block previousBlock, Transaction[] transactions) {

        int timestamp = previousBlock.getTimestamp() + Constant.BLOCK_PERIOD;
        AccountID senderID = new AccountID(getSigner().getPublicKey());
        int height = previousBlock.getHeight() + 1;

        ILedger ledger = ledgerProvider.getLedger(previousBlock);
        ledger = fork.convert(ledger, timestamp);
        Account generator = ledger.getAccount(senderID);

        // validate generator
        if (!accountHelper.validateGenerator(generator, timestamp)) {
            Loggers.warning(GenerateBlockTask.class, "Invalid generator");
            return null;
        }

        int version = fork.getBlockVersion(timestamp);
        if (version < 0) {
            Loggers.warning(GenerateBlockTask.class, "Invalid block version.");
            return null;
        }

        Block targetBlock = previousBlock;
        if (height - Constant.DIFFICULTY_DELAY > 0) {
            int targetHeight = height - Constant.DIFFICULTY_DELAY;
            targetBlock = blockchain.getBlockByHeight(targetHeight);
        }

        Generation generation = new Generation(targetBlock.getGenerationSignature());
        byte[] generationSignature = getSigner().sign(generation, fork.getGenesisBlockID());

        LedgerActionContext ctx = new LedgerActionContext(timestamp);

        ITimeProvider blockTimeProvider = new ImmutableTimeProvider(timestamp);
        TransactionValidator transactionValidator = transactionValidatorFabric.getAllValidators(blockTimeProvider);
        ITransactionParser parser = fork.getParser(timestamp);

        Arrays.sort(transactions, new TransactionComparator());
        int payloadLength = 0;
        List<Transaction> payload = new ArrayList<>(transactions.length);
        for (Transaction tx : transactions) {
            int txLength = tx.getLength();
            if (payloadLength + txLength > Constant.BLOCK_MAX_PAYLOAD_LENGTH) {
                break;
            }

            try {

                ILedger newLedger = ledger;

                ValidationResult r = transactionValidator.validate(tx, newLedger);
                if (r.hasError) {
                    throw r.cause;
                }

                ILedgerAction[] actions = parser.parse(tx);

                for (ILedgerAction action : actions) {
                    newLedger = action.run(newLedger, ctx);
                }

                ledger = newLedger;
                payload.add(tx);
                payloadLength += txLength;
            } catch (Exception e) {
                Loggers.info(GenerateBlockTask.class,
                             "Excluding tr({}) from block generation payload: {}",
                             tx.getID(),
                             e.getMessage());
            }
        }

        long totalFee = 0;
        for (Transaction tx : payload) {
            totalFee += tx.getFee();
        }

        if (totalFee != 0) {
            Account creator = ledger.getAccount(senderID);
            creator = accountHelper.reward(creator, totalFee, timestamp);
            ledger = ledger.putAccount(creator);
        }

        Block newBlock = new Block();
        newBlock.setVersion(version);
        newBlock.setHeight(height);
        newBlock.setTimestamp(timestamp);
        newBlock.setPreviousBlock(previousBlock.getID());
        newBlock.setSenderID(senderID);
        newBlock.setGenerationSignature(generationSignature);
        newBlock.setTransactions(payload);
        newBlock.setSnapshot(ledger.getHash());
        newBlock.setSignature(getSigner().sign(newBlock, fork.getGenesisBlockID()));

        BigInteger diff = accountHelper.getDifficultyAddition(newBlock, generator, timestamp);
        BigInteger cumulativeDifficulty = previousBlock.getCumulativeDifficulty().add(diff);
        newBlock.setCumulativeDifficulty(cumulativeDifficulty);

        return newBlock;
    }

    // region IBlockchainEventListener members

    @Override
    public void onChanging(BlockchainEvent event) {
        isGenAllowed.set(false);
    }

    @Override
    public void onChanged(UpdatedBlockchainEvent event) {

        if (useFastGeneration) {
            isGenAllowed.set(true);
        }
    }

    // endregion

    // region IPeerEventListener members

    @Override
    public void onSynchronized(PeerEvent event) {
        // The blocks is synchronized with at least one env.
        isGenAllowed.set(true);
    }

    // endregion

    public boolean isGenerationAllowed() {
        return isGenAllowed.get();
    }

    public void allowGenerate() {
        isGenAllowed.set(true);
    }
}
