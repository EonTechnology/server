package com.exscudo.eon.app.cfg;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.exscudo.eon.app.cfg.forks.Item;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.crypto.ISignature;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;

/**
 * Basic implementation of the {@code IFork} interface.
 */
public class Fork implements IFork {
    private final LinkedList<Item> items;
    private final BlockID genesisBlockID;
    private long minDepositSize = Long.MAX_VALUE;

    public Fork(BlockID genesisBlockID, Item[] items, String endAll) {
        this.genesisBlockID = genesisBlockID;

        this.items = new LinkedList<>();
        Collections.addAll(this.items, items);

        // Sort by number desc
        this.items.sort(new ItemComparator());

        long end = Instant.parse(endAll).toEpochMilli();
        for (int i = 0; i < this.items.size(); i++) {
            Item item = this.items.get(i);
            item.setEnd(end);
            end = item.getBegin();
        }
    }

    @Override
    public BlockID getGenesisBlockID() {
        return genesisBlockID;
    }

    @Override
    public boolean isPassed(int timestamp) {
        return items.getFirst().isPassed(timestamp);
    }

    @Override
    public boolean isCome(int timestamp) {
        return items.getFirst().isCome(timestamp);
    }

    @Override
    public int getNumber(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? -1 : item.getNumber();
    }

    @Override
    public Set<Integer> getTransactionTypes(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? null : item.getTransactionTypes();
    }

    @Override
    public int getBlockVersion(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? -1 : item.getBlockVersion();
    }

    @Override
    public ILedger covert(ILedger ledger, int timestamp) {
        Item item = getItem(timestamp);

        if (item == null) {
            return ledger;
        }

        if (!item.needConvertAccounts()) {
            return ledger;
        }

        if (getNumber(timestamp - Constant.BLOCK_PERIOD) == item.getNumber()) {
            return ledger;
        }

        ILedger newLedger = ledger;
        for (Account account : ledger) {
            newLedger = newLedger.putAccount(item.convert(account));
        }
        return newLedger;
    }

    @Override
    public boolean validateGenerator(Account generator, int timestamp) {

        if (generator == null) {
            return false;
        }

        GeneratingBalanceProperty deposit = AccountProperties.getDeposit(generator);
        if (deposit.getValue() < getMinDepositSize()) {
            return false;
        }

        if (timestamp - deposit.getTimestamp() < Constant.SECONDS_IN_DAY && deposit.getTimestamp() != 0) {
            return false;
        }

        return true;
    }

    @Override
    public BigInteger getDifficultyAddition(Block block, Account generator, int timestamp) {
        byte[] generationSignatureHash;
        try {
            generationSignatureHash = MessageDigest.getInstance("SHA-512").digest(block.getGenerationSignature());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        BigInteger hit = new BigInteger(1, new byte[] {
                generationSignatureHash[7],
                generationSignatureHash[6],
                generationSignatureHash[5],
                generationSignatureHash[4],
                generationSignatureHash[3],
                generationSignatureHash[2],
                generationSignatureHash[1],
                generationSignatureHash[0]
        });

        // Decimal points in 1 EON - 1000000L
        Long scale = AccountProperties.getDeposit(generator).getValue() / 1000000L;
        if (scale != 0) {
            hit = hit.divide(BigInteger.valueOf(scale));
        }
        return Format.TWO64.divide(hit);
    }

    @Override
    public Account reward(Account account, long totalFee, int timestamp) {

        BalanceProperty balance = AccountProperties.getBalance(account);
        long oldBalance = balance.getValue();
        long newBalance = oldBalance + totalFee;
        balance = balance.setValue(newBalance);
        return AccountProperties.setProperty(account, balance);
    }

    @Override
    public Set<AccountID> getConfirmingAccounts(Account sender, int timestamp) {
        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

        if (validationMode.isMultiFactor()) {
            HashSet<AccountID> set = new HashSet<>();
            for (Map.Entry<AccountID, Integer> entry : validationMode.delegatesEntrySet()) {
                AccountID id = entry.getKey();
                set.add(id);
            }
            return set;
        }

        return null;
    }

    @Override
    public boolean validConfirmation(Transaction transaction, Map<AccountID, Account> set, int timestamp) {
        Account sender = set.get(transaction.getSenderID());
        if (sender == null) {
            throw new IllegalArgumentException();
        }

        ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

        int maxWeight = 0;
        for (Map.Entry<AccountID, Account> e : set.entrySet()) {

            AccountID id = e.getKey();

            if (transaction.getSenderID().equals(id)) {
                maxWeight += validationMode.getBaseWeight();
            } else if (validationMode.containWeightForAccount(id)) {
                maxWeight += validationMode.getWeightForAccount(id);
            } else {
                throw new IllegalArgumentException();
            }
        }

        if (validationMode.quorumForType(transaction.getType()) <= maxWeight ||
                (maxWeight == validationMode.getMaxWeight() && maxWeight != 0)) {
            return true;
        }

        return false;
    }

    @Override
    public byte[] getPublicKeyBySeed(String seed, int timestamp) throws IllegalArgumentException {
        Item item = Objects.requireNonNull(getItem(timestamp));
        try {
            ISignature signature = item.getCryptoProvider().getSignature();
            return signature.getKeyPair(Format.convert(seed)).publicKey;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public <T> boolean verifySignature(T obj, byte[] signature, Account account, int timestamp) {

        Item item = Objects.requireNonNull(getItem(timestamp));
        byte[] publicKey = AccountProperties.getRegistration(account).getPublicKey();
        return item.getCryptoProvider().verify(obj, getGenesisBlockID(), signature, publicKey);
    }

    @Override
    public int getDifficulty(Transaction transaction, int timestamp) {
        Item item = Objects.requireNonNull(getItem(timestamp));
        return item.getEstimator().estimate(transaction);
    }

    @Override
    public int getMaxNoteLength(int timestamp) {
        Item item = Objects.requireNonNull(getItem(timestamp));
        return item.getMaxNoteLength();
    }

    private Item getItem(int timestamp) {

        if (items.getFirst().isCome(timestamp)) {
            return items.getFirst();
        }

        if (!items.getLast().isCome(timestamp)) {
            return null;
        }

        for (Item item : items) {
            if (item.isCome(timestamp) && !item.isPassed(timestamp)) {
                return item;
            }
        }

        throw new UnsupportedOperationException();
    }

    public long getMinDepositSize() {
        return minDepositSize;
    }

    public void setMinDepositSize(long minDepositSize) {
        this.minDepositSize = minDepositSize;
    }

    private static class ItemComparator implements Comparator<Item> {
        @Override
        public int compare(Item o1, Item o2) {
            // Desc
            return Integer.compare(o2.getNumber(), o1.getNumber());
        }
    }
}
