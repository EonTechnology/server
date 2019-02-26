package org.eontechology.and.eon.app.utils.mapper;

import java.util.HashMap;
import java.util.Map;

import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.AccountProperty;
import org.eontechology.and.peer.core.data.identifier.AccountID;

public class AccountMapper {

    public static Map<String, Object> convert(Account account) {

        HashMap<String, Object> map = new HashMap<>();
        for (AccountProperty property : account.getProperties()) {
            map.put(property.getType(), property.getData());
        }

        map.put(Constants.ID, account.getID().toString());
        return map;
    }

    public static Account convert(Map<String, Object> map) {

        AccountID id = new AccountID(String.valueOf(map.get(Constants.ID)));
        Account account = new Account(id);

        for (String p : map.keySet()) {

            if (p.equals(Constants.ID)) {
                continue;
            }

            Map<String, Object> data = (Map<String, Object>) map.get(p);
            AccountProperty property = new AccountProperty(p, data);
            account = account.putProperty(property);
        }

        return account;
    }
}
