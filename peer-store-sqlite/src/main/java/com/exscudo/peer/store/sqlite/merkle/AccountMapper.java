package com.exscudo.peer.store.sqlite.merkle;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.Account;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Convert account to Map
 */
class AccountMapper {

	public static Map<String, Object> convert(IAccount account) {

		Map<String, Object> properties = new TreeMap<>();
		for (AccountProperty property : account.getProperties()) {
			properties.put(property.getType(), property.getData());
		}
		Map<String, Object> map = new TreeMap<>();
		map.put("id", account.getID());
		map.put("properties", properties);
		return map;

	}

	public static Account convert(Map<String, Object> map) {

		ArrayList<AccountProperty> properties = new ArrayList<>();

		if (map.containsKey("properties")) {
			Map<String, Object> p = (Map<String, Object>) map.get("properties");

			for (Map.Entry<String, Object> o : p.entrySet()) {

				AccountProperty property = new AccountProperty(String.valueOf(o.getKey()),
						(Map<String, Object>) o.getValue());
				properties.add(property);
			}
		}
		return new Account((Long) map.get("id"), properties.toArray(new AccountProperty[0]));
	}

}
