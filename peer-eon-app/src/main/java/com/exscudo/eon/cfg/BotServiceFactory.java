package com.exscudo.eon.cfg;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.bot.*;
import com.exscudo.eon.explorer.BlockchainExplorerService;

public class BotServiceFactory {

    private final PeerStarter starter;

    public BotServiceFactory(PeerStarter starter) {
        this.starter = starter;
    }

    public AccountService getAccountService() throws SQLException, IOException, ClassNotFoundException {
        return new AccountService(starter.getBacklog(), starter.getLedgerProvider(), starter.getBlockchain());
    }

    public ColoredCoinService getColoredCoinService() throws SQLException, IOException, ClassNotFoundException {
        return new ColoredCoinService(starter.getExecutionContext(),
                                      starter.getLedgerProvider(),
                                      starter.getBlockchain());
    }

    public TimeService getTimeService() {
        return new TimeService(starter.getTimeProvider());
    }

    public TransactionHistoryService getTransactionHistoryService() throws SQLException, IOException, ClassNotFoundException {
        return new TransactionHistoryService(starter.getStorage(), starter.getBacklog(), starter.getBlockchain());
    }

    public TransactionService getTransactionService() throws SQLException, IOException, ClassNotFoundException {
        return new TransactionService(starter.getTimeProvider(), starter.getBacklog());
    }

    public BlockchainExplorerService getBlockchainExplorerService() throws SQLException, IOException, ClassNotFoundException {
        return new BlockchainExplorerService(starter.getStorage(),
                                             starter.getExecutionContext(),
                                             starter.getBacklog(),
                                             starter.getBlockchain());
    }
}
