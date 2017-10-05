package com.exscudo.peer.eon.tasks;

import com.exscudo.peer.eon.ExecutionContext;

/**
 * The {@code BaseTask} provides base functionality to the node task.
 *
 */
public class BaseTask {

	/**
	 * The context in which the task has been launched.
	 */
	protected final ExecutionContext context;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            the context within which the task is launched
	 */
	BaseTask(ExecutionContext context) {
		this.context = context;
	}

}
