package org.eontechology.and.peer.core.data;

import org.eontechology.and.peer.core.crypto.SignedObject;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.BaseIdentifier;

/**
 * The class {@code SignedMessage} provides base implementation for data message
 * signed by the sender of the object
 */
public abstract class SignedMessage extends SignedObject {
    private static final long serialVersionUID = 1897455959474176742L;

    protected AccountID senderID;
    protected int timestamp;
    protected BaseIdentifier id = null;

    /**
     * ID calculator
     *
     * @return
     */
    protected abstract BaseIdentifier calculateID();

    /**
     * Returns the ID.
     *
     * @return
     */
    public BaseIdentifier getID() {
        if (id == null) {
            id = calculateID();
        }
        return id;
    }

    /**
     * Returns the sender IAccount ID.
     *
     * @return IAccount ID
     */
    public AccountID getSenderID() {
        return senderID;
    }

    /**
     * Sets sender
     *
     * @param senderID sender id
     */
    public void setSenderID(AccountID senderID) {
        this.senderID = senderID;
    }

    /**
     * Returns the time of the creation.
     *
     * @return unix timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Sets timestamp
     *
     * @param timestamp unix timestamp
     */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
