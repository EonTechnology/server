package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.BlockHelper;
import com.exscudo.peer.core.storage.utils.BlockchainHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BlockHelperTest {
    private Storage storage;
    private BlockHelper blockHelper;
    private BlockchainHelper blockchainHelper;

    @Before
    public void setUp() throws Exception {
        storage = ConnectionUtils.create("/com/exscudo/peer/store/sqlite/blocks_test.sql");
        this.blockHelper = storage.getBlockHelper();
        this.blockchainHelper = storage.getBlockchainHelper();
    }

    @After
    public void after() throws Exception {
        blockHelper = null;
        blockchainHelper = null;
        storage.destroy();
    }

    @Test()
    public void get_with_loadTrsAndProps_should_return_trs_amd_props() throws Exception {
        long blockId = -4478580686957051904L;

        Block block = blockHelper.get(new BlockID(blockId));
        assertNotNull(block);
        assertEquals(block.getID().getValue(), blockId);
        assertEquals(block.getCumulativeDifficulty(), new BigInteger("1"));

        Transaction[] transactions = block.getTransactions().toArray(new Transaction[0]);
        assertEquals(transactions.length, 3);
        Arrays.sort(transactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                return Long.compare(o1.getID().getValue(), o2.getID().getValue());
            }
        });
        assertEquals(transactions[0].getID().getValue(), -5907171703930224640L);
        assertEquals(transactions[1].getID().getValue(), -5790597521193566208L);
        assertEquals(transactions[2].getID().getValue(), 8715428717435813888L);
    }

    @Test
    public void save() throws Exception {

        Block block = new Block();
        block.setVersion(1);
        block.setTimestamp(0);
        block.setPreviousBlock(null);
        block.setGenerationSignature(new byte[64]);
        block.setSenderID(new AccountID(12345L));
        block.setSignature(new byte[64]);
        block.setCumulativeDifficulty(new BigInteger("0"));
        block.setSnapshot("");
        block.setPreviousBlock(new BlockID(0L));
        block.setTransactions(new ArrayList<Transaction>());

        BlockID id = block.getID();
        assertNull(blockHelper.get(id));
        blockHelper.save(block);
        Block b = blockHelper.get(id);
        assertNotNull(b);
    }

    @Test
    public void remove() throws Exception {
        assertNotNull(blockHelper.get(new BlockID(7816843914693836980L)));
        blockHelper.remove(new BlockID(7816843914693836980L));
        assertNull(blockHelper.get(new BlockID(7816843914693836980L)));
    }

    // TODO: review
    @Test
    public void blockLinkedList() throws Exception {
        BlockID[] list = blockchainHelper.getBlockLinkedList(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(list.length, 4);
        assertEquals(list[0].getValue(), 0L);
        assertEquals(list[1].getValue(), -4478580686957051904L);
        assertEquals(list[2].getValue(), 7816843914693836980L);
        assertEquals(list[3].getValue(), -2972036271259516568L);
    }
}
