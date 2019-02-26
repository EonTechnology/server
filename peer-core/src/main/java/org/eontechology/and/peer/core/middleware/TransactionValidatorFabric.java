package org.eontechology.and.peer.core.middleware;

import org.eontechology.and.peer.core.common.ITimeProvider;

public interface TransactionValidatorFabric {

    TransactionValidator getAllValidators(ITimeProvider blockProvider);

    TransactionValidator getAllValidators(ITimeProvider blockProvider, ITimeProvider peerProvider);
}
