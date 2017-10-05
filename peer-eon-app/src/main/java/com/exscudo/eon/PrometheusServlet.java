package com.exscudo.eon;

import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.PeerRegistry;
import com.exscudo.peer.store.sqlite.Storage;

/**
 * Servlet for prometheus monitoring system
 */
public class PrometheusServlet extends FrameworkServlet {
	private static final long serialVersionUID = 1L;
	private static long startedAt = System.currentTimeMillis();
	private Storage storage;
	private ExecutionContext context;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);

		WebApplicationContext appContext = getWebApplicationContext();
		this.storage = (Storage) appContext.getBean("storage");
		this.context = (ExecutionContext) appContext.getBean("executionContext");
	}

	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Runtime runtime = Runtime.getRuntime();
		long freeMemory = runtime.freeMemory();
		long totalMemory = runtime.totalMemory();
		long maxMemory = runtime.maxMemory();

		Block lastBlock = storage.getLastBlock();
		PeerRegistry registry = new PeerRegistry();
		String[] list = registry.getPeersList();

		int count = storage.getBacklog().size();

		try (PrintWriter out = response.getWriter()) {
			out.println(String.format("eon_memory_used %d", totalMemory - freeMemory));
			out.println(String.format("eon_memory_total %d", totalMemory));
			out.println(String.format("eon_memory_max %d", maxMemory));
			out.println(String.format("eon_last_block_id %d", lastBlock.getID()));
			out.println(String.format("eon_last_block_height %d", lastBlock.getHeight()));
			out.println(String.format("eon_last_block_generator %d", lastBlock.getSenderID()));
			out.println(String.format("eon_last_block_transactions_count %d", lastBlock.getTransactions().size()));
			out.println(String.format("eon_cumulative_difficulty %d", lastBlock.getCumulativeDifficulty()));
			out.println(String.format("eon_transactions_count %d", count));
			out.println(String.format("eon_peer_count %d", list.length));
			out.println(String.format("eon_uptime %d", (System.currentTimeMillis() - startedAt) / 1000L));

			int timeDiff = context.getCurrentTime() - lastBlock.getTimestamp();
			int targetHeight = lastBlock.getHeight() + timeDiff / Constant.BLOCK_PERIOD;
			out.println(String.format("eon_target_height %d", targetHeight));
		}
	}
}
