package com.exscudo.peer.core.events;

import java.util.EventObject;
import java.util.concurrent.Executor;

/**
 * Allows an event to be dispatched to run on the specified executor.
 * 
 * @param <TTarget>
 *            type of the target of the event
 */
class DispatchableEventDispatcher<TTarget> {
	private final Executor defaultExecutor;

	public DispatchableEventDispatcher(Executor executor) {
		defaultExecutor = executor;
	}

	public Executor getDefaultExecutor() {
		return defaultExecutor;
	}

	protected void raiseEvent(Executor executor, TTarget target,
			DispatchableEvent<TTarget, ? extends EventObject> event) {
		event.dispatch(executor, target);
	}

	public void raiseEvent(TTarget target, DispatchableEvent<TTarget, ? extends EventObject> event) {
		raiseEvent(getDefaultExecutor(), target, event);
	}
}
