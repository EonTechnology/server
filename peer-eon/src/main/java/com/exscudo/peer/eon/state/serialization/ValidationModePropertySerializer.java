package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.ValidationMode;

public class ValidationModePropertySerializer extends AccountPropertySerializer<ValidationMode> {

	public ValidationModePropertySerializer() {
		super(ValidationMode.class);
	}

	@Override
	public void doSerialize(ValidationMode validationMode, IAccount account) throws IOException {

		HashMap<String, Object> data = new HashMap<String, Object>();

		// set quorum
		int defaultQuorum = validationMode.getBaseQuorum();
		HashMap<String, Object> quorums = new HashMap<>();
		quorums.put("all", defaultQuorum);
		for (Integer type : TransactionType.getTypes()) {
			int quorum = validationMode.quorumForType(type);
			if (quorum != defaultQuorum) {
				quorums.put(type.toString(), quorum);
			}
		}
		if (defaultQuorum != ValidationMode.MAX_QUORUM || quorums.size() > 1) {
			data.put("quorums", quorums);
		}

		// set delegates
		HashMap<String, Object> delegates = new HashMap<>();
		for (Map.Entry<Long, Integer> e : validationMode.delegatesEntrySet()) {
			int weight = e.getValue();
			if (weight != 0) {
				delegates.put(Format.ID.accountId(e.getKey()), weight);
			}
		}
		if (validationMode.isPublic()) {
			if (delegates.isEmpty()) {
				throw new IOException();
			}
			data.put("delegates", delegates);
			data.put("seed", validationMode.getSeed());
		} else {

			int baseWeight = validationMode.getBaseWeight();
			if (baseWeight != ValidationMode.MAX_WEIGHT || !delegates.isEmpty()) {
				delegates.put("owner", baseWeight);
			}

			if (!delegates.isEmpty()) {
				data.put("delegates", delegates);
			}
		}

		// timestamp
		data.put("timestamp", validationMode.getTimestamp());

		account.putProperty(new AccountProperty(PropertyType.MODE, data));
	}

}
