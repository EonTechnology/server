package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.Voter;

public class VoterPropertySerializer extends AccountPropertySerializer<Voter> {

	public VoterPropertySerializer() {
		super(Voter.class);
	}

	@Override
	public void doSerialize(Voter voter, IAccount account) throws IOException {

		HashMap<String, Object> polls = new HashMap<>();
		for (Map.Entry<Long, Integer> e : voter.pollsEntrySet()) {
			int votes = e.getValue();
			if (votes != 0) {
				polls.put(Format.ID.accountId(e.getKey()), votes);
			}
		}

		if (polls.isEmpty()) {
			account.removeProperty(PropertyType.VOTER);
		} else {
			HashMap<String, Object> data = new HashMap<>();
			data.put("timestamp", voter.getTimestamp());
			data.put("polls", polls);
			account.putProperty(new AccountProperty(PropertyType.VOTER, data));
		}

	}

}
