package com.exscudo.eon.api.bot;

import java.io.IOException;

import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;

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
