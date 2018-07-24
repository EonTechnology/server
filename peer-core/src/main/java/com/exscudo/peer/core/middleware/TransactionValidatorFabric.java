package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.common.ITimeProvider;

public interface TransactionValidatorFabric {

    TransactionValidator getAllValidators(ITimeProvider blockProvider);

    TransactionValidator getAllValidators(ITimeProvider blockProvider, ITimeProvider peerProvider);
}
