package com.exscudo.peer.core.importer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.blockchain.events.BlockEvent;
import com.exscudo.peer.core.blockchain.events.IBlockEventListener;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.TransactionComparator;
import com.exscudo.peer.core.crypto.BencodeFormatter;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.data.mapper.Constants;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.env.events.IPeerEventListener;
import com.exscudo.peer.core.env.events.PeerEvent;
import com.exscudo.peer.core.importer.tasks.GenerateBlockTask;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;

/**
 * Creates the next block for the chain.
 */
public class BlockGenerator implements IPeerEventListener, IBlockEventListener {

    private final AtomicBoolean isGenAllowed = new AtomicBoolean();
    private final IBacklog backlog;
    private final IBlockchainProvider blockchain;
    private final IFork fork;
    private final LedgerProvider ledgerProvider;
    private boolean useFastGeneration = false;
    private ISigner signer;

    public BlockGenerator(IFork fork,
                          ISigner signer,
                          IBacklog backlog,
                          IBlockchainProvider blockchain,
                          LedgerProvider ledgerProvider) {
        this.backlog = backlog;
        this.blockchain = blockchain;
        this.signer = signer;
        this.fork = fork;
        this.ledgerProvider = ledgerProvider;
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
            if (tx != null && !tx.isFuture(currentTimestamp)) {
                map.put(id, tx);
            }
        }

        Block parallelBlock = blockchain.getBlockByHeight(previousBlock.getHeight() + 1);
        while (parallelBlock != null) {
            for (Transaction tx : parallelBlock.getTransactions()) {

                if (!tx.isFuture(currentTimestamp)) {
                    map.put(tx.getID(), tx);
                }
            }
            parallelBlock = blockchain.getBlockByHeight(parallelBlock.getHeight() + 1);
        }

        return createBlock(previousBlock, map.values().toArray(new Transaction[0]), fork);
    }

    private Block createBlock(Block previousBlock, Transaction[] transactions, IFork fork) {

        int timestamp = previousBlock.getTimestamp() + Constant.BLOCK_PERIOD;
        AccountID senderID = new AccountID(getSigner().getPublicKey());
        int height = previousBlock.getHeight() + 1;

        ILedger ledger = ledgerProvider.getLedger(previousBlock);
        ledger = fork.covert(ledger, timestamp);
        Account generator = ledger.getAccount(senderID);

        // validate generator
        if (!fork.validateGenerator(generator, timestamp)) {
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

        Map<String, Object> gEds = new TreeMap<>();
        gEds.put("network", fork.getGenesisBlockID().toString());
        gEds.put(Constants.GENERATION_SIGNATURE, Format.convert(targetBlock.getGenerationSignature()));
        byte[] gEdsBytes = BencodeFormatter.getBytes(gEds);
        byte[] generationSignature = getSigner().sign(gEdsBytes);

        ITransactionHandler handler = fork.getTransactionExecutor(timestamp);
        TransactionContext ctx = new TransactionContext(timestamp);

        Arrays.sort(transactions, new TransactionComparator());
        int payloadLength = 0;
        List<Transaction> payload = new ArrayList<>(transactions.length);
        for (Transaction tx : transactions) {
            int txLength = tx.getLength();
            if (payloadLength + txLength > Constant.BLOCK_MAX_PAYLOAD_LENGTH) {
                break;
            }

            try {
                ledger = handler.run(tx, ledger, ctx);
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
            long balance = fork.getBalance(creator, timestamp);
            balance += totalFee;
            creator = fork.setBalance(creator, balance, timestamp);
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
        newBlock.setSignature(getSigner().sign(newBlock.getBytes()));

        BigInteger diff = fork.getDifficultyAddition(newBlock, generator, timestamp);
        BigInteger cumulativeDifficulty = previousBlock.getCumulativeDifficulty().add(diff);
        newBlock.setCumulativeDifficulty(cumulativeDifficulty);

        return newBlock;
    }

    /* IBlockEventListener members */

    @Override
    public void onBeforeChanging(BlockEvent event) {
        isGenAllowed.set(false);
    }

    @Override
    public void onLastBlockChanged(BlockEvent event) {

        if (useFastGeneration) {
            isGenAllowed.set(true);
        }
    }

    /* IPeerEventListener */

    @Override
    public void onSynchronized(PeerEvent event) {
        // The blocks is synchronized with at least one env.
        isGenAllowed.set(true);
    }

    public boolean isGenerationAllowed() {
        return isGenAllowed.get();
    }

    public void allowGenerate() {
        isGenAllowed.set(true);
    }
}
