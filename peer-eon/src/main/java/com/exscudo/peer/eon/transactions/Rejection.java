package com.exscudo.peer.eon.transactions;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;

public class Rejection {

	/**
	 * Rejection of the right to confirm transactions of the specified account.
	 *
	 * @param accountID
	 * @return
	 */
	public static TransactionBuilder multiFactor(long accountID) {
		Map<String, Object> map = new HashMap<>();
		map.put("account", Format.ID.accountId(accountID));
		return new TransactionBuilder(TransactionType.Rejection, map);
	}

}
