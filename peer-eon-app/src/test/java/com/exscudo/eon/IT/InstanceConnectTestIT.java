package com.exscudo.eon.IT;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.exscudo.peer.eon.IServiceProxyFactory;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.PeerInfo;
import com.exscudo.peer.eon.TimeProvider;

@Category(IIntegrationTest.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InstanceConnectTestIT {

	private static final String GENERATOR_1 = "55373380ff77987646b816450824310fb377c1a14b6f725b94382af3cf7b788a";
	private static final String GENERATOR_2 = "dd6403d520afbfadeeff0b1bb49952440b767663454ab1e5f1a358e018cf9c73";
	private static final String GENERATOR_3 = "2011550637a84ba5e1125a769e137d6625d412d64e495276330f0c62c9cf8417";
	private TimeProvider mockTimeProvider;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);
	}

	@Test
	public void step_1_sync_service() throws Exception {

		PeerContext ctx_1 = new PeerContext(GENERATOR_1, mockTimeProvider);
		PeerContext ctx_2 = new PeerContext(GENERATOR_2, mockTimeProvider);
		PeerContext ctx_3 = new PeerContext(GENERATOR_3, mockTimeProvider);

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
		Assert.assertEquals("2) Connected 1 peer", 1, ctx_2.context.getConnectedPoolSize());

		ctx_3.context.setProxyFactory(new IServiceProxyFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <TService> TService createProxy(PeerInfo peer, Class<TService> clazz) {
				return (TService) ctx_2.syncMetadataPeerService;
			}
		});

		Peer peer = ctx_2.context.getAnyPeerToConnect();
		ctx_2.context.connectPeer(peer, 0);

		Assert.assertEquals("2) Connected 2 peer", 2, ctx_2.context.getConnectedPoolSize());

		ctx_3.syncPeerListTask.run();

		Assert.assertEquals("2=>3) Sync all connected peers", 2, ctx_3.context.getPeers().getPeersList().length);
	}

	@Test
	public void step_2_sync_task() throws Exception {
		PeerContext ctx_1 = new PeerContext(GENERATOR_1, mockTimeProvider);
		PeerContext ctx_2 = new PeerContext(GENERATOR_2, mockTimeProvider);

		ctx_2.context.setProxyFactory(new IServiceProxyFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <TService> TService createProxy(PeerInfo peer, Class<TService> clazz) {
				return (TService) ctx_1.syncMetadataPeerService;
			}
		});

		ctx_2.context.addPublicPeer("1");
		ctx_2.context.addPublicPeer("2");
		ctx_2.context.addPublicPeer("3");
		ctx_2.context.addPublicPeer("4");
		ctx_2.context.addPublicPeer("5");
		ctx_2.peerConnectTask.run();

		Assert.assertEquals("Connected 2 peer", 2, ctx_2.context.getConnectedPoolSize());
	}

	@Test
	public void step_3_do_not_connect_itself() throws Exception {
		PeerContext ctx_1 = new PeerContext(GENERATOR_1, mockTimeProvider);

		ctx_1.context.setProxyFactory(new IServiceProxyFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <TService> TService createProxy(PeerInfo peer, Class<TService> clazz) {
				return (TService) ctx_1.syncMetadataPeerService;
			}
		});

		ctx_1.context.addPublicPeer("1");
		ctx_1.context.addPublicPeer("2");
		ctx_1.context.addPublicPeer("3");
		ctx_1.context.addPublicPeer("4");
		ctx_1.context.addPublicPeer("5");
		ctx_1.peerConnectTask.run();

		Assert.assertEquals("Connected 1 peer", 1, ctx_1.context.getConnectedPoolSize());
	}

	@Test
	public void step_4_remove_connected() throws Exception {
		PeerContext ctx = new PeerContext(GENERATOR_1, mockTimeProvider);

		Peer peer = ctx.context.getAnyConnectedPeer();

		ctx.peerRemoveTask.run();

		Assert.assertEquals("Connected 0 peers", 0, ctx.context.getConnectedPoolSize());
		Assert.assertTrue("IInstance is disconnected",
				peer.getPeerInfo().getState() == PeerInfo.State.STATE_DISCONNECTED);

		ctx.context.connectPeer(peer);

		ctx.peerRemoveTask.run();

		Assert.assertEquals("Connected 1 peers", 1, ctx.context.getConnectedPoolSize());
		Assert.assertTrue("IInstance is connected", peer.getPeerInfo().getState() == PeerInfo.State.STATE_CONNECTED);
	}

	@Test
	public void step_5_remove_blacklisted() throws Exception {
		PeerContext ctx = new PeerContext(GENERATOR_1, mockTimeProvider);

		Peer peer = ctx.context.getAnyConnectedPeer();
		ctx.context.blacklistPeer(peer);

		Assert.assertNull("IInstance not connected", ctx.context.getAnyConnectedPeer());
		Assert.assertNull("IInstance not to connect", ctx.context.getAnyPeerToConnect());

		ctx.peerRemoveTask.run();

		Assert.assertNull("IInstance not connected", ctx.context.getAnyConnectedPeer());
		Assert.assertNull("IInstance not to connect", ctx.context.getAnyPeerToConnect());

		ctx.context.blacklistPeer(peer, System.currentTimeMillis() - ctx.context.getBlacklistingPeriod() - 1);

		Assert.assertNull("IInstance not connected", ctx.context.getAnyConnectedPeer());
		Assert.assertNull("IInstance not to connect", ctx.context.getAnyPeerToConnect());

		ctx.peerRemoveTask.run();

		Assert.assertNull("IInstance not connected", ctx.context.getAnyConnectedPeer());
		Assert.assertNotNull("IInstance to connect", ctx.context.getAnyPeerToConnect());
	}
}
