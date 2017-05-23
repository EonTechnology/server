package com.exscudo.eon.peer.tasks;

import com.exscudo.eon.peer.ExecutionContext;

/**
 * The <code>AbstractTask</code> provides base functionality to the node task.
 *
 */
public class AbstractTask {
	/**
	 * The context in which the task has been launched.
	 */
	final ExecutionContext context;

	AbstractTask(ExecutionContext context) {

		this.context = context;
		
	}

}
