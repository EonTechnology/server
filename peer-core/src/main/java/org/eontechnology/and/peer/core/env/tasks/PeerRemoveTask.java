package org.eontechnology.and.peer.core.env.tasks;

import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.env.PeerInfo;

/** Removes nodes from the connected and from blacklist after the blocking time has expired. */
public final class PeerRemoveTask implements Runnable {

  private final ExecutionContext context;

  public PeerRemoveTask(ExecutionContext context) {

    this.context = context;
  }

  @Override
  public void run() {

    try {

      long curTime = System.currentTimeMillis();

      String[] wellKnownPeers = context.getPeers().getPeersList();
      for (String address : wellKnownPeers) {
        if (address == null) {
          continue;
        }

        PeerInfo peer = context.getPeers().getPeerByAddress(address);
        if (peer != null) {

          synchronized (peer) {
            if (peer.getBlacklistingTime() > 0) {
              if (peer.getBlacklistingTime() + context.getBlacklistingPeriod() <= curTime) {

                peer.setState(PeerInfo.STATE_AMBIGUOUS);
                peer.setBlacklistingTime(0);

                Loggers.info(
                    PeerRemoveTask.class,
                    "Peer \"{}\" has been removed from blacklist.",
                    peer.getAddress());
              }
            } else {
              if (peer.getState() == PeerInfo.STATE_CONNECTED
                  && peer.getConnectingTime() <= curTime) {

                peer.setState(PeerInfo.STATE_DISCONNECTED);
                Loggers.info(
                    PeerRemoveTask.class, "Peer \"{}\" has been disconnected.", peer.getAddress());
              }
            }

            if (!peer.isInner()
                && !peer.isImmutable()
                && peer.getConnectingTime() < curTime - 60 * 60 * 1000) {

              context.getPeers().remove(address);
              Loggers.info(
                  PeerRemoveTask.class,
                  "Peer \"{}\" has been removed from peer list.",
                  peer.getAddress());
            }
          }
        }
      }
    } catch (Exception e) {

      Loggers.error(PeerRemoveTask.class, e);
    }
  }
}
