package org.eontechnology.and.peer.core.common.tasks;

import org.eontechnology.and.peer.core.common.Loggers;

public class SequencedTask implements Runnable {

  private final Runnable[] taskSet;
  private final long delay;

  public SequencedTask(Runnable[] taskSet, long delay) {

    this.taskSet = taskSet;
    this.delay = delay;
  }

  @Override
  public void run() {

    for (Runnable task : taskSet) {
      try {
        task.run();
        Thread.sleep(delay);
      } catch (Throwable th) {
        Loggers.error(task.getClass(), th);
      }
    }
  }
}
