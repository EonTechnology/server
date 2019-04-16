package org.eontechnology.and.eon.app.cfg;

import java.io.IOException;
import java.sql.SQLException;

import org.eontechnology.and.eon.app.api.bot.AccountBotService;
import org.eontechnology.and.eon.app.api.bot.ColoredCoinBotService;
import org.eontechnology.and.eon.app.api.bot.PropertiesBotService;
import org.eontechnology.and.eon.app.api.bot.TimeBotService;
import org.eontechnology.and.eon.app.api.bot.TransactionBotService;
import org.eontechnology.and.eon.app.api.bot.TransactionHistoryBotService;
import org.eontechnology.and.eon.app.api.explorer.BlockchainExplorerService;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.backlog.services.BacklogService;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.blockchain.services.BlockService;
import org.eontechnology.and.peer.core.blockchain.services.TransactionService;
import org.eontechnology.and.peer.core.common.ITimeProvider;

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
        return new ColoredCoinBotService(starter.getLedgerProvider(), starter.getBlockchainProvider());
    }

    public PropertiesBotService getPropertiesBotService() throws SQLException, IOException, ClassNotFoundException {
        return new PropertiesBotService(starter.getFork().getGenesisBlockID(), starter.getTimeProvider());
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
        return new BacklogService(starter.getBacklog(),
                                  starter.getFork(),
                                  new BacklogTimeProvider(starter.getBlockchainProvider()));
    }

    public BlockService getBlockService() throws SQLException, IOException, ClassNotFoundException {
        return new BlockService(starter.getStorage());
    }

    public TransactionService getTransactionService() throws SQLException, IOException, ClassNotFoundException {
        return new TransactionService(starter.getStorage());
    }

    private static class BacklogTimeProvider implements ITimeProvider {
        private final IBlockchainProvider blockchainProvider;

        private BacklogTimeProvider(IBlockchainProvider blockchainProvider) {
            this.blockchainProvider = blockchainProvider;
        }

        @Override
        public int get() {
            return blockchainProvider.getLastBlock().getTimestamp() + Constant.BLOCK_PERIOD;
        }
    }
}
