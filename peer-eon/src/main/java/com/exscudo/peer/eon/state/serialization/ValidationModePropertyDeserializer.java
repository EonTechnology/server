package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.ValidationMode;

public class ValidationModePropertyDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.MODE);
		if (p == null) {
			return null;
		}

		ValidationMode validationMode = new ValidationMode();
		try {

			if (p.getData().containsKey("timestamp")) {
				int timestamp = Integer.parseInt(String.valueOf(p.getData().get("timestamp")));
				validationMode.setTimestamp(timestamp);
			}

			boolean isPublic = false;
			if (p.getData().containsKey("seed")) {
				validationMode.setPublicMode(String.valueOf(p.getData().get("seed")));
				isPublic = true;
			}

			// delegates
			if (p.getData().containsKey("delegates")) {
				Map<String, Object> map = (Map<String, Object>) p.getData().get("delegates");
				for (Map.Entry<String, Object> e : map.entrySet()) {
					if (e.getKey().equals("owner")) {
						if (isPublic) {
							throw new IOException();
						}
						validationMode.setBaseWeight(Integer.parseInt(String.valueOf(e.getValue())));
					} else {
						long id = Format.ID.accountId(e.getKey());
						int weight = Integer.parseInt(String.valueOf(e.getValue()));
						validationMode.setWeightForAccount(id, weight);
					}
				}
			}

			// quorums
			if (p.getData().containsKey("quorums")) {
				Map<String, Object> map = (Map<String, Object>) p.getData().get("quorums");
				for (Map.Entry<String, Object> e : map.entrySet()) {

					if (e.getKey().equals("all")) {
						validationMode.setQuorum(Integer.parseInt(String.valueOf(e.getValue())));
					} else {
						int type = Integer.parseInt(String.valueOf(e.getKey()));
						int quorum = Integer.parseInt(String.valueOf(e.getValue()));

						validationMode.setQuorum(type, quorum);
					}

				}
			}

		} catch (NumberFormatException e) {
			throw new IOException(e);
		}

		return validationMode;

	}
}
