package org.eontechology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.AccountProperty;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.eon.PropertyType;
import org.eontechology.and.peer.eon.ledger.AccountPropertySerializer;
import org.eontechology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechology.and.peer.tx.TransactionType;

public class ValidationModePropertySerializer extends AccountPropertySerializer<ValidationModeProperty> {

    public ValidationModePropertySerializer() {
        super(ValidationModeProperty.class);
    }

    @Override
    public Account doSerialize(ValidationModeProperty validationMode, Account account) throws IOException {

        HashMap<String, Object> data = new HashMap<>();

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
        if (defaultQuorum != ValidationModeProperty.MAX_QUORUM || quorums.size() > 1) {
            data.put("quorums", quorums);
        }

        // set delegates
        HashMap<String, Object> delegates = new HashMap<>();
        for (Map.Entry<AccountID, Integer> e : validationMode.delegatesEntrySet()) {
            int weight = e.getValue();
            if (weight != 0) {
                delegates.put(e.getKey().toString(), weight);
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
            if (baseWeight != ValidationModeProperty.MAX_WEIGHT || !delegates.isEmpty()) {
                delegates.put("owner", baseWeight);
            }

            if (!delegates.isEmpty()) {
                data.put("delegates", delegates);
            }
        }

        if (!data.isEmpty()) {
            // timestamp
            data.put("timestamp", validationMode.getTimestamp());
            return account.putProperty(new AccountProperty(PropertyType.MODE, data));
        } else {
            return account.removeProperty(PropertyType.MODE);
        }
    }
}
