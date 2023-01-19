package org.eontechnology.and.peer.core.common.tasks;

import org.eontechnology.and.peer.core.common.Loggers;

public class TimedTask implements Runnable {

  private final Runnable mainTask;

  public TimedTask(Runnable mainTask) {

    this.mainTask = mainTask;
  }

  @Override
  public void run() {

    long begin = System.nanoTime();
    try {
      mainTask.run();
    } catch (Throwable th) {
      Loggers.error(mainTask.getClass(), th);
      throw th;
    } finally {
      long diff = System.nanoTime() - begin;
      Loggers.debug(mainTask.getClass(), "Timing: {}ms", diff / 1000000.0);
    }
  }
}
