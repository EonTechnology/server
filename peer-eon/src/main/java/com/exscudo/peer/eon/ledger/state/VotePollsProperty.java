package com.exscudo.peer.eon.ledger.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.data.identifier.AccountID;

public class VotePollsProperty {

    /**
     * Contains a list of votings that the account is participated.
     */
    private Map<AccountID, Integer> votePolls = new HashMap<>();

    /**
     * Modification time
     */
    private int timestamp = -1;

    public int getTimestamp() {
        return timestamp;
    }

    public VotePollsProperty setTimestamp(int timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public VotePollsProperty setPoll(AccountID accountID, int votes) {
        votePolls.put(accountID, votes);
        return this;
    }

    public Set<Map.Entry<AccountID, Integer>> pollsEntrySet() {
        return votePolls.entrySet();
    }

    public boolean hasPolls() {
        return !votePolls.isEmpty();
    }
}
