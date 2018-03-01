package com.exscudo.eon;

import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.PeerRegistry;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * Servlet for prometheus monitoring system
 */
public class PrometheusServlet extends FrameworkServlet {
    private static final long serialVersionUID = 1L;
    private static long startedAt = System.currentTimeMillis();

    private ExecutionContext context;
    private Backlog backlog;
    private IBlockchainService blockchain;
    private TimeProvider timeProvider;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        WebApplicationContext appContext = getWebApplicationContext();
        this.context = (ExecutionContext) appContext.getBean("executionContext");
        this.backlog = (Backlog) appContext.getBean("backlog");
        this.blockchain = (IBlockchainService) appContext.getBean("blockchain");
        this.timeProvider = (TimeProvider) appContext.getBean("timeProvider");
    }

    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();

        Block lastBlock = blockchain.getLastBlock();
        PeerRegistry registry = context.getPeers();
        String[] list = registry.getPeersList();

        int count = backlog.size();

        try (PrintWriter out = response.getWriter()) {
            out.println(String.format("eon_memory_used %d", totalMemory - freeMemory));
            out.println(String.format("eon_memory_total %d", totalMemory));
            out.println(String.format("eon_memory_max %d", maxMemory));
            out.println(String.format("eon_last_block_id %d", lastBlock.getID().getValue()));
            out.println(String.format("eon_last_block_height %d", lastBlock.getHeight()));
            out.println(String.format("eon_last_block_generator %d", lastBlock.getSenderID().getValue()));
            out.println(String.format("eon_last_block_transactions_count %d", lastBlock.getTransactions().size()));
            out.println(String.format("eon_cumulative_difficulty %d", lastBlock.getCumulativeDifficulty()));
            out.println(String.format("eon_transactions_count %d", count));
            out.println(String.format("eon_peer_count %d", list.length));
            out.println(String.format("eon_uptime %d", (System.currentTimeMillis() - startedAt) / 1000L));

            int timeDiff = timeProvider.get() - lastBlock.getTimestamp();
            int targetHeight = lastBlock.getHeight() + timeDiff / Constant.BLOCK_PERIOD;
            out.println(String.format("eon_target_height %d", targetHeight));
        }
    }
}
