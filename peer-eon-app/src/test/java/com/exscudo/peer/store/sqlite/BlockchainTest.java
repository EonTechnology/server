package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;

import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.BlockchainHelper;
import org.junit.Before;
import org.junit.Test;

public class BlockchainTest {

    private BlockchainHelper blockchainHelper;

    @Before
    public void setUp() throws Exception {
        Storage storage = ConnectionUtils.create("/com/exscudo/peer/store/sqlite/blocks_test.sql");
        this.blockchainHelper = storage.getBlockchainHelper();
    }

    @Test
    public void getBlockHeightTest() throws Exception {

        assertEquals(-1, blockchainHelper.getBlockHeight(new BlockID(0L)));
        assertEquals(0, blockchainHelper.getBlockHeight(new BlockID(-4478580686957051904L)));
        assertEquals(1, blockchainHelper.getBlockHeight(new BlockID(7816843914693836980L)));
        assertEquals(2, blockchainHelper.getBlockHeight(new BlockID(-2972036271259516568L)));
    }

    @Test
    public void getLatestBlocksTest() throws Exception {

        BlockID[] list = blockchainHelper.getBlockLinkedList(0, 3);

        assertEquals(list.length, 3);
        assertEquals(list[0].getValue(), -4478580686957051904L);
        assertEquals(list[1].getValue(), 7816843914693836980L);
        assertEquals(list[2].getValue(), -2972036271259516568L);

        list = blockchainHelper.getBlockLinkedList(1, 3);

        assertEquals(list.length, 2);
        assertEquals(list[0].getValue(), 7816843914693836980L);
        assertEquals(list[1].getValue(), -2972036271259516568L);

        list = blockchainHelper.getBlockLinkedList(2, 3);

        assertEquals(list.length, 1);
        assertEquals(list[0].getValue(), -2972036271259516568L);
    }
}
