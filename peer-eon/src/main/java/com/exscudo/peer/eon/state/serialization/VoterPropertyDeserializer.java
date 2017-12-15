package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.Voter;

public class VoterPropertyDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.VOTER);
		if (p == null) {
			return null;
		}

		Voter voter = new Voter();
		try {

			if (p.getData().containsKey("timestamp")) {
				int timestamp = Integer.parseInt(String.valueOf(p.getData().get("timestamp")));
				voter.setTimestamp(timestamp);
			}

			if(p.getData().containsKey("polls")) {
				Map<String, Object> polls = (Map<String, Object>) p.getData().get("polls");
				for (Map.Entry<String, Object> e : polls.entrySet()) {

					long accountId = Format.ID.accountId(e.getKey());
					int votes = Integer.parseInt(String.valueOf(e.getValue()));

					voter.setPoll(accountId, votes);
				}
			}

		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
		return voter;

	}
}
