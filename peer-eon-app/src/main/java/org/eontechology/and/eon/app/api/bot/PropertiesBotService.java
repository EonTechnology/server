package org.eontechology.and.eon.app.api.bot;

import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.data.identifier.BlockID;

/**
 * Current peer time service
 */
public class PropertiesBotService {
    private final BlockID genesisBlockId;
    private final ITimeProvider timeProvider;

    public PropertiesBotService(BlockID genesisBlockId, ITimeProvider timeProvider) {
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
