package com.exscudo.eon.app.cfg;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.app.api.bot.AccountBotService;
import com.exscudo.eon.app.api.bot.ColoredCoinBotService;
import com.exscudo.eon.app.api.bot.TimeBotService;
import com.exscudo.eon.app.api.bot.TransactionBotService;
import com.exscudo.eon.app.api.bot.TransactionHistoryBotService;
import com.exscudo.eon.app.api.explorer.BlockchainExplorerService;
import com.exscudo.peer.core.backlog.services.BacklogService;
import com.exscudo.peer.core.blockchain.services.BlockService;
import com.exscudo.peer.core.blockchain.services.TransactionService;

public class BotServiceFactory {

    private final PeerStarter starter;

    public BotServiceFactory(PeerStarter starter) {
        this.starter = starter;
    }

    public AccountBotService getAccountBotService() throws SQLException, IOException, ClassNotFoundException {
        return new AccountBotService(starter.getBacklog(),
                                     starter.getLedgerProvider(),
                                     starter.getBlockchainProvider());
    }

    public ColoredCoinBotService getColoredCoinBotService() throws SQLException, IOException, ClassNotFoundException {
        return new ColoredCoinBotService(starter.getExecutionContext(),
                                         starter.getLedgerProvider(),
                                         starter.getBlockchainProvider());
    }

    public TimeBotService getTimeBotService() {
        return new TimeBotService(starter.getTimeProvider());
    }

    public TransactionHistoryBotService getTransactionHistoryBotService() throws SQLException, IOException, ClassNotFoundException {
        return new TransactionHistoryBotService(getBacklogService(), getTransactionService(), getBlockService());
    }

    public TransactionBotService getTransactionBotService() throws SQLException, IOException, ClassNotFoundException {
        return new TransactionBotService(starter.getBacklog());
    }

    public BlockchainExplorerService getBlockchainExplorerService() throws SQLException, IOException, ClassNotFoundException {
        return new BlockchainExplorerService(getBacklogService(), getTransactionService(), getBlockService());
    }

    public BacklogService getBacklogService() throws SQLException, IOException, ClassNotFoundException {
        return new BacklogService(starter.getBacklog());
    }

    public BlockService getBlockService() throws SQLException, IOException, ClassNotFoundException {
        return new BlockService(starter.getStorage());
    }

    public TransactionService getTransactionService() throws SQLException, IOException, ClassNotFoundException {
        return new TransactionService(starter.getStorage());
    }
}
