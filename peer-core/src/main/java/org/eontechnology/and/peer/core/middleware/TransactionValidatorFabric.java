package org.eontechnology.and.peer.core.middleware;

import org.eontechnology.and.peer.core.common.ITimeProvider;

public interface TransactionValidatorFabric {

    TransactionValidator getAllValidators(ITimeProvider blockProvider);

    TransactionValidator getAllValidators(ITimeProvider blockProvider, ITimeProvider peerProvider);
}
