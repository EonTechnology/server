package org.eontechnology.and.eon.app.api.peer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.api.IMetadataService;
import org.eontechnology.and.peer.core.api.SalientAttributes;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.env.Peer;
import org.eontechnology.and.peer.core.env.PeerInfo;
import org.eontechnology.and.peer.core.storage.Storage;

/** Basic implementation of the {@code IMetadataService} interface */
public class SyncMetadataService extends BaseService implements IMetadataService {
  private final ExecutionContext context;
  private final IFork fork;
  private final Storage storage;
  private final ITimeProvider timeProvider;

  public SyncMetadataService(
      IFork fork, ExecutionContext context, Storage storage, ITimeProvider timeProvider) {
    this.context = context;
    this.fork = fork;
    this.storage = storage;
    this.timeProvider = timeProvider;
  }

  @Override
  public SalientAttributes getAttributes() throws RemotePeerException, IOException {

    SalientAttributes originAttributes = new SalientAttributes();
    originAttributes.setApplication(context.getApplication());
    originAttributes.setVersion(context.getVersion());
    originAttributes.setPeerId(context.getHost().getPeerID());

    originAttributes.setNetworkID(fork.getGenesisBlockID().toString());
    originAttributes.setFork(fork.getNumber(timeProvider.get()));

    originAttributes.setHistoryFromHeight(storage.metadata().getHistoryFromHeight());

    if (context.getHost().getAddress() != null && context.getHost().getAddress().length() > 0) {
      originAttributes.setAnnouncedAddress(context.getHost().getAddress());
    }
    return originAttributes;
  }

  @Override
  public String[] getWellKnownNodes() throws RemotePeerException, IOException {

    Collection<String> wellKnownPeers = new ArrayList<>();

    String[] addresses = context.getPeers().getPeersList();
    for (String address : addresses) {

      PeerInfo peer = context.getPeers().getPeerByAddress(address);
      if (peer != null) {
        if (peer.getState() == PeerInfo.STATE_CONNECTED
            && peer.getBlacklistingTime() == 0
            && peer.getAddress() != null
            && peer.getAddress().length() > 0
            && !peer.isInner()) {
          wellKnownPeers.add(peer.getAddress());
        }
      }
    }

    return wellKnownPeers.toArray(new String[0]);
  }

  @Override
  public boolean addPeer(long peerID, String address) throws RemotePeerException, IOException {

    if (peerID == context.getHost().getPeerID()) {
      throw new RemotePeerException("Adding by myself");
    }

    try {

      URL url = new URL("https://" + address);
      if (url.getHost().equals("127.0.0.1") || url.getHost().equals("localhost")) {
        address = getRemoteHost() + ":" + url.getPort();
        url = new URL("https://" + address);
      }

      PeerInfo peerByIP = context.getPeers().getPeerByIP(url.getHost());
      if (peerByIP != null) {
        if (!Objects.equals(peerByIP.getAddress(), address)) {
          throw new RemotePeerException("Peer IP already registered");
        }
        return true;
      }
    } catch (RemotePeerException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RemotePeerException(ex);
    }

    PeerInfo peerInfo = new PeerInfo(0, address, PeerInfo.TYPE_NORMAL);
    Peer peer = new Peer(peerInfo, context.getProxyFactory());
    SalientAttributes attributes = peer.getMetadataService().getAttributes();

    if (attributes.getPeerId() != peerID) {
      throw new RemotePeerException("Different PeerID in attributes");
    }

    if (!fork.getGenesisBlockID().toString().equals(attributes.getNetworkID())) {
      throw new RemotePeerException("Different NetworkID");
    }

    int forkNumber = fork.getNumber(timeProvider.get());
    if (attributes.getFork() != forkNumber) {
      throw new RemotePeerException("Different Fork");
    }

    context.getPeers().addPeer(address);

    return true;
  }
}
