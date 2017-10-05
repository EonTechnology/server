package com.exscudo.peer.eon;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.tasks.SyncBlockListTask;
import com.exscudo.peer.eon.crypto.Ed25519SignatureVerifier;
import com.exscudo.peer.eon.tasks.*;

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
			CryptoProvider cryptoProvider = CryptoProvider.getInstance();
			cryptoProvider.addProvider(signatureVerifier);
			cryptoProvider.setDefaultProvider(signatureVerifier.getName());

			Engine engine = new Engine();

			engine.scheduledThreadPool = Executors.newScheduledThreadPool(12);

			engine.scheduledThreadPool.scheduleAtFixedRate(new SyncPeerListTask(context), 0, 5, TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleWithFixedDelay(new PeerRemoveTask(context), 0, 1, TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleAtFixedRate(new PeerConnectTask(context), 0, 5, TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleAtFixedRate(new SyncTimeTask(context), 0, 60, TimeUnit.MINUTES);
			engine.scheduledThreadPool.scheduleAtFixedRate(new SyncTransactionListTask(context), 0, 1,
					TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleWithFixedDelay(new GenerateBlockTask(context), 0, 1, TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleAtFixedRate(new SyncForkedTransactionListTask(context), 0, 3,
					TimeUnit.SECONDS);
			engine.scheduledThreadPool.scheduleWithFixedDelay(new SyncBlockListTask(context), 0, 5, TimeUnit.SECONDS);

			return engine;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void destory() {
		scheduledThreadPool.shutdownNow();
	}

}
