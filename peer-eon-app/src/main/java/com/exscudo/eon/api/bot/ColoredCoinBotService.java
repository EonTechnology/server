package com.exscudo.eon.api.bot;

import java.io.IOException;

import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;

/**
 * Colored coin service.
 */
public class ColoredCoinBotService {

    private final ExecutionContext context;
    private final IBlockchainProvider blockchain;
    private final LedgerProvider ledgerProvider;

    public ColoredCoinBotService(ExecutionContext context,
                                 LedgerProvider ledgerProvider,
                                 IBlockchainProvider blockchain) {
        this.context = context;
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
    }

    /**
     * Get a colored coin information.
     *
     * @param id
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public Info getInfo(String id) throws RemotePeerException, IOException {
        Account account = getColoredAccount(id);

        Info info = new Info();
        info.state = State.Unauthorized;

        if (account == null) {
            return info;
        }

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(account);
        if (!coloredCoin.isIssued()) {
            return info;
        }

        info.state = State.OK;
        info.decimalPoint = coloredCoin.getDecimalPoint();
        info.moneySupply = coloredCoin.getMoneySupply();
        info.timestamp = coloredCoin.getTimestamp();

        return info;
    }

    Account getColoredAccount(String id) throws RemotePeerException {
        AccountID accountID;
        try {
            accountID = new ColoredCoinID(id).getIssierAccount();
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        }

        final ILedger ledgerState = ledgerProvider.getLedger(blockchain.getLastBlock());
        return ledgerState.getAccount(accountID);
    }

    public static class State {

        /**
         * Colored coin is registered
         */
        public static final State OK = new State(200, "OK");

        /**
         * No associated colored coins
         */
        public static final State Unauthorized = new State(401, "Unauthorized");

        public final int code;
        public final String name;

        private State(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public static class Info {
        public State state;
        public Long moneySupply;
        public Integer decimalPoint;
        public Integer timestamp;
    }
}
