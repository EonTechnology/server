package org.eontechnology.and.peer.core.data.identifier;

public class AccountID extends BaseIdentifier {
    private static final String PREFIX = "EON";

    public AccountID(long id) {
        super(id, PREFIX);
    }

    public AccountID(String id) {
        super(id, PREFIX);
    }

    public AccountID(byte[] bytes) {
        super(bytes, PREFIX);
    }
}
