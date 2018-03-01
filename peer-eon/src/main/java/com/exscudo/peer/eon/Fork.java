package com.exscudo.peer.eon;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.importer.IFork;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;

/**
 * Basic implementation of the {@code IFork} interface.
 */
public class Fork implements IFork {

    private final LinkedList<Item> items;
    private final BlockID genesisBlockID;

    public Fork(BlockID genesisBlockID, Item[] items) {
        this.genesisBlockID = genesisBlockID;

        this.items = new LinkedList<>();
        Collections.addAll(this.items, items);
        this.items.sort(new ItemComparator());
    }

    @Override
    public BlockID getGenesisBlockID() {
        return genesisBlockID;
    }

    @Override
    public boolean isPassed(int timestamp) {
        return items.getLast().isPassed(timestamp);
    }

    @Override
    public boolean isCome(int timestamp) {
        return items.getLast().isCome(timestamp);
    }

    @Override
    public int getNumber(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? -1 : item.number;
    }

    @Override
    public ITransactionHandler getTransactionExecutor(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? null : item.handler;
    }

    @Override
    public int getBlockVersion(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? -1 : item.blockVersion;
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

        if (getNumber(timestamp - Constant.BLOCK_PERIOD) == item.number) {
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
        if (deposit.getValue() < EonConstant.MIN_DEPOSIT_SIZE) {
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

        Long scale = AccountProperties.getDeposit(generator).getValue() / EonConstant.DECIMAL_POINT;
        if (scale != 0) {
            hit = hit.divide(BigInteger.valueOf(scale));
        }
        BigInteger value = Format.TWO64.divide(hit);

        return value;
    }

    @Override
    public long getBalance(Account account, int timestamp) {
        return AccountProperties.getBalance(account).getValue();
    }

    @Override
    public Account setBalance(Account account, long value, int timestamp) {
        BalanceProperty balance = AccountProperties.getBalance(account);
        balance = balance.setValue(value);
        return AccountProperties.setProperty(account, balance);
    }

    @Override
    public byte[] getPublicKey(Account account, int timestamp) {
        return AccountProperties.getRegistration(account).getPublicKey();
    }

    private Item getItem(int timestamp) {
        if (!items.getFirst().isCome(timestamp)) {
            return null;
        }
        for (Item item : items) {
            if (item.isCome(timestamp) && !item.isPassed(timestamp)) {
                return item;
            }
        }
        return items.getLast();
    }

    private static class ItemComparator implements Comparator<Item> {
        @Override
        public int compare(Item o1, Item o2) {
            return Integer.compare(o1.number, o2.number);
        }
    }

    public static abstract class AbstractItem {

        public final long begin;
        public final long end;
        public final int number;
        public final ITransactionHandler handler;
        public final int blockVersion;

        public AbstractItem(int number, String begin, String end, ITransactionHandler handler, int blockVersion) {

            this.number = number;
            this.begin = Instant.parse(begin).toEpochMilli();
            this.end = Instant.parse(end).toEpochMilli();

            this.handler = handler;
            this.blockVersion = blockVersion;
        }

        boolean isCome(int timestamp) {
            return timestamp * 1000L > begin;
        }

        boolean isPassed(int timestamp) {
            return timestamp * 1000L > end;
        }

        abstract Account convert(Account account);

        abstract boolean needConvertAccounts();
    }

    public static class Item extends AbstractItem {

        public Item(int number, String begin, String end, ITransactionHandler handler, int blockVersion) {
            super(number, begin, end, handler, blockVersion);
        }

        @Override
        Account convert(Account account) {
            return account;
        }

        @Override
        boolean needConvertAccounts() {
            return false;
        }
    }
}
