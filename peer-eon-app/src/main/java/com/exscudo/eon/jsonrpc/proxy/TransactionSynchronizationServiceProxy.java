package com.exscudo.eon.jsonrpc.proxy;

import java.io.IOException;

import com.exscudo.peer.core.api.ITransactionSynchronizationService;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.env.IServiceProxyFactory;

/**
 * Proxy for {@code ITransactionSynchronizationService} on remote peer
 * <p>
 * To create use {@link IServiceProxyFactory#createProxy}
 *
 * @see ITransactionSynchronizationService
 */
public class TransactionSynchronizationServiceProxy extends PeerServiceProxy implements ITransactionSynchronizationService {

    @Override
    public Transaction[] getTransactions(String lastBlockId,
                                         String[] ignoreList) throws RemotePeerException, IOException {
        return doRequest("getTransactions", new Object[] {lastBlockId, ignoreList}, Transaction[].class);
    }
}
