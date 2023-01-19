package org.eontechnology.and.peer.core.common.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.concurrent.Executor;

/**
 * Provides notification of listeners about the event.
 *
 * @param <TListener> Listener interface specification. Defines the list of events.
 */
public class Dispatcher<TListener extends EventListener>
    extends DispatchableEventDispatcher<TListener> {

  final Collection<ExecutableListener<TListener>> listeners = new ArrayList<>();

  public Dispatcher() {
    super(new DefaultExecutor());
  }

  public Dispatcher(Executor executor) {
    super(executor);
  }

  public void addListener(TListener listener) {
    addListener(listener, getDefaultExecutor());
  }

  public synchronized void addListener(TListener listener, Executor executor) {
    listeners.add(new ExecutableListener<>(listener, executor));
  }

  public void removeListener(TListener listener) {
    removeListener(listener, getDefaultExecutor());
  }

  public synchronized void removeListener(TListener listener, Executor executor) {
    listeners.remove(new ExecutableListener<>(listener, executor));
  }

  public void raiseEvent(DispatchableEvent<TListener, ? extends EventObject> e) {
    for (ExecutableListener<TListener> l : getListeners()) {
      raiseEvent(l.getExecutor(), l.getListener(), e);
    }
  }

  private synchronized Collection<ExecutableListener<TListener>> getListeners() {
    Collection<ExecutableListener<TListener>> listeners = new ArrayList<>(this.listeners.size());
    listeners.addAll(this.listeners);
    return listeners;
  }

  private static class ExecutableListener<TListener extends EventListener> {
    private final TListener listener;
    private final Executor executor;

    public ExecutableListener(TListener listener, Executor executor) {
      this.listener = listener;
      this.executor = executor;
    }

    public TListener getListener() {
      return listener;
    }

    public Executor getExecutor() {
      return executor;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ExecutableListener)) {
        return false;
      }

      ExecutableListener<?> listener = (ExecutableListener<?>) other;
      return (getExecutor().equals(listener.getExecutor())
          && getListener().equals(listener.getListener()));
    }
  }
}
