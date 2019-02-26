package org.eontechology.and.peer.core.importer;

import java.math.BigInteger;
import java.util.Arrays;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.IFork;
import org.eontechology.and.peer.core.blockchain.Blockchain;
import org.eontechology.and.peer.core.blockchain.BlockchainProvider;
import org.eontechology.and.peer.core.blockchain.ITransactionMapper;
import org.eontechology.and.peer.core.common.IAccountHelper;
import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.common.ITransactionEstimator;
import org.eontechology.and.peer.core.common.ImmutableTimeProvider;
import org.eontechology.and.peer.core.common.TransactionComparator;
import org.eontechology.and.peer.core.common.exceptions.IllegalSignatureException;
import org.eontechology.and.peer.core.common.exceptions.LifecycleException;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Generation;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.ledger.LedgerProvider;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.core.middleware.LedgerActionContext;
import org.eontechology.and.peer.core.middleware.TransactionValidator;
import org.eontechology.and.peer.core.middleware.TransactionValidatorFabric;
import org.eontechology.and.peer.core.middleware.ValidationResult;

/**
 * Basic implementation of {@code IUnitOfWork} with direct connection to DB.
 * <p>
 *
 * @see IUnitOfWork
 */
public class UnitOfWork implements IUnitOfWork {

    private final BlockchainProvider blockchainProvider;
    private final LedgerProvider ledgerProvider;
    private final IFork fork;
    private final TransactionValidatorFabric transactionValidatorFabric;
    private final ITransactionEstimator estimator;
    private final IAccountHelper accountHelper;
    private final ITransactionMapper transactionMapper;

    private Blockchain currentBlockchain;

    public UnitOfWork(BlockchainProvider blockchainProvider,
                      LedgerProvider ledgerProvider,
                      IFork fork,
                      Block block,
                      TransactionValidatorFabric transactionValidatorFabric,
                      ITransactionEstimator estimator,
                      IAccountHelper accountHelper,
                      ITransactionMapper mapper) {
        this.blockchainProvider = blockchainProvider;
        this.ledgerProvider = ledgerProvider;
        this.fork = fork;
        this.transactionValidatorFabric = transactionValidatorFabric;
        this.estimator = estimator;
        this.accountHelper = accountHelper;
        this.transactionMapper = mapper;
        this.currentBlockchain = blockchainProvider.getBlockchain(block);
    }

    @Override
    public Block pushBlock(Block newBlock) throws ValidateException {

        Block headBlock = currentBlockchain.getLastBlock();
        ILedger ledger = ledgerProvider.getLedger(headBlock);
        if (ledger == null) {
            throw new ValidateException("Unable to get ledger for block. " + headBlock.getID());
        }

        // version
        int version = fork.getBlockVersion(newBlock.getTimestamp());
        if (version != newBlock.getVersion()) {
            throw new ValidateException("Unsupported block version.");
        }
        ledger = fork.convert(ledger, newBlock.getTimestamp());

        ValidationResult r;

        // validate generator
        Account generator = ledger.getAccount(newBlock.getSenderID());
        if (!accountHelper.validateGenerator(generator, newBlock.getTimestamp())) {
            throw new ValidateException("Invalid generator");
        }

        BigInteger diff = accountHelper.getDifficultyAddition(newBlock, generator, newBlock.getTimestamp());
        BigInteger cumulativeDifficulty = headBlock.getCumulativeDifficulty().add(diff);
        newBlock.setCumulativeDifficulty(cumulativeDifficulty);

        // validate block
        r = validateBlock(newBlock, ledger, headBlock);
        if (r.hasError) {
            throw r.cause;
        }

        // apply
        ledger = applyBlock(newBlock, headBlock, ledger, fork);

        // validate state
        r = validateState(newBlock, ledger);
        if (r.hasError) {
            throw r.cause;
        }

        ledgerProvider.addLedger(ledger);
        transactionMapper.map(newBlock);
        return currentBlockchain.addBlock(newBlock);
    }

    @Override
    public Block commit() {
        if (currentBlockchain != null) {
            return blockchainProvider.setBlockchain(currentBlockchain);
        }
        return null;
    }

    private ValidationResult validateBlock(Block newBlock, ILedger ledger, Block targetBlock) {

        // timestamp
        int timestamp = targetBlock.getTimestamp() + Constant.BLOCK_PERIOD;
        if (timestamp != newBlock.getTimestamp()) {
            return ValidationResult.error(new LifecycleException());
        }

        // previous block
        if (newBlock.getPreviousBlock() == null) {
            return ValidationResult.error("Previous block is not specified.");
        }

        if (!targetBlock.getID().equals(newBlock.getPreviousBlock())) {
            return ValidationResult.error("Unexpected block. Expected - " +
                                                  newBlock.getPreviousBlock() +
                                                  ", current - " +
                                                  targetBlock.getID());
        }

        if (ledger == null) {
            return ValidationResult.error("Unable to get ledger for block. " + targetBlock.getID());
        }

        Account generator = ledger.getAccount(newBlock.getSenderID());
        if (generator == null) {
            return ValidationResult.error("Invalid generator. " + newBlock.getSenderID());
        }

        // generation signature
        int height = targetBlock.getHeight() + 1;
        Block generationBlock = targetBlock;
        if (height - Constant.DIFFICULTY_DELAY > 0) {
            int generationHeight = height - Constant.DIFFICULTY_DELAY;
            // ATTENTION. in case EonConstant.DIFFICULTY_DELAY < SYNC_MILESTONE_DEPTH it is
            // necessary to revise

            generationBlock = currentBlockchain.getByHeight(generationHeight);
        }

        if (!accountHelper.verifySignature(new Generation(generationBlock.getGenerationSignature()),
                                           newBlock.getGenerationSignature(),
                                           generator,
                                           newBlock.getTimestamp())) {
            return ValidationResult.error(new IllegalSignatureException("The field Generation Signature is incorrect."));
        }

        // signature
        if (!accountHelper.verifySignature(newBlock, newBlock.getSignature(), generator, newBlock.getTimestamp())) {
            return ValidationResult.error(new IllegalSignatureException());
        }

        if (newBlock.getTransactions() != null) {
            long payloadLength = 0;
            for (Transaction tr : newBlock.getTransactions()) {
                payloadLength += tr.getLength();
            }

            if (payloadLength > Constant.BLOCK_MAX_PAYLOAD_LENGTH) {
                return ValidationResult.error(new IllegalSignatureException("Block size is incorrect."));
            }
        }

        // adds the data that is not transmitted over the network
        newBlock.setHeight(targetBlock.getHeight() + 1);

        return ValidationResult.success;
    }

    private ValidationResult validateState(Block newBlock, ILedger ledger) {

        String snapshot = ledger.getHash();
        if (snapshot == null || !snapshot.equals(newBlock.getSnapshot())) {
            return ValidationResult.error("Illegal snapshot prefix.");
        }

        return ValidationResult.success;
    }

    private ILedger applyBlock(Block newBlock, Block prevBlock, ILedger ledger, IFork fork) throws ValidateException {

        ITimeProvider blockTimeProvider = new ImmutableTimeProvider(newBlock.getTimestamp());
        TransactionValidator transactionValidator = transactionValidatorFabric.getAllValidators(blockTimeProvider);

        ILedger newLedger = ledger;

        LedgerActionContext ctx = new LedgerActionContext(newBlock.getTimestamp());
        Transaction[] sortedTransactions = newBlock.getTransactions().toArray(new Transaction[0]);

        long totalFee = 0;
        for (Transaction tx : sortedTransactions) {
            int difficulty = estimator.estimate(tx);
            tx.setLength(difficulty);
            totalFee += tx.getFee();
        }
        Arrays.sort(sortedTransactions, new TransactionComparator());

        ITransactionParser parser = fork.getParser(newBlock.getTimestamp());
        for (Transaction tx : sortedTransactions) {

            if (transactionMapper.getBlockID(tx, currentBlockchain) != null) {
                throw new ValidateException("Transaction (or any nested) already exist in blockchain.");
            }
            if (tx.hasNestedTransactions()) {
                for (Transaction nestedTx : tx.getNestedTransactions().values()) {
                    if (transactionMapper.getBlockID(nestedTx, currentBlockchain) != null) {
                        throw new ValidateException("Nested transaction already exist in blockchain.");
                    }
                }
            }

            ValidationResult r = transactionValidator.validate(tx, newLedger);
            if (r.hasError) {
                throw r.cause;
            }

            ILedgerAction[] actions = parser.parse(tx);

            for (ILedgerAction action : actions) {
                newLedger = action.run(newLedger, ctx);
            }
        }

        if (totalFee != 0) {
            Account creator = newLedger.getAccount(newBlock.getSenderID());
            creator = accountHelper.reward(creator, totalFee, newBlock.getTimestamp());
            newLedger = newLedger.putAccount(creator);
        }

        return newLedger;
    }
}
