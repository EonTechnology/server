package org.eontechnology.and.peer.core.data.identifier;

public class BlockID extends BaseIdentifier {
    private static final String PREFIX = "EON-B";

    public BlockID(long id) {
        super(id, PREFIX);
    }

    public BlockID(String id) {
        super(id, PREFIX);
    }

    public BlockID(byte[] bytes, int timestamp) {
        super(bytes, timestamp, PREFIX);
    }
}
