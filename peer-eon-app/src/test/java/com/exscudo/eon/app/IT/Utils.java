package com.exscudo.eon.app.IT;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.dampcake.bencode.Bencode;
import com.exscudo.eon.app.cfg.Fork;
import com.exscudo.eon.app.cfg.ITransactionEstimator;
import com.exscudo.eon.app.cfg.forks.Item;
import com.exscudo.eon.app.utils.TransactionEstimator;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.TransactionComparator;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.tx.TransactionType;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.mockito.Mockito;

public class Utils {
    public static final long MIN_DEPOSIT_SIZE = 500000000L;

    public static Block getLastBlock(Storage storage) throws SQLException {
        return (new BlockchainProvider(storage, null)).getLastBlock();
    }

    public static BlockID getGenesisBlockID(Storage storage) {
        return storage.metadata().getGenesisBlockID();
    }

    public static IFork createFork(Storage storage, int forkID) throws SQLException {
        long time = getLastBlock(storage).getTimestamp();
        String begin = Instant.ofEpochMilli((time - 1) * 1000).toString();
        String end = Instant.ofEpochMilli((time + 10 * 180 * 1000) * 1000).toString();

        Fork fork = new Fork(Utils.getGenesisBlockID(storage), new Item[] {new TestItem(forkID, begin)}, end);
        fork.setMinDepositSize(MIN_DEPOSIT_SIZE);
        return Mockito.spy(fork);
    }

    public static IFork createFork(Storage storage) throws SQLException {
        return createFork(storage, 1);
    }

    public static void comparePeer(PeerContext ctx1, PeerContext ctx2) throws Exception {

        Assert.assertEquals("Blockchain synchronized",
                            ctx1.blockExplorerService.getLastBlock().getID(),
                            ctx2.blockExplorerService.getLastBlock().getID());

        BlockID lastBlockID = ctx1.blockExplorerService.getLastBlock().getID();
        Bencode bencode = new Bencode();

        while (lastBlockID.getValue() != 0) {
            Block block = ctx1.blockExplorerService.getById(lastBlockID);
            Block blockNew = ctx2.blockExplorerService.getById(lastBlockID);

            Assert.assertEquals(block.getVersion(), blockNew.getVersion());
            Assert.assertEquals(block.getTimestamp(), blockNew.getTimestamp());
            Assert.assertEquals(block.getPreviousBlock(), blockNew.getPreviousBlock());
            Assert.assertEquals(Format.convert(block.getGenerationSignature()),
                                Format.convert(blockNew.getGenerationSignature()));
            Assert.assertEquals(block.getSenderID(), blockNew.getSenderID());
            Assert.assertEquals(Format.convert(block.getSignature()), Format.convert(blockNew.getSignature()));
            Assert.assertEquals(block.getID(), blockNew.getID());
            Assert.assertEquals(block.getHeight(), blockNew.getHeight());
            Assert.assertEquals(block.getCumulativeDifficulty().toString(),
                                blockNew.getCumulativeDifficulty().toString());
            Assert.assertEquals(block.getSnapshot(), blockNew.getSnapshot());

            Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
            Transaction[] transactionsNew = blockNew.getTransactions().toArray(new Transaction[0]);

            Arrays.sort(transactions, new TransactionComparator());
            Arrays.sort(transactionsNew, new TransactionComparator());

            Assert.assertEquals(transactions.length, transactionsNew.length);

            for (int i = 0; i < transactions.length; i++) {
                Transaction transaction = transactions[i];
                Transaction transactionNew = transactionsNew[i];

                Assert.assertEquals(transaction.getType(), transactionNew.getType());
                Assert.assertEquals(transaction.getTimestamp(), transactionNew.getTimestamp());
                Assert.assertEquals(transaction.getDeadline(), transactionNew.getDeadline());
                Assert.assertEquals(transaction.getReference(), transactionNew.getReference());
                Assert.assertEquals(transaction.getSenderID(), transactionNew.getSenderID());
                Assert.assertEquals(transaction.getFee(), transactionNew.getFee());
                Assert.assertEquals(Format.convert(bencode.encode(transaction.getData())),
                                    Format.convert(bencode.encode(transactionNew.getData())));
                Assert.assertEquals(Format.convert(transaction.getSignature()),
                                    Format.convert(transactionNew.getSignature()));
                Assert.assertEquals(transaction.getID(), transactionNew.getID());
                Assert.assertEquals(transaction.getLength(), transactionNew.getLength());
            }

            lastBlockID = block.getPreviousBlock();
        }
    }

    public String getGenesisBlockAsJSON(Storage storage) throws Exception {

        HashMap<String, Object> map = new HashMap<>();

        Block lastBlock = getLastBlock(storage);

        map.put("timestamp", lastBlock.getTimestamp());
        map.put("signature", Format.convert(lastBlock.getSignature()));

        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        ILedger ledger = ledgerProvider.getLedger(lastBlock);

        HashMap<String, Object> accSet = new HashMap<>();
        for (Account account : ledger) {
            HashMap<String, Object> accMap = new HashMap<>();

            for (AccountProperty property : account.getProperties()) {
                accMap.put(property.getType(), property.getData());
            }

            accSet.put(account.getID().toString(), accMap);
        }
        map.put("accounts", accSet);

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        return json;
    }

    public static class TestItem implements Item {

        private final int number;

        private long begin;
        private long end;
        private Set<Integer> transactionTypes = new HashSet<>();
        private int blockVersion;
        private ITransactionEstimator estimator;
        private CryptoProvider cryptoProvider;

        private int maxNoteLength;

        public TestItem(int number, String begin) {

            this.number = number;
            this.begin = Instant.parse(begin).toEpochMilli();

            this.blockVersion = 1;
            this.maxNoteLength = Constant.TRANSACTION_NOTE_MAX_LENGTH;

            this.transactionTypes.addAll(Lists.newArrayList(TransactionType.getTypes()));

            this.cryptoProvider = CryptoProvider.getInstance();
            this.estimator = new TransactionEstimator(this.cryptoProvider.getFormatter());
        }

        @Override
        public boolean isCome(int timestamp) {
            return timestamp * 1000L > begin;
        }

        @Override
        public boolean isPassed(int timestamp) {
            return timestamp * 1000L > end;
        }

        @Override
        public int getNumber() {
            return number;
        }

        @Override
        public int getMaxNoteLength() {
            return maxNoteLength;
        }

        public void setMaxNoteLength(int maxNoteLength) {
            this.maxNoteLength = maxNoteLength;
        }

        @Override
        public Set<Integer> getTransactionTypes() {
            return this.transactionTypes;
        }

        public void setTransactionTypes(Set<Integer> transactionTypes) {
            this.transactionTypes = transactionTypes;
        }

        @Override
        public int getBlockVersion() {
            return this.blockVersion;
        }

        public void setBlockVersion(int blockVersion) {
            this.blockVersion = blockVersion;
        }

        @Override
        public ITransactionEstimator getEstimator() {
            return estimator;
        }

        public void setEstimator(ITransactionEstimator estimator) {
            this.estimator = estimator;
        }

        @Override
        public CryptoProvider getCryptoProvider() {
            return this.cryptoProvider;
        }

        public void setCryptoProvider(CryptoProvider cryptoProvider) {
            this.cryptoProvider = cryptoProvider;
        }

        @Override
        public Account convert(Account account) {
            return account;
        }

        @Override
        public boolean needConvertAccounts() {
            return false;
        }

        @Override
        public long getBegin() {
            return begin;
        }

        @Override
        public void setBegin(long begin) {
            this.begin = begin;
        }

        @Override
        public long getEnd() {
            return end;
        }

        @Override
        public void setEnd(long end) {
            this.end = end;
        }
    }
}
