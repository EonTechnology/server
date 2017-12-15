package com.exscudo.peer.core.data.mapper.crypto;

import java.util.Map;

import com.exscudo.peer.core.crypto.ISignedObjectMapper;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;

/**
 * Convert object of supported types to Map
 */
public class SignedObjectMapper implements ISignedObjectMapper {

	private final long networkID;

	public SignedObjectMapper(long networkID) {
		this.networkID = networkID;
	}

	/**
	 * Converts object to Map.
	 *
	 * @param object
	 *            object to convert
	 * @return bytes
	 */
	@Override
	public Map<String, Object> convert(Object object) {

		if (object instanceof Transaction) {
			Map<String, Object> map = TransactionMapper.convert((Transaction) object);
			map.put("network", Format.ID.blockId(networkID));
			return map;
		}
		if (object instanceof Block) {
			return BlockMapper.convert((Block) object);
		}

		throw new UnsupportedOperationException("Unsupported object type : [" + object.getClass() + "]");

	}

}
