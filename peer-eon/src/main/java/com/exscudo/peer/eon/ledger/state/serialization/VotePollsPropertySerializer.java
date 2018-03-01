package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountPropertySerializer;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;

public class VotePollsPropertySerializer extends AccountPropertySerializer<VotePollsProperty> {

    public VotePollsPropertySerializer() {
        super(VotePollsProperty.class);
    }

    @Override
    public Account doSerialize(VotePollsProperty voter, Account account) throws IOException {

        HashMap<String, Object> polls = new HashMap<>();
        for (Map.Entry<AccountID, Integer> e : voter.pollsEntrySet()) {
            int votes = e.getValue();
            if (votes != 0) {
                polls.put(e.getKey().toString(), votes);
            }
        }

        if (polls.isEmpty()) {
            return account.removeProperty(PropertyType.VOTER);
        } else {
            HashMap<String, Object> data = new HashMap<>();
            data.put("timestamp", voter.getTimestamp());
            data.put("polls", polls);
            return account.putProperty(new AccountProperty(PropertyType.VOTER, data));
        }
    }
}
