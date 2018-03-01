package com.exscudo.peer.store.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.exscudo.peer.core.storage.Storage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SettingHelperTest {
    private Storage storage;
    private Storage.Metadata metadata;

    @Before
    public void setUp() throws Exception {
        storage = ConnectionUtils.create("/com/exscudo/peer/store/sqlite/transactions_test.sql");
        metadata = storage.metadata();
    }

    @After
    public void after() throws Exception {
        storage.destroy();
    }

    @Test
    public void getValue() throws Exception {
        assertEquals(metadata.getProperty("Setting_1"), "Value_1");
    }

    @Test
    public void getNonExistentValue() throws Exception {
        assertNull(metadata.getProperty("Setting_NonExistent"));
    }

    @Test
    public void setValue() throws Exception {
        assertNull(metadata.getProperty("Setting_2"));
        metadata.setProperty("Setting_2", "Value_2");
        assertEquals(metadata.getProperty("Setting_2"), "Value_2");
    }
}
