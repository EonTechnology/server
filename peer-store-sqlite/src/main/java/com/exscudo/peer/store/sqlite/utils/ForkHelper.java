package com.exscudo.peer.store.sqlite.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.eon.TransactionHandlerDecorator;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

/**
 * Access to hard-forks description. 
 *
 */
public class ForkHelper {

	public static Fork getFork(ConnectionProxy connection) {

		long genesisBlockID = Long.parseLong(SettingHelper.getValue(connection, SettingName.genesisBlockID), 10);

		try {

			PreparedStatement getListStatement = connection.prepareStatement(
					"SELECT \"id\", \"begin\", \"end\", \"target_tx\", \"target_block\" FROM \"forks\"");

			LinkedList<Fork.Item> list = new LinkedList<>();
			synchronized (getListStatement) {
				ResultSet set = getListStatement.executeQuery();
				while (set.next()) {

					int forkID = set.getInt("id");
					long begin = Instant.parse(set.getString("begin")).toEpochMilli();
					long end = Instant.parse(set.getString("end")).toEpochMilli();

					String targetTxVersion = set.getString("target_tx");
					int[] targetTranVersions = Arrays.stream(targetTxVersion.split(" ")).mapToInt(Integer::parseInt)
							.toArray();

					int blockVersion = Integer.parseInt(set.getString("target_block"));
					ITransactionHandler txHandler = new TransactionHandlerDecorator(
							new HashMap<Integer, ITransactionHandler>() {
								private static final long serialVersionUID = 3518338953704623292L;

								{
									put(TransactionType.AccountRegistration,
											new com.exscudo.peer.eon.transactions.handlers.AccountRegistrationHandler());
									put(TransactionType.OrdinaryPayment,
											new com.exscudo.peer.eon.transactions.handlers.OrdinaryPaymentHandler());
									put(TransactionType.DepositRefill,
											new com.exscudo.peer.eon.transactions.handlers.DepositRefillHandler());
									put(TransactionType.DepositWithdraw,
											new com.exscudo.peer.eon.transactions.handlers.DepositWithdrawHandler());
								}
							});
					list.add(new Fork.Item(forkID, begin, end, targetTranVersions, txHandler, blockVersion));

				}
				set.close();

			}

			return new Fork(genesisBlockID, list);

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}
}
