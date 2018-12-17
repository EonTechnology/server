package com.exscudo.eon.app.IT;

import static org.mockito.Mockito.spy;

import java.io.IOException;

import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@SuppressWarnings("WeakerAccess")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AddPeerTestIT {

    protected static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
    protected static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";

    protected PeerContext ctx1;
    protected PeerContext ctx2;
    TimeProvider mockTimeProvider;

    @Before
    public void setUp() throws Exception {
        mockTimeProvider = Mockito.mock(TimeProvider.class);

        PeerStarter starter = PeerStarterFactory.create().seed(GENERATOR).build(mockTimeProvider);
        PeerStarter starter2 = PeerStarterFactory.create().seed(GENERATOR2).build(mockTimeProvider);

        starter.setFork(spy(starter.getFork()));
        starter2.setFork(spy(starter2.getFork()));

        ctx1 = new PeerContext(starter);
        ctx2 = new PeerContext(starter2);

        ctx1.setPeerToConnect(ctx2);
        ctx2.setPeerToConnect(ctx1);

        Block lastBlock = ctx1.blockExplorerService.getLastBlock();
        Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

        ctx1.generateBlockForNow();
        ctx2.fullBlockSync();
    }

    @Test
    public void step_1_OK() throws IOException, RemotePeerException {

        System.out.println(ctx1.syncBlockPeerService.getDifficulty().getLastBlockID());
        System.out.println(ctx1.syncBlockPeerService.getDifficulty().getLastBlockID());
        // Add incorrect peerID
        try {
            Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(0L, "123"));
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertEquals("Different PeerID in attributes", ex.getMessage());
        }
        // PeerID from target per
        try {
            Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx1.context.getHost().getPeerID(), "123"));
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertEquals("Adding by myself", ex.getMessage());
        }

        // All OK
        Assert.assertTrue(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));

        // Add twice
        Assert.assertTrue(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));

        // Exist host, different port
        try {
            Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123:8888"));
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertEquals("Peer IP already registered", ex.getMessage());
        }
    }

    @Test
    public void step_2_IncorrectID() throws IOException, RemotePeerException {
        try {
            Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(0L, "123"));
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertEquals("Different PeerID in attributes", ex.getMessage());
        }
    }

    @Test
    public void step_4_IncorrectNetwork() throws IOException, RemotePeerException {

        Mockito.when(ctx2.fork.getGenesisBlockID()).thenReturn(new BlockID(0L));

        try {
            Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertEquals("Different NetworkID", ex.getMessage());
        }
    }

    @Test
    public void step_5_IncorrectFork() throws IOException, RemotePeerException {

        Mockito.when(ctx2.fork.getNumber(Mockito.anyInt())).thenReturn(5);

        try {
            Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertEquals("Different Fork", ex.getMessage());
        }
    }
}
