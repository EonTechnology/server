package com.exscudo.eon.app.api.bot;

import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.identifier.BlockID;

/**
 * Current peer time service
 */
public class PropertiesBotService {
    private final BlockID genesisBlockId;
    private final TimeProvider timeProvider;

    public PropertiesBotService(BlockID genesisBlockId, TimeProvider timeProvider) {
        this.genesisBlockId = genesisBlockId;
        this.timeProvider = timeProvider;
    }

    /**
     * Get current peer time service
     *
     * @return unix timestamp
     */
    public long getTimestamp() {
        return timeProvider.get();
    }

    /**
     * Get current network ID
     *
     * @return Network ID
     */
    public String getNetwork() {
        return genesisBlockId.toString();
    }
}
