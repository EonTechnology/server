package com.exscudo.peer.core.crypto;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.utils.Format;

class AccountPropertyMapper {

	static Map<String, Object> convert(AccountProperty property) throws IOException {

		final Map<String, Object> map = new TreeMap<String, Object>();
		map.put("propertyType", property.getType().toString().toLowerCase());
		map.put("data", property.getData());
		map.put("accountID", Format.ID.accountId(property.getAccountID()));

		return map;

	}

}
