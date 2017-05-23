package com.exscudo.eon.peer.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.naming.NamingException;

import com.exscudo.eon.peer.Constant;
import com.exscudo.eon.peer.ExecutionContext;
import com.exscudo.eon.peer.Peer;
import com.exscudo.eon.peer.Peer.State;
import com.exscudo.eon.peer.contract.DataSynchronizationService;
import com.exscudo.eon.peer.data.DatastoreConnector;
import com.exscudo.eon.peer.data.DatastoreConnector.TransactionMapper;
import com.exscudo.eon.peer.data.Transaction;
import com.exscudo.eon.peer.exceptions.ProtocolException;
import com.exscudo.eon.peer.exceptions.RemotePeerException;
import com.exscudo.eon.peer.exceptions.ValidateException;
import com.exscudo.eon.utils.Format;
import com.exscudo.eon.utils.Loggers;

/**
 * Performs the task of synchronizing the list of unconfirmed transactions of
 * the current node and random node
 * 
 */
public final class SyncTransactionListTask extends AbstractTask implements Runnable {
	private final DatastoreConnector target;

	public SyncTransactionListTask(ExecutionContext context, DatastoreConnector target) {
		super(context);

		this.target = target;
	}

	@Override
	public void run() {

		try {

			final Peer peer = context.getAnyConnectedPeer();
			if (peer != null) {

				final TransactionMapper mapper = target.transactions();
				Transaction[] transactions = null;
				try {

					ArrayList<String> encodedIDs = new ArrayList<>();

					int count = Constant.TRANSACTION_SIZE_LIMIT;
					Enumeration<Long> enumeration = mapper.unconfirmed().indexes();
					while (enumeration.hasMoreElements() && --count >= 0) {
						encodedIDs.add(Format.TransactionIdEncode(enumeration.nextElement()));
					}

					DataSynchronizationService stub = context.createProxy(peer, DataSynchronizationService.class);
					transactions = stub.getTransactions(encodedIDs.toArray(new String[0]));

				} catch (RemotePeerException | IOException e) {

					synchronized (peer) {
						if (peer.getState() == State.STATE_AMBIGUOUS) {
							peer.setBlacklistingTime(System.currentTimeMillis());
						} else {
							peer.setState(State.STATE_DISCONNECTED);
						}
					}

					Loggers.STREAM.trace(SyncTransactionListTask.class,
							">> [" + peer.getAnnouncedAddress() + "] Failed to execute a request.", e);
					Loggers.VERBOSE.info(SyncTransactionListTask.class, "The node is disconnected. \"{}\".",
							peer.getAnnouncedAddress());
					return;
				}

				if (transactions != null) {
					for (Transaction tx : transactions) {
						if (tx == null) {
							continue;
						}

						try {

							target.importTransaction(tx);

						} catch (ValidateException e) {

							Loggers.STREAM.trace(SyncTransactionListTask.class, " >> [" + peer.getAnnouncedAddress()
									+ "] Unable to process transaction. " + tx.toString(), e);

						} catch (NamingException e) {

							// An incorrect type of transaction has been passed.
							// May be protocol exception.
							Loggers.STREAM.warning(SyncTransactionListTask.class,
									" >> [" + peer.getAnnouncedAddress() + "] " + tx.toString(), e);
							throw new ProtocolException(e);

						}
					}
				}
			}

		} catch (Exception e) {
			Loggers.NOTICE.error(SyncTransactionListTask.class, e);
		}
	}

}
