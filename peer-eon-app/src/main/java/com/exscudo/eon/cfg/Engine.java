package com.exscudo.eon.cfg;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.mapper.crypto.SignedObjectMapper;
import com.exscudo.peer.core.tasks.SyncBlockListTask;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.crypto.Ed25519SignatureVerifier;
import com.exscudo.peer.eon.tasks.GenerateBlockTask;
import com.exscudo.peer.eon.tasks.PeerConnectTask;
import com.exscudo.peer.eon.tasks.PeerDistributeTask;
import com.exscudo.peer.eon.tasks.PeerRemoveTask;
import com.exscudo.peer.eon.tasks.SyncPeerListTask;
import com.exscudo.peer.eon.tasks.SyncTimeTask;
import com.exscudo.peer.eon.tasks.SyncTransactionListTask;
import com.exscudo.peer.eon.tasks.TimedTask;

/**
 * Initializer peer tasks
 */
public class Engine {

	private ScheduledExecutorService scheduledThreadPool;

	private Engine() {
	}

	public static Engine init(ExecutionContext context) {

		try {

			Ed25519SignatureVerifier signatureVerifier = new Ed25519SignatureVerifier();
			CryptoProvider cryptoProvider = new CryptoProvider(
					new SignedObjectMapper(context.getCurrentFork().getGenesisBlockID()));
			cryptoProvider.addProvider(signatureVerifier);
			cryptoProvider.setDefaultProvider(signatureVerifier.getName());
			CryptoProvider.init(cryptoProvider);

			Engine engine = new Engine();

			engine.scheduledThreadPool = Executors.newScheduledThreadPool(12);

			engine.scheduledThreadPool.scheduleAtFixedRate(timed(new SyncPeerListTask(context)), 0, 5,
					TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleWithFixedDelay(timed(new PeerRemoveTask(context)), 0, 1,
					TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleAtFixedRate(timed(new PeerDistributeTask(context)), 0, 30,
					TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleAtFixedRate(timed(new PeerConnectTask(context)), 0, 5, TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleAtFixedRate(timed(new SyncTimeTask(context)), 0, 60, TimeUnit.MINUTES);
			engine.scheduledThreadPool.scheduleAtFixedRate(timed(new SyncTransactionListTask(context)), 0, 1,
					TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleWithFixedDelay(timed(new GenerateBlockTask(context)), 0, 1,
					TimeUnit.SECONDS);
			// engine.scheduledThreadPool.scheduleAtFixedRate(timed(new
			// SyncForkedTransactionListTask(context)), 0, 3,
			// TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleWithFixedDelay(timed(new SyncBlockListTask(context)), 0, 5,
					TimeUnit.SECONDS);

			return engine;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void destory() {
		scheduledThreadPool.shutdownNow();
	}

	public static Runnable timed(Runnable main) {
		return new TimedTask(main);
	}

}
