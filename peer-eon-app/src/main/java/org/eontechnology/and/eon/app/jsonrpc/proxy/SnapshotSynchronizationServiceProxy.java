package org.eontechnology.and.eon.app.jsonrpc.proxy;

import java.io.IOException;
import org.eontechnology.and.peer.core.api.IBlockSynchronizationService;
import org.eontechnology.and.peer.core.api.ISnapshotSynchronizationService;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.env.IServiceProxyFactory;

/**
 * Proxy for {@code IBlockSynchronizationService} on remote peer
 *
 * <p>To create use {@link IServiceProxyFactory#createProxy}
 *
 * @see IBlockSynchronizationService
 */
public class SnapshotSynchronizationServiceProxy extends PeerServiceProxy
    implements ISnapshotSynchronizationService {

  @Override
  public Block getLastBlock() throws RemotePeerException, IOException {
    return doRequest("get_last_block", new Object[0], Block.class);
  }

  @Override
  public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
    return doRequest("get_block_by_height", new Object[] {height}, Block.class);
  }

  @Override
  public Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException {
    return doRequest("get_blocks_head_from", new Object[] {height}, Block[].class);
  }

  @Override
  public Account[] getAccounts(String blockID) throws RemotePeerException, IOException {
    return doRequest("get_accounts", new Object[] {blockID}, Account[].class);
  }

  @Override
  public Account[] getNextAccounts(String blockID, String accountID)
      throws RemotePeerException, IOException {
    return doRequest("get_next_accounts", new Object[] {blockID, accountID}, Account[].class);
  }
}
