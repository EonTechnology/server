package org.eontechnology.and.peer.core.env;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

public class ExecutionContextTest {

    @Test
    public void setInnerPeers_should_add_peers_to_peer_registry() {
        ExecutionContext ec = new ExecutionContext();
        String[] peers = new String[] {"inner1", "inner2", "inner3"};
        for (String peer : peers) {
            ec.addInnerPeer(peer);
        }

        for (String peer : ec.getPeers().getPeersList()) {
            assertTrue(Arrays.asList(peers).contains(peer));
            PeerInfo p = ec.getPeers().getPeerByAddress(peer);
            assertTrue(p.isInner());
        }
    }

    @Test
    public void getAnyConnectedPeer_should_return_inner_peers_if_they_set() {
        ExecutionContext ec = new ExecutionContext();
        String[] publicPeers = new String[] {"public1", "public2", "public3", "public4"};
        for (String peer : publicPeers) {
            ec.addPublicPeer(peer);
        }
        String[] innerPeers = new String[] {"inner1", "inner2", "inner3", "inner4"};
        for (String peer : innerPeers) {
            ec.addInnerPeer(peer);
        }
        ExecutionContext.Host h = mock(ExecutionContext.Host.class);
        when(h.getPeerID()).thenReturn(-1L);
        ec.setHost(h);

        for (String peer : ec.getPeers().getPeersList()) {
            PeerInfo p = ec.getPeers().getPeerByAddress(peer);
            p.setState(PeerInfo.STATE_CONNECTED);
        }

        int pubCnt = 0;
        int innCnt = 0;

        for (int i = 0; i < 100; i++) {
            Peer p = ec.getAnyConnectedPeer();
            assertNotNull(p);
            if (p.getPeerInfo().isInner()) {
                innCnt++;
            } else {
                pubCnt++;
            }
        }

        assertTrue(pubCnt > 30);
        assertTrue(innCnt > 30);
    }

    @Test
    public void getAnyPeerToConnect_should_return_inner_peers_if_they_set() {
        ExecutionContext ec = new ExecutionContext();
        String[] publicPeers = new String[] {"public1", "public2", "public3", "public4"};
        for (String peer : publicPeers) {
            ec.addPublicPeer(peer);
        }
        String[] innerPeers = new String[] {"inner1", "inner2", "inner3", "inner4"};
        for (String peer : innerPeers) {
            ec.addInnerPeer(peer);
        }
        ExecutionContext.Host h = mock(ExecutionContext.Host.class);
        when(h.getPeerID()).thenReturn(-1L);
        ec.setHost(h);

        for (String peer : ec.getPeers().getPeersList()) {
            PeerInfo p = ec.getPeers().getPeerByAddress(peer);
            p.setState(PeerInfo.STATE_DISCONNECTED);
        }

        int pubCnt = 0;
        int innCnt = 0;

        for (int i = 0; i < 100; i++) {
            Peer p = ec.getAnyPeerToConnect();
            assertNotNull(p);
            if (p.getPeerInfo().isInner()) {
                innCnt++;
            } else {
                pubCnt++;
            }
        }

        assertTrue(pubCnt > 30);
        assertTrue(innCnt > 30);
    }
}