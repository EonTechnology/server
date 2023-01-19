package org.eontechnology.and.eon.app.api.peer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.api.SalientAttributes;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.env.PeerInfo;
import org.eontechnology.and.peer.core.env.PeerRegistry;
import org.eontechnology.and.peer.core.storage.Storage;
import org.junit.Before;
import org.junit.Test;

public class SyncMetadataServiceTest {

  private ExecutionContext ctx;
  private PeerRegistry peerRegistry;
  private String peer1Addr;
  private String peer2Addr;
  private String[] peerAddresses;
  private PeerInfo peer1;
  private PeerInfo peer2;
  private IFork fork;
  private ITimeProvider timeProvider;
  private Storage storage;

  @Test
  public void getAttributes() throws Exception {
    long peerId = 100500L;
    String hostAddress = "testAddress.com";
    String app = "testApp";
    String version = "testVersion";

    ExecutionContext ctx = mock(ExecutionContext.class);
    ExecutionContext.Host host = mock(ExecutionContext.Host.class);
    fork = mock(IFork.class);
    when(fork.isPassed(anyInt())).thenReturn(false);
    when(fork.getGenesisBlockID()).thenReturn(new BlockID(12345L));

    when(ctx.getHost()).thenReturn(host);
    when(host.getPeerID()).thenReturn(peerId);
    when(host.getAddress()).thenReturn(hostAddress);
    when(ctx.getApplication()).thenReturn(app);
    when(ctx.getVersion()).thenReturn(version);

    timeProvider = mock(ITimeProvider.class);

    storage = mock(Storage.class);
    when(storage.metadata()).thenReturn(mock(Storage.Metadata.class));

    SyncMetadataService sms = new SyncMetadataService(fork, ctx, storage, timeProvider);
    SalientAttributes attrs = sms.getAttributes();

    assertEquals(peerId, attrs.getPeerId());
    assertEquals(hostAddress, attrs.getAnnouncedAddress());
    assertEquals(app, attrs.getApplication());
    assertEquals(version, attrs.getVersion());

    when(host.getAddress()).thenReturn(null);

    attrs = sms.getAttributes();
    assertEquals(peerId, attrs.getPeerId());
    assertEquals(null, attrs.getAnnouncedAddress());
    assertEquals(app, attrs.getApplication());
    assertEquals(version, attrs.getVersion());
  }

  @Before
  public void setup() {
    ctx = mock(ExecutionContext.class);
    peerRegistry = mock(PeerRegistry.class);
    peer1Addr = "addr1.com";
    peer2Addr = "addr2.tech";
    peerAddresses = new String[] {peer1Addr, peer2Addr};
    peer1 = mock(PeerInfo.class);
    peer2 = mock(PeerInfo.class);

    when(ctx.getPeers()).thenReturn(peerRegistry);
    when(peerRegistry.getPeersList()).thenReturn(peerAddresses);
    when(peerRegistry.getPeerByAddress(peer1Addr)).thenReturn(peer1);
    when(peerRegistry.getPeerByAddress(peer2Addr)).thenReturn(peer2);

    when(peer1.getState()).thenReturn(PeerInfo.STATE_CONNECTED);
    when(peer1.getBlacklistingTime()).thenReturn(0L);
    when(peer1.getAddress()).thenReturn(peer1Addr);
    when(peer1.isInner()).thenReturn(false);

    when(peer2.getState()).thenReturn(PeerInfo.STATE_CONNECTED);
    when(peer2.getBlacklistingTime()).thenReturn(0L);
    when(peer2.getAddress()).thenReturn(peer2Addr);
    when(peer2.isInner()).thenReturn(false);
  }

  @Test
  public void getWellKnownNodes_should_return_array() throws Exception {
    SyncMetadataService sms = new SyncMetadataService(fork, ctx, storage, timeProvider);
    String[] wkn = sms.getWellKnownNodes();
    assertEquals(2, wkn.length);
    assertEquals(peer1Addr, wkn[0]);
    assertEquals(peer2Addr, wkn[1]);
  }

  @Test
  public void getWellKnownNodes_shouldnt_return_notconnected_peers() throws Exception {
    when(peer1.getState()).thenReturn(PeerInfo.STATE_AMBIGUOUS);
    when(peer2.getState()).thenReturn(PeerInfo.STATE_DISCONNECTED);

    SyncMetadataService sms = new SyncMetadataService(fork, ctx, storage, timeProvider);
    String[] wkn = sms.getWellKnownNodes();
    assertEquals(0, wkn.length);
  }

  @Test
  public void getWellKnownNodes_shouldnt_return_blacklist_peers() throws Exception {
    when(peer1.getBlacklistingTime()).thenReturn(60L);
    when(peer2.getBlacklistingTime()).thenReturn(-60L);

    SyncMetadataService sms = new SyncMetadataService(fork, ctx, storage, timeProvider);
    String[] wkn = sms.getWellKnownNodes();
    assertEquals(0, wkn.length);
  }

  @Test
  public void getWellKnownNodes_shouldnt_return_noaddress_peers() throws Exception {
    when(peer1.getAddress()).thenReturn(null);
    when(peer2.getAddress()).thenReturn("");

    SyncMetadataService sms = new SyncMetadataService(fork, ctx, storage, timeProvider);
    String[] wkn = sms.getWellKnownNodes();
    assertEquals(0, wkn.length);
  }

  @Test
  public void getWellKnownNodes_shouldnt_return_inner_peers() throws Exception {
    when(peer1.isInner()).thenReturn(true);
    when(peer2.isInner()).thenReturn(true);

    SyncMetadataService sms = new SyncMetadataService(fork, ctx, storage, timeProvider);
    String[] wkn = sms.getWellKnownNodes();
    assertEquals(0, wkn.length);
  }
}
