package com.exscudo.peer.store.sqlite.lock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic implementation of {@code ILockManager}.
 * <p>
 * Implements pessimistic locking strategy.
 *
 * @see ILockManager
 */
public class PessimisticLockManager implements ILockManager {
	private static final Set<PessimisticLockManager> instances = Collections
			.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<PessimisticLockManager, Boolean>()));

	private final ConcurrentHashMap<Object, Lock> locks = new ConcurrentHashMap<>();

	public PessimisticLockManager() {
		instances.add(this);
	}

	@Override
	public void obtainLock(Object identifier) {

		while (true) {
			Lock lock = getLockFor(identifier);
			if (!lock.lock()) {
				locks.remove(identifier, lock);
			} else {
				break;
			}
		}
	}

	private Lock getLockFor(Object identifier) {
		Lock lock = locks.get(identifier);
		while (lock == null) {
			locks.putIfAbsent(identifier, new Lock());
			lock = locks.get(identifier);
		}
		return lock;
	}

	@Override
	public void releaseLock(Object identifier) {

		if (!locks.containsKey(identifier)) {
			throw new IllegalStateException();
		}
		Lock lock = getLockFor(identifier);
		lock.unlock(identifier);

	}

	private static Set<Thread> waitingThreads(Thread owner, Set<PessimisticLockManager> set) {
		Set<Thread> waitingThreads = new HashSet<Thread>();
		for (PessimisticLockManager lockManager : set) {
			for (Lock lock : lockManager.locks.values()) {
				if (lock.isOwner(owner)) {
					final Collection<Thread> c = lock.getQueuedThreads();
					for (Thread thread : c) {
						if (waitingThreads.add(thread)) {
							waitingThreads.addAll(waitingThreads(thread, set));
						}
					}
				}
			}
		}
		return waitingThreads;
	}

	class Lock {

		private final ReentrantLockWrapper lock;
		private volatile boolean isClosed;

		private Lock() {
			this.lock = new ReentrantLockWrapper();
		}

		public Collection<Thread> getQueuedThreads() {
			return lock.getQueuedThreads();
		}

		public boolean isOwner(Thread owner) {
			return lock.isOwner(owner);
		}

		private void unlock(Object identifier) {
			try {
				lock.unlock();
			} finally {
				if (lock.tryLock()) {
					try {
						if (lock.getHoldCount() == 1) {
							isClosed = true;
							locks.remove(identifier, this);
						}
					} finally {
						lock.unlock();
					}
				}
			}
		}

		private boolean lock() {

			try {
				if (!lock.tryLock(0, TimeUnit.NANOSECONDS)) {
					do {
						if (!lock.isHeldByCurrentThread() && lock.isLocked()) {
							for (Thread thread : waitingThreads(Thread.currentThread(), instances)) {

								if (lock.isOwner(thread)) {
									throw new RuntimeException("Deadlock.");
								}
							}
						}
					} while (!lock.tryLock(100, TimeUnit.MILLISECONDS));
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread was interrupted.", e);
			}

			if (isClosed) {
				lock.unlock();
				return false;
			}

			return true;
		}

		class ReentrantLockWrapper extends ReentrantLock {
			private static final long serialVersionUID = 7511480519432537617L;

			public boolean isOwner(Thread thread) {
				return thread.equals(this.getOwner());
			}

			@Override
			public Collection<Thread> getQueuedThreads() {
				return super.getQueuedThreads();
			}
		}
	}

}
