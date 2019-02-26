package org.eontechology.and.peer.core.data.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class BaseIdentifierTest {

    @Test
    public void test_id() throws Exception {
        Random r = new Random(123);
        byte[] pk = new byte[32];
        r.nextBytes(pk);

        BaseIdentifier id = new BaseIdentifier(pk, "ID");
        assertEquals("ID-RMNF4-KLGQ7-9Y65X", id.toString());
        assertEquals(5700631840397120119L, id.getValue());

        id = new BaseIdentifier(pk, 1, "ID");
        assertEquals("ID-32222-2E26M-GUYNN", id.toString());
        assertEquals(-1352658774023733247L, id.getValue());

        BaseIdentifier newID = new BaseIdentifier(id.toString(), "ID");
        assertEquals(id.toString(), newID.toString());
        assertEquals(id.getValue(), newID.getValue());
        assertTrue(id.equals(newID));
        assertEquals(id, newID);
        assertEquals(id.hashCode(), newID.hashCode());

        newID = new BaseIdentifier(id.getValue(), "ID");
        assertEquals(id.toString(), newID.toString());
        assertEquals(id.getValue(), newID.getValue());
        assertTrue(id.equals(newID));
        assertEquals(id, newID);
        assertEquals(id.hashCode(), newID.hashCode());
    }
}
