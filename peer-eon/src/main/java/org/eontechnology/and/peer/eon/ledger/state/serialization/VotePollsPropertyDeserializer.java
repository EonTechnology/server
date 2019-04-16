package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;

import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertyDeserializer;
import org.eontechnology.and.peer.eon.ledger.state.VotePollsProperty;

public class VotePollsPropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.VOTER);
        if (p == null) {
            return new VotePollsProperty();
        }

        VotePollsProperty voter = new VotePollsProperty();
        try {

            if (p.getData().containsKey("timestamp")) {
                int timestamp = Integer.parseInt(String.valueOf(p.getData().get("timestamp")));
                voter.setTimestamp(timestamp);
            }

            if (p.getData().containsKey("polls")) {

                @SuppressWarnings("unchecked")
                Map<String, Object> polls = (Map<String, Object>) p.getData().get("polls");
                for (Map.Entry<String, Object> e : polls.entrySet()) {

                    AccountID accountId = new AccountID(e.getKey());
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
