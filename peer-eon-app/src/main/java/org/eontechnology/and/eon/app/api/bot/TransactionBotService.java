package org.eontechnology.and.eon.app.api.bot;

import java.io.IOException;

import org.eontechnology.and.peer.core.backlog.IBacklog;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;

/**
 * Transaction processing service
 */
public class TransactionBotService {

    private final IBacklog backlog;

    public TransactionBotService(IBacklog backlog) {
        this.backlog = backlog;
    }

    /**
     * Put transaction to Backlog list.
     *
     * @param tx
     * @throws RemotePeerException
     * @throws IOException
     */
    public void putTransaction(Transaction tx) throws RemotePeerException, IOException {
        try {

            backlog.put(tx);
        } catch (ValidateException e) {
            throw new RemotePeerException(e);
        }
    }
}
