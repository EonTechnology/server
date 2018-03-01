package com.exscudo.peer.core.data.identifier;

public class TransactionID extends BaseIdentifier {
    private static final String PREFIX = "EON-T";

    public TransactionID(long id) {
        super(id, PREFIX);
    }

    public TransactionID(String id) {
        super(id, PREFIX);
    }

    public TransactionID(byte[] bytes, int timestamp) {
        super(bytes, timestamp, PREFIX);
    }
}
