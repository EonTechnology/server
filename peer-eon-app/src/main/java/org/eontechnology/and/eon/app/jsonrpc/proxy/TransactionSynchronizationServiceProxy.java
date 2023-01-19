package org.eontechnology.and.eon.app.jsonrpc.proxy;

import java.io.IOException;
import org.eontechnology.and.peer.core.api.ITransactionSynchronizationService;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.env.IServiceProxyFactory;

/**
 * Proxy for {@code ITransactionSynchronizationService} on remote peer
 *
 * <p>To create use {@link IServiceProxyFactory#createProxy}
 *
 * @see ITransactionSynchronizationService
 */
public class TransactionSynchronizationServiceProxy extends PeerServiceProxy
    implements ITransactionSynchronizationService {

  @Override
  public Transaction[] getTransactions(String lastBlockId, String[] ignoreList)
      throws RemotePeerException, IOException {
    return doRequest(
        "get_transactions", new Object[] {lastBlockId, ignoreList}, Transaction[].class);
  }
}
