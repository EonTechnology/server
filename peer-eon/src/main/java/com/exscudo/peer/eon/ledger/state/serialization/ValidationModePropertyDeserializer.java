package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.AccountPropertyDeserializer;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;

public class ValidationModePropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.MODE);
        if (p == null) {
            return new ValidationModeProperty();
        }

        ValidationModeProperty validationMode = new ValidationModeProperty();
        try {

            if (p.getData().containsKey("timestamp")) {
                int timestamp = Integer.parseInt(String.valueOf(p.getData().get("timestamp")));
                validationMode.setTimestamp(timestamp);
            }

            boolean isPublic = false;
            if (p.getData().containsKey("seed")) {
                validationMode.setPublicMode(String.valueOf(p.getData().get("seed")));
                validationMode.setBaseWeight(0);
                isPublic = true;
            }

            // delegates
            if (p.getData().containsKey("delegates")) {

                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) p.getData().get("delegates");
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    if (e.getKey().equals("owner")) {
                        if (isPublic) {
                            throw new IOException();
                        }
                        validationMode.setBaseWeight(Integer.parseInt(String.valueOf(e.getValue())));
                    } else {
                        AccountID id = new AccountID(e.getKey());
                        int weight = Integer.parseInt(String.valueOf(e.getValue()));
                        validationMode.setWeightForAccount(id, weight);
                    }
                }
            }

            // quorums
            if (p.getData().containsKey("quorums")) {

                @SuppressWarnings("unchecked")
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
