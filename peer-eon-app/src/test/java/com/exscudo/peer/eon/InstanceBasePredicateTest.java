package com.exscudo.peer.eon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.PeerInfo;
import org.junit.Before;
import org.junit.Test;

public class InstanceBasePredicateTest {

    private PeerInfo[] peers;

    @Before
    public void setup() {
        boolean isInner = false;
        peers = new PeerInfo[100];
        for (int i = 0; i < 100; i++) {
            PeerInfo p = mock(PeerInfo.class);
            PeerInfo.Metadata m = mock(PeerInfo.Metadata.class);
            when(p.getBlacklistingTime()).thenReturn(0L);
            when(p.getAddress()).thenReturn(String.format("peer%d", i + 1));
            when(p.isInner()).thenReturn(isInner);
            when(p.getMetadata()).thenReturn(m);
            when(m.getPeerID()).thenReturn((long) i + 1);

            peers[i] = p;
            isInner = !isInner;
        }
    }

    @Test
    public void predicate_should_filter_not_inner_peers_only() throws Exception {
        for (int i = 0; i < 100; i++) {

            ExecutionContext.PeerBasePredicate pr = new ExecutionContext.PeerBasePredicate(0L, false) {
            };

            PeerInfo[] rezult = Arrays.stream(peers).filter(pr).toArray(PeerInfo[]::new);
            assertEquals(50, rezult.length);
            for (PeerInfo p : rezult) {
                assertEquals(false, p.isInner());
            }
        }
    }

    @Test
    public void predicate_should_filter_any_peers_randomly() throws Exception {
        int in = 0;
        int out = 0;

        for (int i = 0; i < 100; i++) {

            ExecutionContext.PeerBasePredicate pr = new ExecutionContext.PeerBasePredicate(0L, true) {
            };

            PeerInfo[] rezult = Arrays.stream(peers).filter(pr).toArray(PeerInfo[]::new);

            boolean inner = false;
            if (rezult[0].isInner()) {
                in++;
                inner = true;
            } else {
                out++;
            }

            assertEquals(50, rezult.length);
            for (PeerInfo p : rezult) {
                assertEquals(inner, p.isInner());
            }
        }

        assertTrue(in > 0);
        assertTrue(out > 0);
    }
}