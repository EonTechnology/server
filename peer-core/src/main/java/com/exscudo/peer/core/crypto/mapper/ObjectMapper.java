package com.exscudo.peer.core.crypto.mapper;

import java.util.Map;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;

/**
 * Convert object of supported types to Map
 */
public class ObjectMapper {

	/**
	 * Converts object to Map.
	 *
	 * @param object
	 *            object to convert
	 * @param forSign
	 *            is used for EDS (remove EDS field)
	 * @return bytes
	 */
	public static Map<String, Object> getBytes(Object object, boolean forSign) {

		if (object instanceof Transaction) {
			return TransactionMapper.convert((Transaction) object, forSign);
		}
		if (object instanceof Block) {
			return BlockMapper.convert((Block) object);
		}

		throw new UnsupportedOperationException("Unsupported object type : [" + object.getClass() + "]");

	}

}
