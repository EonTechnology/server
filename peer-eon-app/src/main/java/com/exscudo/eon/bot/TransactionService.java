package com.exscudo.eon.bot;

import java.io.IOException;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;

/**
 * Transaction processing service
 */
public class TransactionService {

    private final IBacklogService backlogService;
    private final TimeProvider timeProvider;

    public TransactionService(TimeProvider timeProvider, IBacklogService backlogService) {
        this.backlogService = backlogService;
        this.timeProvider = timeProvider;
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
            // TODO: move to backlog
            if (tx.isFuture(timeProvider.get() + Constant.MAX_LATENCY)) {
                throw new LifecycleException();
            }

            backlogService.put(tx);
        } catch (ValidateException e) {
            throw new RemotePeerException(e);
        }
    }
}
