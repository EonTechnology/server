package org.eontechnology.and.peer.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eontechnology.and.peer.core.ConnectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SettingHelperTest {
    private Storage storage;

    @Before
    public void setUp() throws Exception {
        storage = ConnectionUtils.create();
    }

    @After
    public void after() throws Exception {
        storage.destroy();
    }

    @Test
    public void getNonExistentValue() throws Exception {
        Storage.Metadata metadata = storage.metadata();
        assertNull(metadata.getProperty("Setting_NonExistent"));
    }

    @Test
    public void setAndGetValue() throws Exception {
        Storage.Metadata metadata = storage.metadata();
        assertNull(metadata.getProperty("Setting_2"));
        metadata.setProperty("Setting_2", "Value_2");
        assertEquals(metadata.getProperty("Setting_2"), "Value_2");
    }
}
