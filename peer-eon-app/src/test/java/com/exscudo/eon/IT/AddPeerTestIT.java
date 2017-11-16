package com.exscudo.eon.IT;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.eon.TimeProvider;

@SuppressWarnings("WeakerAccess")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AddPeerTestIT {

	protected static String GENERATOR = "55373380ff77987646b816450824310fb377c1a14b6f725b94382af3cf7b788a";
	protected static String GENERATOR2 = "dd6403d520afbfadeeff0b1bb49952440b767663454ab1e5f1a358e018cf9c73";
	TimeProvider mockTimeProvider;

	protected PeerContext ctx1;
	protected PeerContext ctx2;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);
		ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		ctx1.setPeerToConnect(ctx2);
		ctx2.setPeerToConnect(ctx1);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		ctx1.generateBlockForNow();
		ctx2.fullBlockSync();
	}

	@Test
	public void step_1_OK() throws IOException, RemotePeerException {

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

		Fork fork = Mockito.spy(ctx2.context.getCurrentFork());
		Mockito.when(fork.getGenesisBlockID()).thenReturn(0L);
		Mockito.when(ctx2.context.getCurrentFork()).thenReturn(fork);

		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Different NetworkID", ex.getMessage());
		}
	}

	@Test
	public void step_5_IncorrectFork() throws IOException, RemotePeerException {

		Fork fork = Mockito.spy(ctx2.context.getCurrentFork());
		Mockito.when(fork.getNumber(Mockito.anyInt())).thenReturn(5);
		Mockito.when(ctx2.context.getCurrentFork()).thenReturn(fork);

		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Different Fork", ex.getMessage());
		}
	}

}
