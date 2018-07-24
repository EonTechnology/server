package com.exscudo.eon.app.IT;

import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.core.env.PeerInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InstanceConnectTestIT {

    private static final String GENERATOR_1 = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    private static final String GENERATOR_2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
    private static final String GENERATOR_3 = "391b34d7f878c7f327fd244370edb9d521472e36816a36299341d0220662e0c2";
    private TimeProvider mockTimeProvider;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);
    }

    @Test
    public void step_1_sync_service() throws Exception {

        PeerContext ctx_1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_1).build(mockTimeProvider));
        PeerContext ctx_2 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_2).build(mockTimeProvider));
        PeerContext ctx_3 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_3).build(mockTimeProvider));

        ctx_1.context.addPublicPeer("1");
        ctx_1.context.addPublicPeer("2");
        ctx_1.context.addPublicPeer("3");
        ctx_1.context.addPublicPeer("4");
        ctx_1.context.addPublicPeer("5");

        for (int i = 0; i < 5; i++) {
            Peer peer = ctx_1.context.getAnyPeerToConnect();
            if (peer != null) {
                ctx_1.context.connectPeer(peer, 0);
            }
        }

        ctx_2.setPeerToConnect(ctx_1);
        ctx_2.syncPeerListTask.run();

        Assert.assertEquals("1=>2) Sync all peers", 5, ctx_2.context.getPeers().getPeersList().length);
        Assert.assertEquals("2) Connected 1 peer", 1, ctx_2.context.getConnectedPeerCount());

        Peer peer = ctx_2.context.getAnyPeerToConnect();
        ctx_2.context.connectPeer(peer, 0);

        Assert.assertEquals("2) Connected 2 peer", 2, ctx_2.context.getConnectedPeerCount());

        ctx_3.setPeerToConnect(ctx_2);
        ctx_3.syncPeerListTask.run();

        Assert.assertEquals("2=>3) Sync all connected peers", 2, ctx_3.context.getPeers().getPeersList().length);
    }

    @Test
    public void step_2_sync_task() throws Exception {

        PeerContext ctx_1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_1).build(mockTimeProvider));
        PeerContext ctx_2 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_2).build(mockTimeProvider));

        ctx_2.context.addPublicPeer("1");
        ctx_2.context.addPublicPeer("2");
        ctx_2.context.addPublicPeer("3");
        ctx_2.context.addPublicPeer("4");
        ctx_2.context.addPublicPeer("5");

        ctx_2.setPeerToConnect(ctx_1);
        ctx_2.peerConnectTask.run();

        Assert.assertEquals("Connected 2 peer", 2, ctx_2.context.getConnectedPeerCount());
    }

    @Test
    public void step_3_do_not_connect_itself() throws Exception {

        PeerContext ctx_1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_1).build(mockTimeProvider));

        ctx_1.context.addPublicPeer("1");
        ctx_1.context.addPublicPeer("2");
        ctx_1.context.addPublicPeer("3");
        ctx_1.context.addPublicPeer("4");
        ctx_1.context.addPublicPeer("5");

        ctx_1.setPeerToConnect(ctx_1);
        ctx_1.peerConnectTask.run();

        Assert.assertEquals("Connected 1 peer", 1, ctx_1.context.getConnectedPeerCount());
    }

    @Test
    public void step_4_remove_connected() throws Exception {

        PeerContext ctx_1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_1).build(mockTimeProvider));

        Peer peer = ctx_1.context.getAnyConnectedPeer();

        ctx_1.peerRemoveTask.run();

        Assert.assertEquals("Connected 0 peers", 0, ctx_1.context.getConnectedPeerCount());
        Assert.assertTrue("IInstance is disconnected", peer.getPeerInfo().getState() == PeerInfo.STATE_DISCONNECTED);

        ctx_1.context.connectPeer(peer);

        ctx_1.peerRemoveTask.run();

        Assert.assertEquals("Connected 1 peers", 1, ctx_1.context.getConnectedPeerCount());
        Assert.assertTrue("IInstance is connected", peer.getPeerInfo().getState() == PeerInfo.STATE_CONNECTED);
    }

    @Test
    public void step_5_remove_blacklisted() throws Exception {

        PeerContext ctx_1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_1).build(mockTimeProvider));

        Peer peer = ctx_1.context.getAnyConnectedPeer();
        ctx_1.context.blacklistPeer(peer);

        Assert.assertNull("IInstance not connected", ctx_1.context.getAnyConnectedPeer());
        Assert.assertNull("IInstance not to connect", ctx_1.context.getAnyPeerToConnect());

        ctx_1.peerRemoveTask.run();

        Assert.assertNull("IInstance not connected", ctx_1.context.getAnyConnectedPeer());
        Assert.assertNull("IInstance not to connect", ctx_1.context.getAnyPeerToConnect());

        ctx_1.context.blacklistPeer(peer, System.currentTimeMillis() - ctx_1.context.getBlacklistingPeriod() - 1);

        Assert.assertNull("IInstance not connected", ctx_1.context.getAnyConnectedPeer());
        Assert.assertNull("IInstance not to connect", ctx_1.context.getAnyPeerToConnect());

        ctx_1.peerRemoveTask.run();

        Assert.assertNull("IInstance not connected", ctx_1.context.getAnyConnectedPeer());
        Assert.assertNotNull("IInstance to connect", ctx_1.context.getAnyPeerToConnect());
    }

    @Test
    public void step_6_remove_old_connected() throws Exception {

        PeerContext ctx_1 = new PeerContext(PeerStarterFactory.create().seed(GENERATOR_1).build(mockTimeProvider));

        ctx_1.context.addPublicPeer("2");
        Assert.assertEquals("Peer updated", 2, ctx_1.context.getPeers().getPeersList().length);

        Peer peer = ctx_1.context.getAnyPeerToConnect();
        peer.getPeerInfo().setConnectingTime(1);

        ctx_1.peerRemoveTask.run();

        Assert.assertEquals("Peer list cleared", 1, ctx_1.context.getPeers().getPeersList().length);
    }
}
