package com.exscudo.peer.core.importer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.blockchain.Blockchain;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.TransactionComparator;
import com.exscudo.peer.core.common.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.crypto.BencodeFormatter;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.Constants;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;

/**
 * Basic implementation of {@code IUnitOfWork} with direct connection to DB.
 * <p>
 *
 * @see IUnitOfWork
 */
public class UnitOfWork implements IUnitOfWork {

    private final BlockchainProvider blockchainProvider;
    private final LedgerProvider ledgerProvider;
    private IFork fork;

    private Blockchain currentBlockchain;

    public UnitOfWork(BlockchainProvider blockchainProvider, LedgerProvider ledgerProvider, IFork fork, Block block) {
        this.blockchainProvider = blockchainProvider;
        this.ledgerProvider = ledgerProvider;
        this.fork = fork;
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
        ledger = fork.covert(ledger, newBlock.getTimestamp());

        ValidationResult r;

        // validate generator
        Account generator = ledger.getAccount(newBlock.getSenderID());
        if (!fork.validateGenerator(generator, newBlock.getTimestamp())) {
            throw new ValidateException("Invalid generator");
        }

        BigInteger diff = fork.getDifficultyAddition(newBlock, generator, newBlock.getTimestamp());
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

        headBlock = currentBlockchain.addBlock(newBlock);
        ledgerProvider.addLedger(ledger);

        return headBlock;
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

        byte[] publicKey = fork.getPublicKey(generator, newBlock.getTimestamp());
        // generation signature
        int height = targetBlock.getHeight() + 1;
        Block generationBlock = targetBlock;
        if (height - Constant.DIFFICULTY_DELAY > 0) {
            int generationHeight = height - Constant.DIFFICULTY_DELAY;
            // ATTENTION. in case EonConstant.DIFFICULTY_DELAY < SYNC_MILESTONE_DEPTH it is
            // necessary to revise

            generationBlock = currentBlockchain.getByHeight(generationHeight);
        }

        Map<String, Object> gEds = new TreeMap<>();
        gEds.put("network", fork.getGenesisBlockID().toString());
        gEds.put(Constants.GENERATION_SIGNATURE, Format.convert(generationBlock.getGenerationSignature()));
        byte[] gEdsBytes = BencodeFormatter.getBytes(gEds);

        if (!CryptoProvider.getInstance().verifySignature(gEdsBytes, newBlock.getGenerationSignature(), publicKey)) {
            return ValidationResult.error(new IllegalSignatureException("The field Generation Signature is incorrect."));
        }

        // signature
        if (!newBlock.verifySignature(publicKey)) {
            return ValidationResult.error(new IllegalSignatureException());
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

        ILedger newLedger = ledger;

        ITransactionHandler handler = fork.getTransactionExecutor(newBlock.getTimestamp());
        TransactionContext ctx = new TransactionContext(newBlock.getTimestamp());
        Transaction[] sortedTransactions = newBlock.getTransactions().toArray(new Transaction[0]);
        Arrays.sort(sortedTransactions, new TransactionComparator());
        for (Transaction tx : sortedTransactions) {
            if (currentBlockchain.containsTransaction(tx)) {
                throw new ValidateException("Transaction already exist in blockchain.");
            }
            if (tx.isFuture(newBlock.getTimestamp())) {
                throw new LifecycleException();
            }
            newLedger = handler.run(tx, newLedger, ctx);
        }

        long totalFee = 0;
        for (Transaction tx : sortedTransactions) {
            totalFee += tx.getFee();
        }

        if (totalFee != 0) {
            Account creator = newLedger.getAccount(newBlock.getSenderID());
            long balance = fork.getBalance(creator, newBlock.getTimestamp());
            balance += totalFee;
            creator = fork.setBalance(creator, balance, newBlock.getTimestamp());
            newLedger = newLedger.putAccount(creator);
        }

        return newLedger;
    }
}
